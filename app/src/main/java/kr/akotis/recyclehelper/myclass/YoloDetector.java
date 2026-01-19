package kr.akotis.recyclehelper.myclass;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * YOLO 계열 TFLite 모델을 이용해 실시간 객체 탐지를 수행하는 헬퍼 클래스.
 */
public class YoloDetector implements AutoCloseable {

    private static final String TAG = "YoloDetector";
    private static final String MODEL_FILE = "best_float32_epoch200.tflite";
    private static final float SCORE_THRESHOLD = 0.5f; // 50% 이상 정확도만 표시 (낮은 정확도 필터링)
    private static final float NMS_THRESHOLD = 0.45f;

    // 16개 클래스 레이블
    private static final List<String> LABELS = Arrays.asList(
            "건전지",
            "금속캔_알루미늄캔",
            "금속캔_철캔",
            "비닐",
            "스티로폼",
            "유리병_갈색",
            "유리병_녹색",
            "유리병_투명",
            "종이",
            "페트병_PP",
            "페트병_무색단일",
            "페트병_유색단일",
            "플라스틱_PE",
            "플라스틱_PP",
            "플라스틱_PS",
            "형광등"
    );

    private Interpreter interpreter;
    private final int inputWidth;
    private final int inputHeight;
    private final int[] outputShape;
    private volatile boolean isClosed = false; // interpreter가 닫혔는지 확인하는 플래그

    public YoloDetector(Context context) throws IOException {
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(4);
        interpreter = new Interpreter(loadModelFile(context, MODEL_FILE), options);

        // 입력 형태 분석
        int[] inputShape = interpreter.getInputTensor(0).shape();
        if (inputShape.length == 4) {
            if (inputShape[1] == 3) { // NCHW
                inputHeight = inputShape[2];
                inputWidth = inputShape[3];
            } else { // NHWC
                inputHeight = inputShape[1];
                inputWidth = inputShape[2];
            }
        } else {
            inputHeight = 640;
            inputWidth = 640;
        }

        outputShape = interpreter.getOutputTensor(0).shape();
        Log.d(TAG, "모델 로드 완료: 입력=" + inputWidth + "x" + inputHeight + ", 임계값=" + (SCORE_THRESHOLD * 100) + "%");
    }

    public List<DetectionResult> detect(Bitmap bitmap) {
        // interpreter가 닫혔거나 null이면 빈 리스트 반환
        if (bitmap == null || interpreter == null || isClosed) {
            return Collections.emptyList();
        }

        // 원본 이미지 크기 저장
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();

        // 이미지 전처리
        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        tensorImage.load(bitmap);

        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(inputHeight, inputWidth, ResizeOp.ResizeMethod.BILINEAR))
                .build();

        tensorImage = imageProcessor.process(tensorImage);

        // 0~255 -> 0~1 정규화
        ByteBuffer inputBuffer = tensorImage.getBuffer();
        ByteBuffer normalizedBuffer = ByteBuffer.allocateDirect(inputBuffer.capacity());
        normalizedBuffer.order(ByteOrder.nativeOrder());
        inputBuffer.rewind();
        while (inputBuffer.hasRemaining()) {
            float pixel = inputBuffer.getFloat();
            normalizedBuffer.putFloat(pixel / 255f);
        }
        normalizedBuffer.rewind();

        // 출력 버퍼 준비
        float[][][] output = new float[outputShape[0]][outputShape[1]][outputShape[2]];

        // interpreter가 닫혔는지 다시 확인 (멀티스레드 환경에서 안전하게)
        if (isClosed) {
            return Collections.emptyList();
        }

        try {
            interpreter.run(normalizedBuffer, output);
        } catch (IllegalStateException e) {
            // interpreter가 이미 닫혔을 경우
            Log.w(TAG, "Interpreter가 이미 닫혔습니다: " + e.getMessage());
            isClosed = true;
            return Collections.emptyList();
        } catch (RuntimeException e) {
            // 다른 런타임 예외 처리 (예: 네이티브 크래시)
            Log.e(TAG, "Interpreter 실행 중 런타임 예외 발생", e);
            if (e.getMessage() != null && e.getMessage().contains("closed")) {
                isClosed = true;
            }
            return Collections.emptyList();
        } catch (Exception e) {
            // 기타 예외 처리
            Log.e(TAG, "Interpreter 실행 중 예외 발생", e);
            return Collections.emptyList();
        }

        // 출력 파싱 및 후처리
        List<DetectionResult> candidates = parseOutput(output, originalWidth, originalHeight);
        List<DetectionResult> results = nonMaxSuppression(candidates, NMS_THRESHOLD);
        
