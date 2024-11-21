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

import java.util.List;

import kr.akotis.recyclehelper.R;
import kr.akotis.recyclehelper.notice.NoticeImgAdapter;

public class CommunityDetailActivity extends AppCompatActivity {

    private TextView tvTitle, tvContent;
    private RecyclerView recyclerImages, recyclerComments;
    private CommunityImgAdapter imgAdapter;
    private EditText etComment;
    private ImageButton btnMenu, btnSend;

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

        // 삭제, 신고 기능
        btnMenu = findViewById(R.id.btn_menu);

        Intent intent = getIntent();
        Community community = intent.getParcelableExtra("community");
        Comment comment = intent.getParcelableExtra("comment");

        if (community != null) {
            tvTitle.setText(community.getTitle());
            tvContent.setText(community.getContent());

            // 이미지 URL 리스트 처리
            String[] imgUrls = community.getImgUrls().split(",");
            imgAdapter = new CommunityImgAdapter(List.of(imgUrls));
            recyclerImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            recyclerImages.setAdapter(imgAdapter);

            Log.e("CommunityDetailActivity", "===================================Community: " + community.getTitle());
        }else {
            Log.e("CommunityDetailActivity", "===================================Community data is null");
        }

        if (comment != null) {
            recyclerComments.setLayoutManager(new LinearLayoutManager(this));
            //recyclerComments.setAdapter(new CommentAdapter(List.of(comment)));
        }else {
            Log.e("CommunityDetailActivity", "===================================Comment data is null");
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.recycler_view), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}