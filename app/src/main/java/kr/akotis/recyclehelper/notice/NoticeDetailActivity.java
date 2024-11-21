package kr.akotis.recyclehelper.notice;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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

public class NoticeDetailActivity extends AppCompatActivity {

    private TextView tvTitle, tvDate, tvContent;
    private RecyclerView recyclerImages;
    private NoticeImgAdapter imgAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notice_detail);

        // View 초기화
        tvTitle = findViewById(R.id.tv_title);
        tvDate = findViewById(R.id.tv_date);
        tvContent = findViewById(R.id.tv_content);
        recyclerImages = findViewById(R.id.recycler_images);

        Intent intent = getIntent();
        Notice notice = intent.getParcelableExtra("notice");


        if (notice != null) {
            tvTitle.setText(notice.getTitle());
            tvContent.setText(notice.getContent());
            tvDate.setText(notice.getDate());

            // 이미지 URL 리스트 처리
            String[] imgUrls = notice.getImgUrls().split(",");
            imgAdapter = new NoticeImgAdapter(List.of(imgUrls));
            recyclerImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            recyclerImages.setAdapter(imgAdapter);

            Log.d("NoticeDetailActivity", "===================================Notice: " + notice.getTitle());
        }else {
            Log.e("NoticeDetailActivity", "===================================Notice data is null");
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.recycler_view), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}