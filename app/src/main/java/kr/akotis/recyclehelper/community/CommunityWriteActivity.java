package kr.akotis.recyclehelper.community;


import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import kr.akotis.recyclehelper.R;

public class CommunityWriteActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{android.Manifest.permission.CAMERA};
    private static final int REQUEST_CODE_CAMERA = 1;
    private static final int REQUEST_CODE_GALLERY = 2;

    private EditText etTitle, etContent, etPassword;
    private Button btnAddPhoto, btnSubmit;

    private Uri imageUri;
    private DatabaseReference databaseRef;


    private RecyclerView recyclerImages;
    private CommunityImgAdapter imgAdapter;
    private ArrayList<String> imgUrls = new ArrayList<>();
    private String savedUrls = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_community_write);

        // Firebase 참조 설정
        databaseRef = FirebaseDatabase.getInstance().getReference("Community");

        etTitle = findViewById(R.id.et_title);
        etContent = findViewById(R.id.et_content);
        etPassword = findViewById(R.id.et_password);
        btnAddPhoto = findViewById(R.id.btn_add_photo);
        btnSubmit = findViewById(R.id.btn_submit);

        recyclerImages = findViewById(R.id.recycler_images);

        // 사진 추가 버튼 클릭 리스너
        btnAddPhoto.setOnClickListener(v -> openImagePickerDialog());

        // 작성 완료 버튼
        btnSubmit.setOnClickListener(v -> submitPost());
    }



    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // 권한 요청 결과 처리
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                openCamera();
            } else {
                Toast.makeText(this, "카메라 권한이 없습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 카메라 실행 메서드
    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CODE_CAMERA);
    }

    // Activity 결과 처리 (카메라에서 사진을 찍었을 때)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_CAMERA && data != null) {
                // 카메라로 찍은 사진 처리
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                imageUri = getImageUriFromBitmap(photo);
            } else if (requestCode == REQUEST_CODE_GALLERY && data != null) {
                // 갤러리에서 선택한 이미지 처리
                imageUri = data.getData();
            }

            imgUrls.add(imageUri.toString());
            imgAdapter = new CommunityImgAdapter(imgUrls);
            recyclerImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            recyclerImages.setAdapter(imgAdapter);
        }
    }

    private void openImagePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("사진 선택");
        builder.setItems(new CharSequence[]{"카메라", "갤러리"}, (dialog, which) -> {
            if (which == 0) {
                // 카메라 선택
                if (allPermissionsGranted()) {
                    openCamera();
                } else {
                    ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
                }
            } else {
                // 갤러리 선택
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_CODE_GALLERY);
            }
        });
        builder.show();
    }

    // Bitmap을 Uri로 변환 (카메라에서 찍은 사진)
    private Uri getImageUriFromBitmap(Bitmap bitmap) {
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "title", null);
        return Uri.parse(path);
    }

    // Firebase에 이미지를 업로드하는 메서드
    private String uploadImageToFirebase(Uri imageUri) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference imageRef = storageRef.child("images/" + UUID.randomUUID().toString() + ".jpg");

        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();
                        saveURLs(downloadUrl);
                        Log.d("TEST", downloadUrl);
                        //saveImageUrlToDatabase(downloadUrl);
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CommunityWriteActivity.this, "사진 업로드 실패", Toast.LENGTH_SHORT).show();
                });
        return null;
    }

    private void saveURLs(String url) {
        if (!savedUrls.isEmpty()) {
            savedUrls += ","; // 기존에 값이 있으면 콤마 추가
        }
        savedUrls += url;
    }

    // Firebase Database에 이미지 URL 저장
    private void saveImageUrlToDatabase() {
        String communityId = databaseRef.push().getKey();
        if (communityId != null) {
            Community community = new Community(
                    communityId,
                    etTitle.getText().toString(),
                    etContent.getText().toString(),
                    System.currentTimeMillis(),
                    savedUrls,
                    Integer.parseInt(etPassword.getText().toString().trim()),
                    0,  // 신고 횟수
                    new HashMap<>()
            );

            databaseRef.child(communityId).setValue(community)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(CommunityWriteActivity.this, "게시글이 등록되었습니다.", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(CommunityWriteActivity.this, "게시글 등록 실패", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    // 작성 완료 버튼 클릭 시
    private void submitPost() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(content) || TextUtils.isEmpty(password)) {
            Toast.makeText(CommunityWriteActivity.this, "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        System.out.println("THIS imgUrls SIZE IS : " + imgUrls.size());
        if(!imgUrls.isEmpty()) {
            System.out.println("NOT EMPTY");
            for(String imgu : imgUrls) {
                Uri uri = Uri.parse(imgu);
                Log.d("Test -- " , imgu);
                uploadImageToFirebase(uri);
            }
        }

        Log.d("VALUE : ", savedUrls);
        saveImageUrlToDatabase();

        // 초기화
        savedUrls = "";
        imgUrls = new ArrayList<>();
        imageUri = null;
    }
}
