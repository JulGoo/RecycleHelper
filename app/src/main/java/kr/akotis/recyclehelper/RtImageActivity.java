package kr.akotis.recyclehelper;

import android.content.pm.PackageManager;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Size;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kr.akotis.recyclehelper.myclass.DetectionResult;
import kr.akotis.recyclehelper.myclass.OverlayView;


/**
 * ML Kit 기반 실시간 객체 탐지 화면.
 * Google ML Kit을 사용하여 일반 객체를 탐지합니다.
 */
public class RtImageActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{android.Manifest.permission.CAMERA};

    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private TextView detectedObjectTextView;
    private OverlayView overlayView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_rtimage);

        previewView = findViewById(R.id.preview_view);
        detectedObjectTextView = findViewById(R.id.detected_objects_text);
        overlayView = findViewById(R.id.overlay_view);

        cameraExecutor = Executors.newSingleThreadExecutor();

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
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(640, 640))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

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
                                List<DetectionResult> detectionResults = new ArrayList<>();
                                StringBuilder detectedObjectsInfo = new StringBuilder();

                                for (DetectedObject obj : detectedObjects) {
                                    RectF boundingBox = new RectF(obj.getBoundingBox());
                                    String label = "Unknown";
                                    float score = 0f;
                                    
                                    if (!obj.getLabels().isEmpty()) {
                                        label = obj.getLabels().get(0).getText();
                                        score = obj.getLabels().get(0).getConfidence();
                                        detectedObjectsInfo.append(label).append(" ");
                                    } else {
                                        detectedObjectsInfo.append("Unknown ");
                                    }
                                    
                                    detectionResults.add(new DetectionResult(boundingBox, label, score));
                                }

                                overlayView.setDetections(detectionResults);
                                detectedObjectTextView.setText(detectedObjectsInfo.toString());
                                image.close();
                            })
                            .addOnFailureListener(e -> {
                                detectedObjectTextView.setText("Detection failed");
                                image.close();
                            });
                });

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
            } catch (Exception e) {
                Toast.makeText(this, "카메라 시작 실패", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

}