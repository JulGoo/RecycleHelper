package kr.akotis.recyclehelper;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.annotation.Nullable;

public class FullScreenImgActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;
    Context context = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_img);

        PhotoView fullScreenImageView = findViewById(R.id.fullScreenImageView);
        //ImageButton saveImageButton = findViewById(R.id.saveImageButton);
        ImageButton closeImageButton = findViewById(R.id.closeImageButton);

        // Intent로 전달받은 이미지 URL 가져오기
        String imageUrl = getIntent().getStringExtra("imageUrl");

        // Glide를 이용하여 이미지 로드
        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.error)
                .into(fullScreenImageView);

        /* / 이미지 저장
        saveImageButton.setOnClickListener(v -> {
            if (checkPermission()){
                //saveImageToLocal(imageUrl);
            } else {
                requestPermissions();
            }
        });
         */

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
            return false;
            //return ContextCompat.checkSelfPermission(context.getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            //return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    // 권한 요청
    private void requestPermissions() {
        // Android Q 이상에서는 별도의 권한이 필요 없으므로 바로 저장 가능
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Toast.makeText(context, "이미지 저장이 가능해졌습니다.", Toast.LENGTH_SHORT).show();
        } else {
            // WRITE_EXTERNAL_STORAGE 권한을 요청합니다.
            ActivityCompat.requestPermissions((Activity) context, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }


    // 이미지 저장
    private void saveImageToLocal(String imageUrl, Context context) {
        Glide.with(context)
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

                            // 이미지 저장 후 갤러리가 새 파일을 인식하도록 미디t어 스캐너 호출
                            MediaScannerConnection.scanFile(FullScreenImgActivity.this,
                                    new String[]{file.getAbsolutePath()},
                                    null,
                                    (path, uri) -> {
                                        // 미디어 스캔이 완료되었을 때 호출되는 콜백
                                        Log.d("MediaScanner", "Scanned " + path);
                                        runOnUiThread(() -> {
                                            Toast.makeText(context, "이미지가 저장되었습니다.", Toast.LENGTH_SHORT).show();
                                        });
                                    });

                        } catch (IOException e) {
                            e.printStackTrace();
                            runOnUiThread(() -> {
                                Toast.makeText(context, "이미지 저장에 실패했습니다.", Toast.LENGTH_SHORT).show();
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





    public void show(Context context, Uri imageUri) {
        context = context;
        // 다이얼로그 생성
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.fragment_img_dialog_view);
        //dialog.setCancelable(false); // 백버튼으로 닫히지 않도록 설정

        // 다이얼로그 뷰 연결
        ImageView imageView = dialog.findViewById(R.id.dialogImageView);
        Button buttonClose = dialog.findViewById(R.id.dialogButtonClose);
        //Button buttonSave = dialog.findViewById(R.id.dialogButtonSave);

        // 이미지 설정
        if (imageUri != null && imageUri.toString().startsWith("gs://")) {
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageReference = storage.getReferenceFromUrl(imageUri.toString());

            int newWidth = dialog.getContext().getResources().getDisplayMetrics().widthPixels - 200;
            Log.d("Image WIDTH", "Image newWidth is : " + newWidth);
            Context finalContext = context;
            storageReference.getBytes(Long.MAX_VALUE).addOnSuccessListener(bytes -> {
                Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bm != null) {
                    int width = bm.getWidth();
                    int height = bm.getHeight();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // 비율 계산
                            float aspectRatio = (float) width / (float) height;
                            //int newWidth = getResources().getDisplayMetrics().widthPixels;  // 화면 전체 너비
                            int newHeight = (int) (newWidth / aspectRatio);  // 비율을 맞춰서 높이 계산

                            // ImageView 크기 설정
                            ViewGroup.LayoutParams params = imageView.getLayoutParams();
                            params.width = newWidth;
                            params.height = newHeight;
                            imageView.setLayoutParams(params);  // ImageView에 새로운 LayoutParams 설정

                            // 비트맵을 ImageView에 설정
                            imageView.setImageBitmap(bm);
                        }
                    });
                }
            }).addOnFailureListener(e -> {
               Log.e("Firebase Error", "Failed to load image: " + e);
                Toast.makeText(finalContext, "이미지를 로드할 수 없습니다.", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        } else if (imageUri != null) {
            //imageView.setImageURI(imageUri);
            //Glide.with(imageView.getContext())
            //        .load(imageUri)
            //        .override(Target.SIZE_ORIGINAL)
            //        .error(R.drawable.error)
            //        .into(imageView);

            int newWidth = dialog.getContext().getResources().getDisplayMetrics().widthPixels - 200;
            Log.d("Image WIDTH", "Image newWidth is : " + newWidth);
            new Thread(new Runnable() {
               @Override
               public void run() {
                   Bitmap bm = null;
                   try {
                       URL url = new URL(imageUri.toString());
                       URLConnection conn = url.openConnection();
                       conn.connect();
                       InputStream is = conn.getInputStream();
                       BufferedInputStream bis = new BufferedInputStream(is);
                       bm = BitmapFactory.decodeStream(bis);
                       bis.close();
                       is.close();
                   } catch(Exception e) {
                       Log.d("URL ERROR", "open url error : " + e);
                   }

                   if (bm != null) {
                       Bitmap finalBm = bm;
                       int width = bm.getWidth();
                       int height = bm.getHeight();

                       runOnUiThread(new Runnable() {
                           @Override
                           public void run() {
                               // 비율 계산
                               float aspectRatio = (float) width / (float) height;
                               //int newWidth = getResources().getDisplayMetrics().widthPixels;  // 화면 전체 너비
                               int newHeight = (int) (newWidth / aspectRatio);  // 비율을 맞춰서 높이 계산

                               // ImageView 크기 설정
                               ViewGroup.LayoutParams params = imageView.getLayoutParams();
                               params.width = newWidth;
                               params.height = newHeight;
                               imageView.setLayoutParams(params);  // ImageView에 새로운 LayoutParams 설정

                               // 비트맵을 ImageView에 설정
                               imageView.setImageBitmap(finalBm);
                           }
                       });
                   }
               }
            }).start();



        } else {
            Toast.makeText(context, "이미지를 로드할 수 없습니다.", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            return;
        }

        // 닫기 버튼
        buttonClose.setOnClickListener(v -> dialog.dismiss());

        /*/ 저장 버튼
        buttonSave.setOnClickListener(v -> {
            if (checkPermission()){
                saveImageToLocal(String.valueOf(imageUri), dialog.getContext());
            } else {
                Toast.makeText(dialog.getContext(), "이미지 저장에 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
            //saveImageToLocal(String.valueOf(imageUri), dialog.getContext());
            dialog.dismiss(); // 저장 후 다이얼로그 닫기
        });

         */

        // 다이얼로그 표시
        dialog.show();
    }


}
