package kr.akotis.recyclehelper.community;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import kr.akotis.recyclehelper.R;

public class CommunityWriteActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_CAMERA = 1;
    private static final int REQUEST_CODE_GALLERY = 2;

    private EditText etTitle, etContent, etPassword;
    private Button btnAddPhoto, btnSubmit;

    private Uri imageUri;
    private DatabaseReference databaseRef;

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

        // 사진 추가 버튼 클릭 리스너
        btnAddPhoto.setOnClickListener(v -> openImagePickerDialog());

        // 작성 완료 버튼
        btnSubmit.setOnClickListener(v -> submitPost());
    }

    private void openImagePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("사진 선택");
        builder.setItems(new CharSequence[]{"카메라", "갤러리"}, (dialog, which) -> {
            if (which == 0) {
                // 카메라 선택
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, REQUEST_CODE_CAMERA);
            } else {
                // 갤러리 선택
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_CODE_GALLERY);
            }
        });
        builder.show();
    }

    // Activity 결과 처리 (카메라 또는 갤러리에서 이미지를 받기)
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
        }
    }

    // Bitmap을 Uri로 변환 (카메라에서 찍은 사진)
    private Uri getImageUriFromBitmap(Bitmap bitmap) {
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "title", null);
        return Uri.parse(path);
    }

    // Firebase에 이미지를 업로드하는 메서드
    private void uploadImageToFirebase(Uri imageUri) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference imageRef = storageRef.child("images/" + UUID.randomUUID().toString() + ".jpg");

        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();
                        saveImageUrlToDatabase(downloadUrl);
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CommunityWriteActivity.this, "사진 업로드 실패", Toast.LENGTH_SHORT).show();
                });
    }

    // Firebase Database에 이미지 URL 저장
    private void saveImageUrlToDatabase(String imageUrl) {
        String communityId = databaseRef.push().getKey();
        if (communityId != null) {
            Community community = new Community(
                    communityId,
                    etTitle.getText().toString(),
                    etContent.getText().toString(),
                    System.currentTimeMillis(),
                    imageUrl,
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

        if (imageUri != null) {
            uploadImageToFirebase(imageUri);
        } else {
            saveImageUrlToDatabase(null);
        }
    }
}