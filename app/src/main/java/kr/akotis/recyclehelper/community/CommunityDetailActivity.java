package kr.akotis.recyclehelper.community;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import kr.akotis.recyclehelper.R;

public class CommunityDetailActivity extends AppCompatActivity {

    private TextView tvTitle, tvContent, tvDate;
    private RecyclerView recyclerImages, recyclerComments;
    private CommunityImgAdapter imgAdapter;
    private CommentAdapter commentAdapter;
    private EditText etComment;
    private ImageButton btnMenu, btnSend;

    private DatabaseReference commentRef;
    private String thisHashedPwd = "";
    private String id = "";
    private Community community;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_community_detail);

        // 상세게시글
        tvTitle = findViewById(R.id.tv_title);
        tvContent = findViewById(R.id.tv_content);
        tvDate = findViewById(R.id.tv_date);
        recyclerImages = findViewById(R.id.recycler_images);

        // 댓글
        recyclerComments = findViewById(R.id.recycler_comments);
        etComment = findViewById(R.id.et_comment);
        btnSend = findViewById(R.id.btn_send);

        btnSend.setOnClickListener(v -> {
            String commentText = etComment.getText().toString();

            if (commentText.isEmpty()) {
                Toast.makeText(this, "댓글 내용을 입력해주세요.", Toast.LENGTH_SHORT).show();
            }else {
                showCommentPasswordDialog(commentText);
            }
        });

        // 삭제, 신고 메뉴
        btnMenu = findViewById(R.id.btn_menu);

        // 팝업 메뉴 클릭 리스너
        btnMenu.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(CommunityDetailActivity.this, btnMenu);
            popupMenu.getMenuInflater().inflate(R.menu.popup, popupMenu.getMenu());

            popupMenu.setOnMenuItemClickListener(item -> {
                Log.d("menu", "menu_title: " + item.getTitle());
                if(item.getTitle().equals("삭제")) {
                    showDeleteDialog(community);
                } else {
                    // 2131231143
                    showReportDialog();
                }

                return true;
            });
            popupMenu.show();
        });

        // Firebase 참조 설정
        Intent intent = getIntent();
        Community community = intent.getParcelableExtra("community");
        if (community != null) {
            tvTitle.setText(community.getTitle());
            tvContent.setText(community.getContent());

            // 타임스탬프를 읽어 날짜로 변환
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            String formattedDate = sdf.format(new Date(community.getDate()));

            tvDate.setText(formattedDate);
            thisHashedPwd = community.getHashedPwd();
            id = community.getId();

            // 이미지 URL 리스트 처리
            if (community.getImgUrls() != null && !community.getImgUrls().isEmpty()) { // null 또는 빈 문자열 확인
                String[] imgUrls = community.getImgUrls().split(",");
                List<String> imgUrlList = Arrays.asList(imgUrls); // Arrays.asList 사용으로 불변 List 생성

                if (!imgUrlList.isEmpty()) { // 리스트가 비어있지 않은 경우
                    imgAdapter = new CommunityImgAdapter(imgUrlList);

                    // RecyclerView 초기화
                    recyclerImages.setHasFixedSize(true); // 성능 최적화
                    recyclerImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
                    recyclerImages.setAdapter(imgAdapter);

                    // 디버깅 로그 추가
                    Log.d("RecyclerView Init", "RecyclerView initialized with " + imgUrlList.size() + " items.");
                } else {
                    Log.e("RecyclerView Init", "Image URL list is empty.");
                }
            } else {
                Log.e("RecyclerView Init", "Image URLs are null or empty.");
            }

            // Firebase에서 댓글 경로 설정
            commentRef = FirebaseDatabase.getInstance()
                    .getReference("Community")
                    .child(community.getId())
                    .child("comments");

            setupCommentRecyclerView();

            Log.e("CommunityDetailActivity", "Community: " + community.getTitle());
        } else {
            Log.e("CommunityDetailActivity", "Community data is null");
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.recycler_view), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupCommentRecyclerView() {
        // FirebaseRecyclerOptions 설정
        FirebaseRecyclerOptions<Comment> options = new FirebaseRecyclerOptions.Builder<Comment>()
                .setQuery(commentRef, Comment.class)
                .build();

        // CommentAdapter 초기화 및 RecyclerView 설정
        commentAdapter = new CommentAdapter(options);
        recyclerComments.setLayoutManager(new LinearLayoutManager(this));
        recyclerComments.setAdapter(commentAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (commentAdapter != null) {
            commentAdapter.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (commentAdapter != null) {
            commentAdapter.stopListening();
        }
    }

    private String hashedPwd(String pwd) {
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(pwd.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();  // SHA-256 해시된 비밀번호 반환
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void showDeleteDialog(Object target) {
        // AlertDialog 빌더 생성
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("정말로 삭제하시겠습니까?");

        // 레이아웃 설정
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10); // 여백 조정

        // 안내 텍스트 추가
        TextView textView = new TextView(this);
        textView.setText("게시글의 비밀번호를 입력해주세요.");
        textView.setTextSize(16);
        layout.addView(textView);

        // 텍스트 입력란 추가
        EditText editText = new EditText(this);
        editText.setHint("여기에 입력하세요");
        layout.addView(editText);

        builder.setView(layout);

        // 확인 버튼 설정
        builder.setPositiveButton("확인", (dialog, which) -> {
            // 입력값 처리
            String inputText = editText.getText().toString().trim();
            if(inputText.equals("")) {
                Toast.makeText(this, "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }

            // 입력한 비밀번호 해시 처리
            String hashedInput = hashedPwd(inputText);

//            try {
//                if(hashedInput != null && hashedInput.equals(thisHashedPwd)) {
//                    deleteItemFromFirebase();
//                } else {
//                    Toast.makeText(this, "비밀번호가 틀렸습니다.", Toast.LENGTH_SHORT).show();
//                }
//            }catch(Exception e) {
//                Toast.makeText(this, "지원하지 않는 형식입니다.", Toast.LENGTH_SHORT).show();
//                e.printStackTrace();
//            }
//
//            dialog.dismiss(); // 팝업 닫기
//        });

            // 삭제 대상이 게시글인지 댓글인지 확인
            try {
                if (target instanceof Community) {
                    Community community = (Community) target;
                    if (hashedInput != null && hashedInput.equals(community.getHashedPwd())) {
                        deleteItemFromFirebase(community);
                    } else {
                        Toast.makeText(this, "비밀번호가 틀렸습니다.", Toast.LENGTH_SHORT).show();
                    }
                } else if (target instanceof Comment) {
                    Comment comment = (Comment) target;
                    if (hashedInput != null && hashedInput.equals(comment.getPwd())) {
                        deleteCommentFromFirebase(comment);
                    } else {
                        Toast.makeText(this, "비밀번호가 틀렸습니다.", Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (Exception e) {
                Toast.makeText(this, "지원하지 않는 형식입니다.", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }

            dialog.dismiss(); // 팝업 닫기
        });

        // 취소 버튼 설정
        builder.setNegativeButton("취소", (dialog, which) -> dialog.dismiss());

        // 다이얼로그 표시
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showReportDialog() {
        // AlertDialog 빌더 생성
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("해당 게시글을 신고하시겠습니까?");

        // 레이아웃 설정
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10); // 여백 조정

        builder.setView(layout);

        // 확인 버튼 설정
        builder.setPositiveButton("확인", (dialog, which) -> {
            // 입력값 처리
            Toast.makeText(this, "신고가 완료 되었습니다.", Toast.LENGTH_SHORT).show();

            dialog.dismiss(); // 팝업 닫기
        });

        // 취소 버튼 설정
        builder.setNegativeButton("취소", (dialog, which) -> dialog.dismiss());

        // 다이얼로그 표시
        AlertDialog dialog = builder.create();
        dialog.show();
    }

//    private void deleteItemFromFirebase() {
//        // Firebase DatabaseReference 초기화
//        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("Community");
//
//        // 특정 ID 데이터 삭제
//        databaseRef.child(id).removeValue()
//                .addOnSuccessListener(aVoid -> {
//                    // 성공적으로 삭제된 경우
//                    Toast.makeText(this, "게시글이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
//                    finish();
//                })
//                .addOnFailureListener(e -> {
//                    // 삭제 실패한 경우
//                    Toast.makeText(this, "삭제에 실패하였습니다", Toast.LENGTH_SHORT).show();
//                });
//    }

    // 게시글 삭제
    private void deleteItemFromFirebase(Community community) {
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("Community");
        databaseRef.child(community.getId()).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "게시글이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "삭제에 실패하였습니다", Toast.LENGTH_SHORT).show();
                });
    }

    //댓글 작성 다이얼로그
    private void showCommentPasswordDialog(String commentText) {
        // 다이얼로그 빌더 생성
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("댓글 비밀번호를 입력해주세요.");

        // 비밀번호 입력란 생성
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);  // 여백 조정

        // 안내 텍스트 추가
        TextView textView = new TextView(this);
        textView.setText("댓글 비밀번호를 입력하세요.");
        textView.setTextSize(16);
        layout.addView(textView);

        // 비밀번호 입력란 추가
        EditText etPwd = new EditText(this);
        etPwd.setHint("여기에 비밀번호를 입력하세요");
        etPwd.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);  // 비밀번호 입력 스타일
        layout.addView(etPwd);

        builder.setView(layout);

        // 확인 버튼 설정
        builder.setPositiveButton("확인", (dialog, which) -> {
            // 입력된 비밀번호 가져오기
            String inputPwd = etPwd.getText().toString().trim();

            // 비밀번호가 비어있는지 확인
            if(inputPwd.isEmpty()) {
                Toast.makeText(this, "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 비밀번호 해시 처리
            String hashedPwd = hashedPwd(inputPwd);

            // 비밀번호가 유효하면 Firebase에 댓글 추가
            sendToFirebase(commentText, hashedPwd);
        });

        // 취소 버튼 설정
        builder.setNegativeButton("취소", (dialog, which) -> dialog.dismiss());

        // 다이얼로그 표시
        builder.create().show();
    }

    // 댓글 삭제
    private void deleteCommentFromFirebase(Comment comment) {
        DatabaseReference commentRef = FirebaseDatabase.getInstance()
                .getReference("Community")
                .child(id)
                .child("comments");

        commentRef.child(comment.getId()).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "댓글이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "댓글 삭제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // firebase 댓글 추가
    private void sendToFirebase(String commentText, String hashedPwd) {
        DatabaseReference communityRef = FirebaseDatabase.getInstance().getReference("Community").child(id).child("comments");
        String key = communityRef.push().getKey();
        if(key != null) {
            Map<String, Object> commentData = new HashMap<>();
            commentData.put("content", commentText);
            commentData.put("date", System.currentTimeMillis());
            commentData.put("pwd", hashedPwd);
            commentData.put("report", 0);

            communityRef.child(key).setValue(commentData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "댓글이 추가되었습니다.", Toast.LENGTH_SHORT).show();
                        etComment.setText("");
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "댓글 추가 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void setupCommentPopupMenu(View view, Comment comment) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenuInflater().inflate(R.menu.popup, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            if(item.getTitle().equals("삭제")) {
                // 댓글 삭제
                showDeleteDialog(comment);
            } else {
                showReportDialog();
            }
            return true;
        });

        popupMenu.show();
    }


}