        // 최대 3개 선택: 점수와 중심 거리를 고려한 가중치 계산
        return selectTopDetections(results, originalWidth, originalHeight, 3);
    }

    /**
     * 출력 텐서를 파싱하여 DetectionResult 리스트로 변환
     */
    private List<DetectionResult> parseOutput(float[][][] output, int imgWidth, int imgHeight) {
        List<DetectionResult> candidates = new ArrayList<>();

        int dim1 = outputShape[1];
        int dim2 = outputShape[2];

        // dim1 < dim2: [1, 4+nc, num_boxes] (YOLOv8)
        // dim1 > dim2: [1, num_boxes, 4+nc] (transposed)
        if (dim1 < dim2) {
            int numBoxes = dim2;
            int numAttributes = dim1;
            int numClasses = numAttributes - 4;

            for (int i = 0; i < numBoxes; i++) {
                // 좌표 추출 (이미 픽셀 단위이거나 정규화된 값)
                float cx = output[0][0][i];
                float cy = output[0][1][i];
                float w = output[0][2][i];
                float h = output[0][3][i];

                // 최고 점수 클래스 찾기
                float bestScore = 0f;
                int bestClassIndex = -1;
                for (int c = 0; c < numClasses; c++) {
                    float classScore = output[0][4 + c][i];
                    if (classScore > bestScore) {
                        bestScore = classScore;
                        bestClassIndex = c;
                    }
                }

                // 정확도 임계값 이하는 필터링 (50% 미만은 무시)
                if (bestScore < SCORE_THRESHOLD || bestClassIndex < 0) {
                    continue; // 낮은 정확도 탐지는 건너뜀
                }

                // 좌표가 0~1 정규화인지, 0~640 픽셀 단위인지 확인
                boolean isNormalized = (cx <= 1.0f && cy <= 1.0f && w <= 1.0f && h <= 1.0f);
                
                float scaledCx, scaledCy, scaledW, scaledH;
                if (isNormalized) {
                    // 정규화된 좌표 -> 원본 이미지 크기로 변환
                    scaledCx = cx * imgWidth;
                    scaledCy = cy * imgHeight;
                    scaledW = w * imgWidth;
                    scaledH = h * imgHeight;
                } else {
                    // 픽셀 좌표 (입력 이미지 기준) -> 원본 이미지 크기로 스케일링
                    float scaleX = (float) imgWidth / inputWidth;
                    float scaleY = (float) imgHeight / inputHeight;
                    scaledCx = cx * scaleX;
                    scaledCy = cy * scaleY;
                    scaledW = w * scaleX;
                    scaledH = h * scaleY;
                }

                // center x,y,w,h -> left,top,right,bottom
                float left = scaledCx - scaledW / 2f;
                float top = scaledCy - scaledH / 2f;
                float right = scaledCx + scaledW / 2f;
                float bottom = scaledCy + scaledH / 2f;

                RectF box = new RectF(
                        clamp(left, 0, imgWidth),
                        clamp(top, 0, imgHeight),
                        clamp(right, 0, imgWidth),
                        clamp(bottom, 0, imgHeight)
                );

                // 유효한 박스인지 확인
                if (box.width() > 5 && box.height() > 5) {
                    String label = bestClassIndex < LABELS.size()
                            ? LABELS.get(bestClassIndex)
                            : "class-" + bestClassIndex;

                    candidates.add(new DetectionResult(box, label, bestScore));
                }
            }
        } else {
            // [1, num_boxes, 4+nc] 형태 (transposed)
            int numBoxes = dim1;
            int numAttributes = dim2;
            int numClasses = numAttributes - 4;

            for (int i = 0; i < numBoxes; i++) {
                float cx = output[0][i][0];
                float cy = output[0][i][1];
                float w = output[0][i][2];
                float h = output[0][i][3];

                // 최고 점수 클래스 찾기
                float bestScore = 0f;
                int bestClassIndex = -1;
                for (int c = 0; c < numClasses; c++) {
                    float classScore = output[0][i][4 + c];
                    if (classScore > bestScore) {
                        bestScore = classScore;
                        bestClassIndex = c;
                    }
                }

                // 정확도 임계값 이하는 필터링 (50% 미만은 무시)
                if (bestScore < SCORE_THRESHOLD || bestClassIndex < 0) {
                    continue; // 낮은 정확도 탐지는 건너뜀
                }

                // 좌표가 0~1 정규화인지 확인
                boolean isNormalized = (cx <= 1.0f && cy <= 1.0f && w <= 1.0f && h <= 1.0f);
                
                float scaledCx, scaledCy, scaledW, scaledH;
                if (isNormalized) {
                    scaledCx = cx * imgWidth;
                    scaledCy = cy * imgHeight;
                    scaledW = w * imgWidth;
                    scaledH = h * imgHeight;
                } else {
                    float scaleX = (float) imgWidth / inputWidth;
                    float scaleY = (float) imgHeight / inputHeight;
                    scaledCx = cx * scaleX;
                    scaledCy = cy * scaleY;
                    scaledW = w * scaleX;
                    scaledH = h * scaleY;
                }

                float left = scaledCx - scaledW / 2f;
                float top = scaledCy - scaledH / 2f;
                float right = scaledCx + scaledW / 2f;
                float bottom = scaledCy + scaledH / 2f;

                RectF box = new RectF(
                        clamp(left, 0, imgWidth),
                        clamp(top, 0, imgHeight),
                        clamp(right, 0, imgWidth),
                        clamp(bottom, 0, imgHeight)
                );

                if (box.width() > 5 && box.height() > 5) {
                    String label = bestClassIndex < LABELS.size()
                            ? LABELS.get(bestClassIndex)
                            : "class-" + bestClassIndex;

                    candidates.add(new DetectionResult(box, label, bestScore));
                }
            }
        }

        return candidates;
    }

    @Override
    public synchronized void close() {
        if (isClosed) {
            return; // 이미 닫혔으면 중복 호출 방지
        }
        isClosed = true; // 먼저 플래그 설정 (다른 스레드에서 detect() 호출 방지)
        if (interpreter != null) {
            try {
                interpreter.close();
                interpreter = null; // 참조 제거
            } catch (Exception e) {
                Log.w(TAG, "Interpreter 닫기 중 오류: " + e.getMessage());
            }
        }
    }

    private MappedByteBuffer loadModelFile(Context context, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 점수와 중심 거리를 고려하여 최대 N개의 탐지 결과를 선택한다.
     * 가중치 = 점수 * 0.7 + 중심거리_가중치 * 0.3
     */
    private List<DetectionResult> selectTopDetections(List<DetectionResult> detections, 
                                                      int imgWidth, int imgHeight, int maxCount) {
        if (detections.isEmpty() || maxCount <= 0) {
            return new ArrayList<>();
        }

        // 이미지 중심점
        float centerX = imgWidth / 2f;
        float centerY = imgHeight / 2f;
        float maxDistance = (float) Math.sqrt(centerX * centerX + centerY * centerY);

        // 가중치 계산을 위한 임시 클래스
        class WeightedDetection {
            DetectionResult detection;
            float weight;

            WeightedDetection(DetectionResult detection, float weight) {
                this.detection = detection;
                this.weight = weight;
            }
        }

        List<WeightedDetection> weighted = new ArrayList<>();
        for (DetectionResult det : detections) {
            RectF box = det.getBoundingBox();
            float boxCenterX = (box.left + box.right) / 2f;
            float boxCenterY = (box.top + box.bottom) / 2f;
            
            // 중심으로부터의 거리
            float distance = (float) Math.sqrt(
                    Math.pow(boxCenterX - centerX, 2) + 
                    Math.pow(boxCenterY - centerY, 2));
            
            // 거리를 0~1로 정규화 (가까울수록 1에 가까움)
            float normalizedDistance = 1f - (distance / maxDistance);
            normalizedDistance = Math.max(0f, Math.min(1f, normalizedDistance));
            
            // 가중치 계산: 점수 70% + 중심거리 30%
            float weight = det.getScore() * 0.7f + normalizedDistance * 0.3f;
            
            weighted.add(new WeightedDetection(det, weight));
        }

        // 가중치 순으로 정렬
        weighted.sort((a, b) -> Float.compare(b.weight, a.weight));

        // 최대 N개 선택
        List<DetectionResult> selected = new ArrayList<>();
        int count = Math.min(maxCount, weighted.size());
        for (int i = 0; i < count; i++) {
            selected.add(weighted.get(i).detection);
        }

        return selected;
    }

    private List<DetectionResult> nonMaxSuppression(List<DetectionResult> detections, float iouThreshold) {
        List<DetectionResult> results = new ArrayList<>();
        detections.sort(Comparator.comparing(DetectionResult::getScore).reversed());

        for (DetectionResult candidate : detections) {
            boolean shouldSelect = true;
            for (DetectionResult picked : results) {
                float iou = calculateIou(candidate.getBoundingBox(), picked.getBoundingBox());
                if (iou > iouThreshold) {
                    shouldSelect = false;
                    break;
                }
            }
            if (shouldSelect) {
                results.add(candidate);
            }
        }
        return results;
    }

    private float calculateIou(RectF a, RectF b) {
        float intersectionLeft = Math.max(a.left, b.left);
        float intersectionTop = Math.max(a.top, b.top);
        float intersectionRight = Math.min(a.right, b.right);
        float intersectionBottom = Math.min(a.bottom, b.bottom);

        float intersectionArea = Math.max(0, intersectionRight - intersectionLeft)
                * Math.max(0, intersectionBottom - intersectionTop);
        float unionArea = a.width() * a.height() + b.width() * b.height() - intersectionArea;
        if (unionArea <= 0) return 0f;
        return intersectionArea / unionArea;
    }
}
