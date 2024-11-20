package kr.akotis.recyclehelper;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kr.akotis.recyclehelper.myclass.OverlayView;

public class ImgSearchActivity extends AppCompatActivity {
    private static final String TAG = "ObjectDetection";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{android.Manifest.permission.CAMERA};


    private PreviewView previewView;
    private ImageButton captureButton;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private MediaPlayer mediaPlayer;
    private ObjectDetector objectDetector;
    private OverlayView overlayView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_img_search);

        previewView = findViewById(R.id.previewView);
        captureButton = findViewById(R.id.captureButton);
        overlayView = findViewById(R.id.overlayView);
        cameraExecutor = Executors.newSingleThreadExecutor();
        mediaPlayer = MediaPlayer.create(this, R.raw.camera_click);
        captureButton.setOnClickListener(view -> {
            animateButton(captureButton);
            vibrateOnClick();
            playClickSound();
            takePhoto();
        });

        // 카메라 권한 확인 및 요청
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        ObjectDetectorOptions options =
                new ObjectDetectorOptions.Builder()
                        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE) // 실시간 모드
                        .enableClassification() // 객체 분류 활성화
                        .build();

        // 객체 탐지기 초기화
        objectDetector = ObjectDetection.getClient(options);

        // ExecutorService 초기화
        cameraExecutor = Executors.newSingleThreadExecutor();
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

    // 카메라 시작
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // 프리뷰 설정
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                // 이미지 분석 설정
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                // 분석기 설정
                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                androidx.camera.core.Preview preview = new androidx.camera.core.Preview.Builder().build();

                // 이미지 캡처 설정
                //imageCapture = new ImageCapture.Builder().build();

                // 프리뷰 연결
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // 카메라 연결
                //Camera camera = cameraProvider.bindToLifecycle(
                //        this, cameraSelector, preview, imageCapture);
                Camera camera = cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                Toast.makeText(this, "카메라를 시작할 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }



    private void takePhoto() {
        if (imageCapture == null) return;

        /* 사진 로컬에 저장하는 코드
        // 저장 위치 지정
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "New_Photo_" + System.currentTimeMillis());
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

        // 출력 옵션 설정
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(
                getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
        ).build();

        // 사진 촬영
        imageCapture.takePicture(outputOptions, cameraExecutor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Uri savedUri = outputFileResults.getSavedUri();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "사진 저장됨: " + savedUri, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "사진 촬영 실패: " + exception.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
        */

        // 출력 옵션 설정
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(
                new File(getCacheDir(), "temp_image.jpg")
        ).build();

        // 사진 촬영
        imageCapture.takePicture(outputOptions, cameraExecutor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                File photoFile = new File(getCacheDir(), "temp_image.jpg");
                try {
                    // 사진을 비트맵으로 로드
                    Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());

                    // 비트맵을 바이트 배열로 변환
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                    byte[] imageBytes = byteArrayOutputStream.toByteArray();

                    // Google Vision API에 이미지를 전송하여 결과 받기
                    //detectTextFromImage(imageBytes);

                    //detectObjectsFromImage(bitmap);

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(ImgSearchActivity.this, "이미지 처리 실패", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                runOnUiThread(() -> Toast.makeText(ImgSearchActivity.this, "사진 촬영 실패: " + exception.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }


    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        try {
            @SuppressWarnings("UnsafeOptInUsageError")
            InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());

            /*
            // 객체 탐지 수행
            objectDetector.process(image)
                    .addOnSuccessListener(detectedObjects -> {
                        for (DetectedObject detectedObject : detectedObjects) {
                            Log.d("ObjectDetection", "경계 상자: " + detectedObject.getBoundingBox());

                            if (detectedObject.getLabels().isEmpty()) {
                                Log.d("ObjectDetection", "라벨 없음");
                            } else {
                                for (DetectedObject.Label label : detectedObject.getLabels()) {
                                    Log.d("ObjectDetection", "탐지된 객체: " + label.getText() + ", 신뢰도: " + label.getConfidence());
                                }
                            }
                        }
                    })
                    .addOnFailureListener(e -> Log.e("ObjectDetection", "객체 탐지 실패: ", e))
                    .addOnCompleteListener(task -> imageProxy.close());
            */

            objectDetector.process(image)
                    .addOnSuccessListener(detectedObjects -> {
                        List<Rect> boundingBoxes = new ArrayList<>();
                        for (DetectedObject detectedObject : detectedObjects) {
                            boundingBoxes.add(detectedObject.getBoundingBox());

                            if (detectedObject.getLabels().isEmpty()) {
                                Log.d("ObjectDetection", "라벨 없음");
                            } else {
                                for (DetectedObject.Label label : detectedObject.getLabels()) {
                                    Log.d("ObjectDetection", "탐지된 객체: " + label.getText() + ", 신뢰도: " + label.getConfidence());
                                }
                            }
                        }
                        runOnUiThread(() -> overlayView.setBoundingBoxes(boundingBoxes));
                    })
                    .addOnFailureListener(e -> Log.e("ObjectDetection", "객체 탐지 실패: ", e))
                    .addOnCompleteListener(task -> imageProxy.close());
        } catch (Exception e) {
            Log.e("ObjectDetection", "이미지 분석 오류: ", e);
            imageProxy.close();
        }
    }










    private void animateButton(View button) {
        // 버튼이 커지는 애니메이션
        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(button, "scaleX", 1.0f, 1.1f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(button, "scaleY", 1.0f, 1.1f);

        // 버튼이 다시 원래 크기로 돌아가는 애니메이션
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(button, "scaleX", 1.1f, 1.0f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(button, "scaleY", 1.1f, 1.0f);

        // 애니메이션 순서 설정
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(scaleUpX).with(scaleUpY); // 동시에 확대
        animatorSet.play(scaleDownX).with(scaleDownY).after(scaleUpX); // 축소는 확대 후 실행
        animatorSet.setDuration(200); // 전체 애니메이션 시간 (밀리초)
        animatorSet.start(); // 애니메이션 시작
    }
    private void vibrateOnClick() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }
    private void playClickSound() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        if (objectDetector != null) {
            objectDetector.close();
        }
    }


}