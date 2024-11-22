package kr.akotis.recyclehelper.community;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import kr.akotis.recyclehelper.R;

public class CommunityDetailActivity extends AppCompatActivity {

    private TextView tvTitle, tvContent, tvDate;
    private RecyclerView recyclerImages, recyclerComments;
    private CommunityImgAdapter imgAdapter;
    private CommentAdapter commentAdapter;
    private EditText etComment;
    private ImageButton btnMenu, btnSend;

    private DatabaseReference commentRef;
    private int thispw = 0;
    private String id = "";


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

        // 삭제, 신고 메뉴
        btnMenu = findViewById(R.id.btn_menu);

        // 팝업 메뉴 클릭 리스너
        btnMenu.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(CommunityDetailActivity.this, btnMenu);
            popupMenu.getMenuInflater().inflate(R.menu.popup, popupMenu.getMenu());

            popupMenu.setOnMenuItemClickListener(item -> {
                //Intent intent1 = new Intent(CommunityDetailActivity.this, PopupActivity.class);
                //intent1.putExtra("menu_id", item.getItemId()); // 선택된 메뉴 ID 전달
                //startActivity(intent1);


                if(item.getItemId() == 2131230892) {
                    showDeleteDialog();
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
            thispw = community.getPwd();
            id = community.getId();

            // 이미지 URL 리스트 처리
            String[] imgUrls = community.getImgUrls().split(",");
            if(imgUrls.length != 1) {
                imgAdapter = new CommunityImgAdapter(List.of(imgUrls));
                recyclerImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
                recyclerImages.setAdapter(imgAdapter);
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



    private void showDeleteDialog() {
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

            try {
                int a = Integer.parseInt(inputText);
                if(a == thispw) {
                    deleteItemFromFirebase();
                } else {
                    Toast.makeText(this, "비밀번호가 틀렸습니다.", Toast.LENGTH_SHORT).show();
                }
            }catch(Exception e) {
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

    private void deleteItemFromFirebase() {
        // Firebase DatabaseReference 초기화
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("Community");

        // 특정 ID 데이터 삭제
        databaseRef.child(id).removeValue()
                .addOnSuccessListener(aVoid -> {
                    // 성공적으로 삭제된 경우
                    Toast.makeText(this, "게시글이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    // 삭제 실패한 경우
                    Toast.makeText(this, "삭제에 실패하였습니다", Toast.LENGTH_SHORT).show();
                });
    }
}
