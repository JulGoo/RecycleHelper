package kr.akotis.recyclehelper;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.annotation.Nullable;

public class FullScreenImgActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_img);

        PhotoView fullScreenImageView = findViewById(R.id.fullScreenImageView);
        ImageButton saveImageButton = findViewById(R.id.saveImageButton);
        ImageButton closeImageButton = findViewById(R.id.closeImageButton);

        // Intent로 전달받은 이미지 URL 가져오기
        String imageUrl = getIntent().getStringExtra("imageUrl");

        // Glide를 이용하여 이미지 로드
        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.error)
                .into(fullScreenImageView);

        // 이미지 저장
        saveImageButton.setOnClickListener(v -> {
            if (checkPermission()){
                saveImageToLocal(imageUrl);
            } else {
                requestPermissions();
            }
        });

        // 닫기 버튼 클릭 시 액티비티 종료
        closeImageButton.setOnClickListener(v -> {
            // 로그 추가하여 버튼 클릭 시 어떤 동작을 하는지 확인
            Log.d("FullScreenImgActivity", "Close button clicked.");
            try {
                //onBackPressed();  // 이전 액티비티로 돌아가기
                finish();
            } catch (Exception e) {
                Log.e("FullScreenImgActivity", "Error when closing the activity", e);
                Toast.makeText(FullScreenImgActivity.this, "Error while closing the activity", Toast.LENGTH_SHORT).show();
            }
            Log.e("Activity Finish", "Error finish acitivyt ~~");
        });
    }

    //권한 확인
    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Scoped Storage에서는 추가적인 권한이 필요 없음
            return true;
        } else {
            return ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    // 권한 요청
    private void requestPermissions() {
        // Android Q 이상에서는 별도의 권한이 필요 없으므로 바로 저장 가능
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Toast.makeText(this, "이미지 저장이 가능해졌습니다.", Toast.LENGTH_SHORT).show();
        } else {
            // WRITE_EXTERNAL_STORAGE 권한을 요청합니다.
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 허용되었을 때 이미지 저장
                Toast.makeText(this, "권한이 허용되었습니다.", Toast.LENGTH_SHORT).show();
            } else {
                // 권한이 거부되었을 때
                Toast.makeText(this, "권한이 거부되었습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 이미지 저장
    private void saveImageToLocal(String imageUrl) {
        Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @androidx.annotation.Nullable Transition<? super Bitmap> transition) {
                        try {
                            File directory = new File(Environment.getExternalStorageDirectory() + "/Download");
                            if (!directory.exists()) {
                                directory.mkdirs();
                            }

                            String fileName = "image_" + System.currentTimeMillis() + ".jpg";
                            File file = new File(directory, fileName);

                            FileOutputStream out = new FileOutputStream(file);
                            resource.compress(Bitmap.CompressFormat.JPEG, 100, out);
                            out.flush();
                            out.close();

                            // 이미지 저장 후 갤러리가 새 파일을 인식하도록 미디어 스캐너 호출
                            MediaScannerConnection.scanFile(FullScreenImgActivity.this,
                                    new String[]{file.getAbsolutePath()},
                                    null,
                                    (path, uri) -> {
                                        // 미디어 스캔이 완료되었을 때 호출되는 콜백
                                        Log.d("MediaScanner", "Scanned " + path);
                                        runOnUiThread(() -> {
                                            Toast.makeText(FullScreenImgActivity.this, "이미지가 저장되었습니다.", Toast.LENGTH_SHORT).show();
                                        });
                                    });

                        } catch (IOException e) {
                            e.printStackTrace();
                            runOnUiThread(() -> {
                                Toast.makeText(FullScreenImgActivity.this, "이미지 저장에 실패했습니다.", Toast.LENGTH_SHORT).show();
                            });
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // 이미지 로드 취소 시 호출됨
                    }
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
