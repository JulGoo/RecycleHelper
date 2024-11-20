package kr.akotis.recyclehelper;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.Image;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.RequestBody;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;


public class ImgSearchActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{android.Manifest.permission.CAMERA};

    // UI
    private ImageView resultImage;
    private PreviewView previewView;
    private ImageButton captureButton;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private MediaPlayer mediaPlayer;
    private View dimBackground;
    private ProgressBar loadingSpinner;
    private LinearLayout resultLayout;
    private TextView resultName;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_img_search);


        previewView = findViewById(R.id.previewView);       // 카메라 프리뷰
        captureButton = findViewById(R.id.captureButton);   // 촬영 버튼
        dimBackground = findViewById(R.id.dimBackground);   // 버튼 클릭시 검정화면
        loadingSpinner = findViewById(R.id.loadingSpinner); // 버튼 클릭시 로딩
        resultLayout = findViewById(R.id.resultLayout);     // 결과 화면 레이아웃
        resultImage = findViewById(R.id.resultImage);       // 결과 이미지
        resultName = findViewById(R.id.resultName);         // 결과 이름


        cameraExecutor = Executors.newSingleThreadExecutor();
        mediaPlayer = MediaPlayer.create(this, R.raw.camera_click);
        captureButton.setOnClickListener(view -> {
            animateButton(captureButton);   // 버튼 커졌다 작게
            vibrateOnClick();               // 진동
            playClickSound();               // 사운드
            showLoadingScreen();            // 로딩 화면 띄우기
            takePhoto();                    // 사진 촬영 및 API 요청
        });
        
        // 결과 화면 클릭 시 사라지게
        resultLayout.setOnClickListener(v -> hideResultScreen());

        // 카메라 권한 확인 및 요청
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

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
                Toast.makeText(this, "카메라 권한이 없습니다.", Toast.LENGTH_SHORT).show();
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

                // 이미지 캡처 설정
                imageCapture = new ImageCapture.Builder().build();

                // 프리뷰 연결
                androidx.camera.core.Preview preview = new androidx.camera.core.Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // 카메라 연결
                Camera camera = cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture);
            } catch (Exception e) {
                Toast.makeText(this, "카메라를 시작할 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }


    private void takePhoto() {
        if (imageCapture == null) return;
        // 사진 촬영
        imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                super.onCaptureSuccess(image);
                Log.d("ObjectDetection", "이미지 캡처 성공");

                // 캡처 이미지를 API 요청을 위한 ByteArray로 변환
                Bitmap bitmap = imageProxyToBitmap(image);
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                byte[] byteArray = byteArrayOutputStream.toByteArray();

                // API 서버 요청
                sendToFlaskServer(byteArray);
                image.close();
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                runOnUiThread(() -> Toast.makeText(ImgSearchActivity.this, "사진 촬영에 실패하였습니다.", Toast.LENGTH_SHORT).show());
            }
        });
    }


    // ImageProxy 를 Bitmap 형식으로 변환
    private Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
        @SuppressWarnings("UnsafeOptInUsageError")
        Image image = imageProxy.getImage();
        if (image != null) {
            // YUV_420_888 형식을 JPEG로 변환
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            return bitmap;
        }
        return null;
    }

    // API 서버에 요청
    private void sendToFlaskServer(byte[] byteArray) {
        String serverUrl = "https://mojuk.kr/vision";

        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = RequestBody.create(byteArray, MediaType.parse("image/jpeg"));
        Request request = new Request.Builder()
                .url(serverUrl)
                .post(requestBody)
                .build();

        // 서버로 요청 보내기
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("ServerRequest", "서버 요청 실패(연결X)", e);
                runOnUiThread(() -> Toast.makeText(ImgSearchActivity.this, "인터넷 연결에 실패하였습니다.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    // 결과 파싱
                    String responseData = response.body().string();
                    String name = "";
                    Bitmap resultmap = null;
                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        name = jsonObject.getString("name");

                        Bitmap basicmap = base64ToBitmap(jsonObject.getString("img"));
                        resultmap = rotateBitmap(basicmap, 90);
                    } catch (JSONException e) {
                        Log.e("ServerRequest", "데이터 파싱 에러 발생", e);
                        runOnUiThread(() -> Toast.makeText(ImgSearchActivity.this, "서버에 일시적인 문제가 발생하였습니다.", Toast.LENGTH_SHORT).show());
                    }

                    // 결과 이미지, 이름 설정
                    String finalName = name;
                    Bitmap finalResultmap = resultmap;
                    runOnUiThread(() -> {
                        try {
                            // 결과 표출하기
                            showResultScreen(finalResultmap, finalName);
                        } catch (Exception e) {
                            Log.e("ImgSearchActivity", "결과 UI 표출 실패", e);
                            runOnUiThread(() -> Toast.makeText(ImgSearchActivity.this, "UI 업데이트에 실패하였습니다.", Toast.LENGTH_SHORT).show());
                        }
                    });

                    Log.d("ServerResponse", "서버 응답: " + responseData);
                } else if (response.code() == 999) {
                    runOnUiThread(() -> {
                        Toast.makeText(ImgSearchActivity.this, "탐지된 객체가 없습니다.", Toast.LENGTH_SHORT).show();
                        hideResultScreen();
                    });
                } else if (response.code() == 998) {
                    runOnUiThread(() -> {
                        Toast.makeText(ImgSearchActivity.this, "API 연동에 실패하였습니다.", Toast.LENGTH_SHORT).show();
                        hideResultScreen();
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(ImgSearchActivity.this, "다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                        hideResultScreen();
                    });
                }
            }
        });
    }

    // API 서버에서 가져온 Base64 -> Bitmap 변환
    public Bitmap base64ToBitmap(String base64String) {
        byte[] decodedString = Base64.decode(base64String, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    }

    // AI 측정을 위해 기울어진 Bitmap 이미지 회전시키기
    private Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }








    // 사진 처리 UI 동작
    private void showLoadingScreen() {
        hideResultScreen();
        dimBackground.setVisibility(View.VISIBLE);
        loadingSpinner.setVisibility(View.VISIBLE);
        resultLayout.setVisibility(View.GONE);
    }
    private void hideResultScreen() {
        dimBackground.setVisibility(View.GONE);
        loadingSpinner.setVisibility(View.GONE);
        resultLayout.setVisibility(View.GONE);
    }
    private void showResultScreen(Bitmap img, String name) {
        loadingSpinner.setVisibility(View.GONE);
        resultLayout.setVisibility(View.VISIBLE);
        resultImage.setImageBitmap(img);
        resultName.setText(name);
    }











    // 촬영 버튼 효과
    private void animateButton(View button) {
        // 버튼이 커지는 애니메이션
        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(button, "scaleX", 1.0f, 1.2f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(button, "scaleY", 1.0f, 1.2f);

        // 버튼이 다시 원래 크기로 돌아가는 애니메이션
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(button, "scaleX", 1.2f, 1.0f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(button, "scaleY", 1.2f, 1.0f);

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
    }


}