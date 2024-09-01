package kr.kro.barrierfree.recyclehelper;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.common.util.concurrent.ListenableFuture;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageSearchActivity extends AppCompatActivity {
    private static final String TAG = "ObjectDetection";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{android.Manifest.permission.CAMERA};

    private PreviewView previewView;
    private TextView resultTextView;
    private Interpreter tflite;
    private ExecutorService cameraExecutor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_image_search);

        // UI 요소 초기화
        previewView = findViewById(R.id.previewView);
        resultTextView = findViewById(R.id.resultTextView);

        // 카메라 권한 확인 및 요청
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }


        // ExecutorService 초기화
        cameraExecutor = Executors.newSingleThreadExecutor();

        // TensorFlow Lite 모델 로드
        try {
            tflite = new Interpreter(FileUtil.loadMappedFile(this, "final.tflite"));
            Log.d(TAG, "TFLite model loaded successfully.");
        } catch (IOException e) {
            Log.e(TAG, "Error loading TFLite model.", e);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }



    // 권한 요청 결과 처리
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    // 모든 권한이 부여되었는지 확인
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }


    // 카메라 실행
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // 프리뷰 설정
                Preview preview = new Preview.Builder()
                        .build();

                // 후면 카메라 선택
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                // 이미지 분석 설정
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(640, 640))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    detectObjects(imageProxy);
                });

                // 라이프사이클에 바인딩
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

                // 프리뷰 설정
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera.", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }


    // 객체 탐지 메서드
    private void detectObjects(ImageProxy imageProxy) {
        @OptIn(markerClass = ExperimentalGetImage.class) Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            // ImageProxy를 Bitmap으로 변환
            Bitmap bitmap = toBitmap(mediaImage);
            if (bitmap != null) {
                // 모델 입력 크기에 맞게 Bitmap 리사이즈
                Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 640, 640, true);

                // Bitmap을 ByteBuffer로 변환
                ByteBuffer inputBuffer = convertBitmapToByteBuffer(resizedBitmap);

                // 모델 출력 설정 (float[1][3][10][10][85])
                float[][][][][] output = new float[1][3][10][10][85];

                // 모델 실행
                tflite.run(inputBuffer, output);

                // 결과 분석
                String detectionResult = parseOutput(output);

                // UI 업데이트는 메인 스레드에서 수행
                runOnUiThread(() -> resultTextView.setText(detectionResult));
            }
        }
        imageProxy.close();
    }


    // Image를 Bitmap으로 변환
    private Bitmap toBitmap(Image image) {
        byte[] nv21 = yuv420ToNv21(image);
        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 100, out);
        byte[] jpegBytes = out.toByteArray();
        return android.graphics.BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);
    }


    // YUV_420_888 이미지를 NV21 포맷으로 변환
    private byte[] yuv420ToNv21(Image image) {
        byte[] nv21;
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer(); // Y
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer(); // U
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer(); // V

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        nv21 = new byte[ySize + uSize + vSize];

        // Y 데이터 복사
        yBuffer.get(nv21, 0, ySize);

        // VU 순서로 NV21에 복사
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        return nv21;
    }


    // Bitmap을 ByteBuffer로 변환
    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1 * 3 * 640 * 640 * 4); // float 크기
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[640 * 640];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        // Normalize and convert to float
        for (int pixelValue : intValues) {
            // 추출한 픽셀을 RGB로 변환하고 정규화 (0-1 범위)
            float r = ((pixelValue >> 16) & 0xFF) / 255.0f;
            float g = ((pixelValue >> 8) & 0xFF) / 255.0f;
            float b = (pixelValue & 0xFF) / 255.0f;
            byteBuffer.putFloat(r);
            byteBuffer.putFloat(g);
            byteBuffer.putFloat(b);
        }
        return byteBuffer;
    }

    // 모델 출력 파싱
    private String parseOutput(float[][][][][] output) {
        StringBuilder result = new StringBuilder();
        for (int gridY = 0; gridY < 10; gridY++) {
            for (int gridX = 0; gridX < 10; gridX++) {
                for (int anchor = 0; anchor < 3; anchor++) {
                    float confidence = output[0][anchor][gridY][gridX][4];
                    if (confidence > 0.3) { // 신뢰도 임계값
                        // 클래스 확률 찾기
                        int detectedClass = -1;
                        float maxProb = 0;
                        for (int i = 5; i < 85; i++) { // 클래스 수에 따라 변경
                            if (output[0][anchor][gridY][gridX][i] > maxProb) {
                                maxProb = output[0][anchor][gridY][gridX][i];
                                detectedClass = i - 5; // 클래스 인덱스 조정
                            }
                        }
                        if (detectedClass != -1) {
                            result.append("객체: ").append(getClassName(detectedClass))
                                    .append(" | Confidence: ").append(String.format("%.2f", confidence * maxProb))
                                    .append("\n");
                        }
                    }
                }
            }
        }
        if (result.length() == 0) {
            result.append("탐지된 객체가 없습니다..");
        }
        return result.toString();
    }


    // 클래스 인덱스에 따른 클래스 이름 반환 (예시)
    private String getClassName(int index) {
        switch(index) {
            case 0: return "can_steel";
            case 1: return "can_aluminium";
            case 2: return "paper";
            case 3: return "PET_transparent";
            case 4: return "PET_color";
            case 5: return "plastic_PE";
            case 6: return "plastic_PP";
            case 7: return "plastic_PS";
            case 8: return "styrofoam";
            case 9: return "plastic_bag";
            case 10: return "glass_brown";
            case 11: return "glass_green";
            case 12: return "glass_transparent";
            case 13: return "battery";
            case 14: return "light";
            default: return "Unknown";
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (tflite != null) {
            tflite.close();
        }
    }

}