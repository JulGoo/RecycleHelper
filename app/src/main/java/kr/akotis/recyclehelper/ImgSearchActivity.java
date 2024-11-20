package kr.akotis.recyclehelper;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.Image;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
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

import org.json.JSONArray;
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
import okhttp3.RequestBody;
import okhttp3.OkHttpClient;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import java.io.IOException;


public class ImgSearchActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{android.Manifest.permission.CAMERA};

    private TextView resultText;
    private PreviewView previewView;
    private ImageButton captureButton;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private MediaPlayer mediaPlayer;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_img_search);

        resultText = findViewById(R.id.resultTextView);
        previewView = findViewById(R.id.previewView);
        captureButton = findViewById(R.id.captureButton);
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

                Bitmap bitmap = imageProxyToBitmap(image);
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                byte[] byteArray = byteArrayOutputStream.toByteArray();

                // Flask 서버로 이미지 전송
                sendToFlaskServer(byteArray);


                image.close();
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                runOnUiThread(() -> Toast.makeText(ImgSearchActivity.this, "이미지 분석 실패: ", Toast.LENGTH_SHORT).show());
            }
        });
    }


    private Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
        @SuppressWarnings("UnsafeOptInUsageError")
        Image image = imageProxy.getImage();
        if (image != null) {
            // YUV_420_888 형식을 JPEG로 변환 (필요시 추가적인 코드 필요)
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            image.close();
            return bitmap;
        }
        return null;
    }


    private void sendToFlaskServer(byte[] byteArray) {
        // 서버 URL 설정
        String serverUrl = "https://mojuk.kr/vision";

        // 요청을 만들기 위한 OkHttpClient 사용
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
                Log.e("ServerRequest", "서버 요청 실패: ", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    JSONArray jsonArray = null;
                    try {
                        jsonArray = new JSONArray(responseData);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    Log.d("ServerResponse", "서버 응답: " + responseData);
                } else {
                    Log.e("ServerRequest", "서버 오류: " + response.code());
                }
            }
        });
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
    }


}