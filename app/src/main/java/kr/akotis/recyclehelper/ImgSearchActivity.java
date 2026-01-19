package kr.akotis.recyclehelper;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import kr.akotis.recyclehelper.myclass.DetectionResult;
import kr.akotis.recyclehelper.myclass.OverlayView;
import kr.akotis.recyclehelper.myclass.YoloDetector;

/**
 * 실시간 YOLO 객체 탐지 화면.
 * 카메라 프리뷰 위에 탐지된 객체마다 박스와 라벨을 실시간으로 표시한다.
 */
public class ImgSearchActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};

    private PreviewView previewView;
    private OverlayView overlayView;

    private ExecutorService cameraExecutor;
    private YoloDetector yoloDetector;
    private volatile boolean isProcessingFrame = false;
    private volatile boolean isDestroyed = false;
    private volatile boolean isPaused = false;
    private ProcessCameraProvider cameraProvider;
    private ImageAnalysis imageAnalysis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_img_search);

        previewView = findViewById(R.id.previewView);
        overlayView = findViewById(R.id.overlayView);

        cameraExecutor = Executors.newSingleThreadExecutor();

        try {
            yoloDetector = new YoloDetector(this);
        } catch (Exception e) {
            Toast.makeText(this, "모델을 불러오지 못했습니다. assets 폴더를 확인하세요.", Toast.LENGTH_LONG).show();
            Log.e("ImgSearchActivity", "YOLO 모델 로딩 실패", e);
            finish();
            return;
        }

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "카메라 권한이 없습니다.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                
                if (isDestroyed || isPaused) {
                    return;
                }

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(640, 640))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
            } catch (Exception e) {
                Log.e("ImgSearchActivity", "카메라 시작 실패", e);
                if (!isDestroyed && !isPaused) {
                    Toast.makeText(this, "카메라를 시작할 수 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void analyzeImage(ImageProxy imageProxy) {
        if (isDestroyed || isPaused || isProcessingFrame || yoloDetector == null) {
            imageProxy.close();
            return;
        }
        isProcessingFrame = true;

        Bitmap bitmap = imageProxyToBitmap(imageProxy);
        imageProxy.close();

        if (bitmap != null && !isDestroyed) {
            try {
                final int bitmapWidth = bitmap.getWidth();
                final int bitmapHeight = bitmap.getHeight();
                List<DetectionResult> results = yoloDetector.detect(bitmap);
                
                if (!isDestroyed && overlayView != null) {
                    runOnUiThread(() -> {
                        if (isDestroyed || overlayView == null) {
                            return;
                        }
                        int overlayWidth = overlayView.getWidth();
                        int overlayHeight = overlayView.getHeight();

                        if (overlayWidth > 0 && overlayHeight > 0) {
                            List<DetectionResult> scaled = scaleToOverlay(results, bitmapWidth, bitmapHeight);
                            overlayView.setDetections(scaled);
                        } else {
                            overlayView.setDetections(results);
                        }
                    });
                }
            } catch (Exception e) {
                Log.e("ImgSearchActivity", "객체 탐지 중 오류 발생", e);
                if (!isDestroyed && overlayView != null) {
                    runOnUiThread(() -> {
                        if (!isDestroyed && overlayView != null) {
                            overlayView.setDetections(new ArrayList<>());
                        }
                    });
                }
            }
        }

        isProcessingFrame = false;
    }

    private Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
        ImageProxy.PlaneProxy[] planes = imageProxy.getPlanes();
        if (planes == null || planes.length == 0) return null;

        ByteBuffer buffer = planes[0].getBuffer();
        buffer.rewind();

        int width = imageProxy.getWidth();
        int height = imageProxy.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);

        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
        if (rotationDegrees != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotationDegrees);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }
        return bitmap;
    }

    private List<DetectionResult> scaleToOverlay(List<DetectionResult> detections, int imageWidth, int imageHeight) {
        if (detections == null || detections.isEmpty()) return new ArrayList<>();
        if (overlayView.getWidth() == 0 || overlayView.getHeight() == 0) return detections;

        float scaleX = (float) overlayView.getWidth() / imageWidth;
        float scaleY = (float) overlayView.getHeight() / imageHeight;

        List<DetectionResult> scaled = new ArrayList<>();
        for (DetectionResult detection : detections) {
            RectF box = detection.getBoundingBox();
            RectF mapped = new RectF(
                    box.left * scaleX,
                    box.top * scaleY,
                    box.right * scaleX,
                    box.bottom * scaleY
            );
            scaled.add(new DetectionResult(mapped, detection.getLabel(), detection.getScore()));
        }
        return scaled;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isPaused = true;
        
        // ImageAnalysis analyzer를 제거하여 백그라운드 분석 즉시 중지
        if (imageAnalysis != null) {
            try {
                imageAnalysis.clearAnalyzer();
            } catch (Exception e) {
                Log.e("ImgSearchActivity", "ImageAnalysis analyzer 제거 중 오류", e);
            }
        }
        
        // 카메라 중지
        if (cameraProvider != null) {
            try {
                cameraProvider.unbindAll();
            } catch (Exception e) {
                Log.e("ImgSearchActivity", "카메라 중지 중 오류", e);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isDestroyed) {
            return;
        }
        isPaused = false;
        
        // 카메라 다시 시작
        if (allPermissionsGranted()) {
            startCamera();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDestroyed = true;
        isPaused = true;
        
        // ImageAnalysis analyzer 제거
        if (imageAnalysis != null) {
            try {
                imageAnalysis.clearAnalyzer();
            } catch (Exception e) {
                Log.e("ImgSearchActivity", "ImageAnalysis analyzer 제거 중 오류", e);
            }
        }
        
        // 카메라 먼저 중지
        if (cameraProvider != null) {
            try {
                cameraProvider.unbindAll();
            } catch (Exception e) {
                Log.e("ImgSearchActivity", "카메라 중지 중 오류", e);
            }
        }
        
        // YOLO 모델 닫기
        if (yoloDetector != null) {
            try {
                yoloDetector.close();
            } catch (Exception e) {
                Log.e("ImgSearchActivity", "YOLO 모델 닫기 중 오류", e);
            }
        }
        
        // Executor 종료 (대기 중인 작업 완료 대기)
        if (cameraExecutor != null) {
            try {
                cameraExecutor.shutdown();
                // 최대 2초 대기 후 강제 종료
                if (!cameraExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    cameraExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                cameraExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                Log.e("ImgSearchActivity", "Executor 종료 중 오류", e);
            }
        }
    }
}
