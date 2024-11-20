package kr.akotis.recyclehelper.notice;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;

import kr.akotis.recyclehelper.R;

public class FullScreenImgNoticeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_img_notice);

        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) ImageView fullScreenImageView = findViewById(R.id.fullScreenImageView);

        // Intent로 전달받은 이미지 URL 가져오기
        String imageUrl = getIntent().getStringExtra("imageUrl");

        // Glide를 이용하여 이미지 로드
        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.error)
                .into(fullScreenImageView);

        // 클릭 시 액티비티 종료
        fullScreenImageView.setOnClickListener(v -> finish());
    }
}