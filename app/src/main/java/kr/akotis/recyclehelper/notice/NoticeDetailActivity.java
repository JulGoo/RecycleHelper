package kr.akotis.recyclehelper.notice;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import kr.akotis.recyclehelper.R;

public class NoticeDetailActivity extends AppCompatActivity {

    TextView tvTitle = findViewById(R.id.tv_notice_content_title);
    TextView tvContent = findViewById(R.id.tv_notice_content_content);
    //ImageView ivImage = findViewById(R.id.iv_notice_image);
    TextView tvDate = findViewById(R.id.tv_notice_content_date);

    Notice notice = getIntent().getParcelableExtra("notice");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notice_detail);

        if (notice != null) {
            tvTitle.setText(notice.getTitle());
            tvContent.setText(notice.getContent());
            tvDate.setText(notice.getDate());

//            if (notice.getImgUrl() != null && !notice.getImgUrl().isEmpty()) {
//                ivImage.setVisibility(View.VISIBLE);
//                Glide.with(this).load(notice.getImgUrl()).into(ivImage); // 이미지 로드
//            }
            Log.d("NoticeDetailActivity", "Notice: " + notice.getTitle());
            Notice notice = (Notice)getIntent().getParcelableExtra("notice");
        }else {
            Log.e("NoticeDetailActivity", "Notice data is null===================================");
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.recycler_view), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}