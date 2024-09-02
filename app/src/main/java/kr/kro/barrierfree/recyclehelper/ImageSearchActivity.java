package kr.kro.barrierfree.recyclehelper;

import static androidx.browser.browseractions.BrowserServiceFileProvider.loadBitmap;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
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
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageSearchActivity extends AppCompatActivity {
    private Interpreter tflite;
    private static final int INPUT_SIZE = 640;
    private PreviewView previewView;
    private TextView resultTextView;
    private ExecutorService cameraExecutor;
    private ImageUtils imageUtils;

    private String a = ".";
    private ImageCapture imageCapture;
    private Handler handler = new Handler(Looper.getMainLooper());
    /*
    private static final String[] classNames = {"can_steel", "can_aluminium", "paper", "PET_transparent", "PET_color",
            "plastic_PE", "plastic_PP", "plastic_PS", "styrofoam", "plastic_bag",
            "glass_brown", "glass_green", "glass_transparent", "battery", "light"};

     */
    private static final String[] classNames = {"캔", "알루미늄 캔", "종이", "투명한 페트병", "색상이 있는 페트병",
        "플라스틱 PE", "플라스틱 PP", "플라스틱 PS", "스티로폼", "비닐",
        "갈색 유리", "초록색 유리", "투명한 유리", "배터리", "전등"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_image_search);

        imageUtils = new ImageUtils();

        // UI 요소 초기화
        previewView = findViewById(R.id.previewView);
        resultTextView = findViewById(R.id.resultTextView);
        //button = findViewById(R.id.btn_shot);
        //button.setOnClickListener(this);

        // 권한 요청
        TedPermission.with(this)
                .setPermissionListener(permissionListener)
                .setRationaleMessage("카메라 권한이 필요합니다.")
                .setDeniedMessage("카메라 권한이 거부되었습니다.")
                .setPermissions(Manifest.permission.CAMERA)
                .check();

        // ExecutorService 초기화
        cameraExecutor = Executors.newSingleThreadExecutor();

        // TensorFlow Lite 모델 로드
        try {
            tflite = new Interpreter(FileUtil.loadMappedFile(this, "final.tflite"));
            Log.d("Tensorflow Interpreter", "TFLite model loaded successfully.");
        } catch (IOException e) {
            Log.e("Tensorflow Interpreter", "Error loading TFLite model.", e);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void updateTextView(String text, long delayMillis) {
        runOnUiThread(() -> {
            resultTextView.setText(text);

            if (delayMillis > 0) {
                // 지정된 시간 후에 텍스트를 초기화
                handler.postDelayed(() -> resultTextView.setText(""), delayMillis);
            }
        });
    }


    private void capturePhoto() {
        if(imageCapture == null) {
            return;
        }
        imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                super.onCaptureSuccess(image);
                    // mediaImage 객체를 사용하여 후처리 작업 수행 가능
                    // 예를 들어, mediaImage의 데이터로 이미지 처리 등
                detectObject(image);
                image.close();
                //Toast.makeText(ImageSearchActivity.this, "Photo captured!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                super.onError(exception);
                Log.e("Image Capture : ", "Capture failed", exception);
                //Toast.makeText(ImageSearchActivity.this, "Capture failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }




    // 카메라 권한 리스너
    private final PermissionListener permissionListener = new PermissionListener() {
        @Override
        public void onPermissionGranted() {
            startCamera();
        }
        @Override
        public void onPermissionDenied(List<String> deniedPermissions) {
            Toast.makeText(ImageSearchActivity.this, "권한이 거부되었습니다: " + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
            finish();
        }
    };


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

                // 캡처 설정
                imageCapture = new ImageCapture.Builder()
                        .setTargetResolution(new Size(640,640))
                        .build();

                // 이미지 분석 설정
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(640, 640))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                // 이미지 분석 로직
                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    detectObject(imageProxy);
                });

                // 라이프사이클에 바인딩
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis, imageCapture);

                // 프리뷰 설정
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

            } catch (ExecutionException | InterruptedException e) {
                Log.e("Object Dectection : ", "Error starting camera.", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }




    // 객체 탐지 메서드
    public void detectObject(ImageProxy imageProxy) {
        @OptIn(markerClass = ExperimentalGetImage.class) Image mediaImage = imageProxy.getImage();

        // 사진 데이터,
        ByteBuffer inputBuffer = imageUtils.preprocessImage(mediaImage, INPUT_SIZE, INPUT_SIZE);
        float[][][][][] output = new float[1][3][10][10][85];

        // 모델 작동
        tflite.run(inputBuffer, output);

        List<DetectionResult> detectResults = parseOutput(output);
        if(detectResults == null || detectResults.isEmpty()) {
            //updateTextView("No Object", 0);
            a = "No Object";
        } else {
            printDetectionResults(detectResults);
            //String whatName = classNames[detectResults.get(0).getClassId()];
            //updateTextView(whatName, 3000);
            a = classNames[detectResults.get(0).getClassId()];
        }

        runOnUiThread(() -> resultTextView.setText(a));

        imageProxy.close();
    }



    // output 가공
    public List<DetectionResult> parseOutput(float[][][][][] output) {
        float confThreshold = 0.5f;
        List<DetectionResult> detections = new ArrayList<>();

        for (int i = 0; i < 3; i++) {  // 3개의 scale 탐색
            for (int y = 0; y < 10; y++) {  // 10x10 grid 탐색
                for (int x = 0; x < 10; x++) {
                    float score = sigmoid(output[0][i][y][x][4]);

                    if (score > confThreshold) {
                        float[] classScores = Arrays.copyOfRange(output[0][i][y][x], 5, 85);
                        for (int j = 0; j < classScores.length; j++) {
                            classScores[j] = sigmoid(classScores[j]);
                        }

                        int classId = argmax(classScores);
                        float classConf = classScores[classId];

                        if (classConf > confThreshold) {
                            detections.add(new DetectionResult(classId, score, classConf));
                        }
                    }
                }
            }
        }
        return detections;
    }



    private void printDetectionResults(List<DetectionResult> detections) {
        for (int i = 0; i < detections.size(); i++) {
            DetectionResult detection = detections.get(i);
            Log.d("Detection", "Object " + (i + 1) + ":");
            Log.d("Detection", "  Class: " + classNames[detection.getClassId()]);
            Log.d("Detection", "  Confidence: " + detection.getScore());
            Log.d("Detection", "  Class Confidence: " + detection.getClassConfidence());
        }
    }

    // 종료
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }

    // 시그모이드 함수
    private float sigmoid(float x) {
        return (float) (1 / (1 + Math.exp(-x)));
    }
    // argmax 함수
    private int argmax(float[] array) {
        int maxIndex = 0;
        for (int i = 1; i < array.length; i++) {
            if (array[i] > array[maxIndex]) {
                maxIndex = i;
            }
        }
        return maxIndex;
    }

}