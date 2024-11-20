package kr.akotis.recyclehelper;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
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
import com.google.mlkit.vision.common.InputImage;

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
    private ImageView resultImageView;
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
        resultImageView = findViewById(R.id.resultImageView);
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
                //Bitmap copybitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
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
                    String name = "";
                    Bitmap resultmap = null;
                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        name = jsonObject.getString("name");
                        String imgresult = jsonObject.getString("img");
                        Bitmap basicmap = base64ToBitmap(imgresult);
                        resultmap = rotateBitmap(basicmap, 90);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    String finalName = name;
                    Bitmap finalResultmap = resultmap;
                    runOnUiThread(() -> {
                        resultText.setText(finalName);
                        resultImageView.setImageBitmap(finalResultmap);
                    });

                    Log.d("ServerResponse", "서버 응답: " + responseData);
                    //Bitmap result = processImageWithBoundingBox(bitmap, responseData);
                    //resultImageView.setImageBitmap(result);
                } else {
                    Log.e("ServerRequest", "서버 오류: " + response.code());
                }
            }
        });
    }

    public Bitmap base64ToBitmap(String base64String) {
        byte[] decodedString = Base64.decode(base64String, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    }


    public Bitmap processImageWithBoundingBox(Bitmap resultBitmap, String jsonResponse) {
        try {
            // JSON 파싱
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONArray boundingBoxArray = jsonObject.getJSONArray("bounding_box");
            String name = jsonObject.getString("name");
            double score = jsonObject.getDouble("score");

            // 바운딩 박스 좌표
            float[] boundingBoxCoordinates = new float[8]; // x1, y1, x2, y2, x3, y3, x4, y4
            for (int i = 0; i < 4; i++) {
                JSONObject point = boundingBoxArray.getJSONObject(i);
                boundingBoxCoordinates[i * 2] = (float) point.getDouble("x"); // x
                boundingBoxCoordinates[i * 2 + 1] = (float) point.getDouble("y"); // y
            }

            // 이미지를 90도 회전
            resultBitmap = rotateBitmap(resultBitmap, 90);

            // Canvas를 사용하여 이미지에 바운딩 박스 그리기
            Canvas canvas = new Canvas(resultBitmap);
            Paint paint = new Paint();
            paint.setColor(Color.RED); // 빨간색
            paint.setStrokeWidth(5);
            paint.setStyle(Paint.Style.STROKE);  // 선 스타일 설정

            // 이미지 크기 비율을 맞춰서 좌표 조정
            int width = resultBitmap.getWidth();
            int height = resultBitmap.getHeight();

            float left = boundingBoxCoordinates[0] * width;
            float top = boundingBoxCoordinates[1] * height;
            float right = boundingBoxCoordinates[2] * width;
            float bottom = boundingBoxCoordinates[3] * height;

            // 각 점을 연결하여 빨간색 선으로 네모 그리기
            canvas.drawLine(left, top, right, top, paint);   // top side
            canvas.drawLine(right, top, right, bottom, paint); // right side
            canvas.drawLine(right, bottom, left, bottom, paint); // bottom side
            canvas.drawLine(left, bottom, left, top, paint);   // left side

            // 콘솔에 객체 이름 출력
            //Log.d("ObjectDetection", "Detected object: " + name + " with score: " + score);

            // 화면에 객체 이름 출력
            runOnUiThread(() -> resultText.setText(name));

        } catch (Exception e) {
            Log.e("ImageProcessor", "Error processing image", e);
        }

        return resultBitmap;  // 그려진 Bitmap을 반환
    }

    private Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
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