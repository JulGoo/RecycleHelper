package kr.akotis.recyclehelper;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.Image;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Base64;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.TextView;
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
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kr.akotis.recyclehelper.myclass.OverlayView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class RtImageActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{android.Manifest.permission.CAMERA};

    private ExecutorService cameraExecutor;
    private TextView detectedObjectTextView;
    private OverlayView overlayView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_rtimage);

        detectedObjectTextView = findViewById(R.id.detected_objects_text);
        overlayView = findViewById(R.id.overlay_view);

        // Initialize CameraX
        cameraExecutor = Executors.newSingleThreadExecutor();


        // 카메라 권한 확인 및 요청
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
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

                Preview preview = new Preview.Builder().build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(640, 640))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                // Configure the object detector
                ObjectDetectorOptions options = new ObjectDetectorOptions.Builder()
                        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                        .enableMultipleObjects()
                        .enableClassification()
                        .build();

                ObjectDetector objectDetector = ObjectDetection.getClient(options);

                imageAnalysis.setAnalyzer(cameraExecutor, image -> {
                    @SuppressWarnings("UnsafeOptInUsageError")
                    InputImage inputImage = InputImage.fromMediaImage(
                            image.getImage(),
                            image.getImageInfo().getRotationDegrees()
                    );

                    objectDetector.process(inputImage)
                            .addOnSuccessListener(detectedObjects -> {
                                overlayView.clear(); // Clear previous drawings
                                StringBuilder detectedObjectsInfo = new StringBuilder();

                                for (DetectedObject obj : detectedObjects) {
                                    RectF boundingBox = new RectF(obj.getBoundingBox());
                                    overlayView.drawBoundingBox(boundingBox);

                                    if (!obj.getLabels().isEmpty()) {
                                        detectedObjectsInfo.append(obj.getLabels().get(0).getText())
                                                .append(" ");
                                    } else {
                                        detectedObjectsInfo.append("Unknown ");
                                    }
                                }

                                detectedObjectTextView.setText(detectedObjectsInfo.toString());
                                image.close();
                            })
                            .addOnFailureListener(e -> {
                                detectedObjectTextView.setText("Detection failed");
                                image.close();
                            });
                });


                //ProcessCameraProvider camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
                Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

}