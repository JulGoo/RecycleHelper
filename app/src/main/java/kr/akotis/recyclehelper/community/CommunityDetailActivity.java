package kr.akotis.recyclehelper.community;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
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

import java.util.List;

import kr.akotis.recyclehelper.R;
import kr.akotis.recyclehelper.notice.NoticeImgAdapter;

public class CommunityDetailActivity extends AppCompatActivity {

    private TextView tvTitle, tvContent;
    private RecyclerView recyclerImages, recyclerComments;
    private CommunityImgAdapter imgAdapter;
    private CommentAdapter commentAdapter;
    private EditText etComment;
    private ImageButton btnMenu, btnSend;

    private DatabaseReference commentRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_community_detail);

        // 상세게시글
        tvTitle = findViewById(R.id.tv_title);
        //tvDate = findViewById(R.id.tv_date);
        tvContent = findViewById(R.id.tv_content);
        recyclerImages = findViewById(R.id.recycler_images);

        // 댓글
        recyclerComments = findViewById(R.id.recycler_comments);
        etComment = findViewById(R.id.et_comment);
        btnSend = findViewById(R.id.btn_send);

        // 삭제, 신고 메뉴
        btnMenu = findViewById(R.id.btn_menu);

        // Firebase 참조 설정
        Intent intent = getIntent();
        Community community = intent.getParcelableExtra("community");
        if (community != null) {
            tvTitle.setText(community.getTitle());
            tvContent.setText(community.getContent());

            // 이미지 URL 리스트 처리
            String[] imgUrls = community.getImgUrls().split(",");
            imgAdapter = new CommunityImgAdapter(List.of(imgUrls));
            recyclerImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            recyclerImages.setAdapter(imgAdapter);

            // Firebase에서 댓글 경로 설정
            commentRef = FirebaseDatabase.getInstance()
                    .getReference("Community")
                    .child(community.getId())
                    .child("comments");

            setupCommentRecyclerView();

            Log.e("CommunityDetailActivity", "===================================Community: " + community.getTitle());
        } else {
            Log.e("CommunityDetailActivity", "===================================Community data is null");
        }

        // 댓글 전송 버튼 클릭 리스너
//        btnSend.setOnClickListener(v -> {
//            String commentContent = etComment.getText().toString();
//            if (!commentContent.isEmpty()) {
//                addComment(commentContent);
//            }
//        });

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

        commentRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // 댓글이 존재하면 데이터를 처리
                    for (DataSnapshot commentSnapshot : dataSnapshot.getChildren()) {
                        String content = commentSnapshot.child("content").getValue(String.class);
                        Long date = commentSnapshot.child("date").getValue(Long.class);
                        int report = commentSnapshot.child("report").getValue(Integer.class);

                        // 필요한 처리를 진행 (예: RecyclerView에 데이터 추가)
                        Log.e("CommunityDetailActivity", "Comment: " + content);
                    }
                } else {
                    // 댓글이 없을 때
                    Log.e("CommunityDetailActivity", "No comments found.");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("CommunityDetailActivity", "Failed to load comments.", databaseError.toException());
            }
        });

        Log.e("CommunityDetailActivity", "===========!!!!!!!!!!!!!!!!!!!!!!=============="+commentAdapter.getItemCount());
    }

    private void addComment(String commentContent) {
//        String commentId = commentRef.push().getKey();
//        if (commentId != null) {
//            Comment newComment = new Comment(content, System.currentTimeMillis(), 0, 0);
//            commentRef.child(commentId).setValue(newComment)
//                    .addOnSuccessListener(aVoid -> etComment.setText("")) // 입력 필드 초기화
//                    .addOnFailureListener(e -> Log.e("CommunityDetailActivity", "Failed to add comment", e));
//        }
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
}