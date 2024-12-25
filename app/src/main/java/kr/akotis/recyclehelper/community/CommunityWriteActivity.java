package kr.akotis.recyclehelper.community;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

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

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

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

        InputFilter[] filters = new InputFilter[]{
                new InputFilter.LengthFilter(4),
                (source, start, end, dest, dstart, dend) -> {
                    for (int i = start; i < end; i++) {
                        if (!Character.isDigit(source.charAt(i))) {
                            return "";
                        }
                    }
                    return null; // 입력 가능한 경우
                }
        };
        etPassword.setFilters(filters);
        etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); // 입력된 값은 비밀번호처럼 *로 표시됨
    }

    //권한 확인
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
    private void uploadImageToFirebase(Uri imageUri, CountDownLatch latch) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference imageRef = storageRef.child("Community/" + UUID.randomUUID().toString() + ".jpg");

        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();
                    saveURLs(downloadUrl); // URL 저장
                    latch.countDown(); // 업로드 완료 시 카운터 감소
                }))
                .addOnFailureListener(e -> {
                    Toast.makeText(CommunityWriteActivity.this, "사진 업로드 실패", Toast.LENGTH_SHORT).show();
                    latch.countDown(); // 실패해도 카운터 감소
                });
    }

    private void saveURLs(String url) {
        if (!savedUrls.isEmpty()) {
            savedUrls += ","; // 기존에 값이 있으면 콤마 추가
        }
        savedUrls += url;
    }

    // Firebase Database에 이미지 URL 저장
    private void saveImageUrlToDatabase(String hashedPwd) {
        String communityId = databaseRef.push().getKey();
        if (communityId != null) {
            Community community = new Community(
                    communityId,
                    etTitle.getText().toString(),
                    etContent.getText().toString(),
                    System.currentTimeMillis(),
                    savedUrls,
                    hashedPwd,
                    0,  // 신고 횟수
                    new HashMap<>() // 댓글
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

        if (password.length() != 4) {  // 입력값이 4자리가 아닐 경우
            Toast.makeText(this, "비밀번호는 4자리를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return; // 4자리가 아니면 더 이상 진행하지 않음
        }

        // 비밀번호 해시 처리
        String hashedPwd = hashPwd(password);
        if (hashedPwd == null) {
            Toast.makeText(CommunityWriteActivity.this, "비밀번호 처리에 문제가 있습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!imgUrls.isEmpty()) {
            // 업로드할 이미지 수를 기반으로 CountDownLatch 생성
            CountDownLatch latch = new CountDownLatch(imgUrls.size());

            for (String imgu : imgUrls) {
                Uri uri = Uri.parse(imgu);
                uploadImageToFirebase(uri, latch); // latch 전달
            }

            // 새로운 스레드에서 모든 업로드 완료를 기다림
            new Thread(() -> {
                try {
                    latch.await(); // 모든 업로드가 끝날 때까지 대기
                    runOnUiThread(() -> saveImageUrlToDatabase(hashedPwd)); // UI 스레드에서 데이터 저장 호출
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        } else {
            saveImageUrlToDatabase(hashedPwd); // 이미지가 없으면 바로 데이터 저장
        }
    }

    // 비밀번호 암호화
    private String hashPwd(String pwd) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(pwd.getBytes());
            StringBuilder hexString = new StringBuilder();

            //해시 값을 16진수 문자열로 변환
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
