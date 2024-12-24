package kr.akotis.recyclehelper;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import kr.akotis.recyclehelper.community.CommunityActivity;
import kr.akotis.recyclehelper.notice.NoticeActivity;
import kr.akotis.recyclehelper.recycleGuide.RecycleGuideActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageButton btnImageSearch;
    private ImageButton btnVoiceSearch;
    private ImageButton btnRecyclingGuide;
    private ImageButton btnNotice;
    private ImageButton btnCommunity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        btnImageSearch = findViewById(R.id.btn_img_search);
        btnVoiceSearch = findViewById(R.id.btn_voice_search);
        btnRecyclingGuide = findViewById(R.id.btn_recycling_guide);
        btnNotice = findViewById(R.id.btn_notice);
        btnCommunity = findViewById(R.id.btn_community);

        btnImageSearch.setOnClickListener(this);
        btnVoiceSearch.setOnClickListener(this);
        btnRecyclingGuide.setOnClickListener(this);
        btnNotice.setOnClickListener(this);
        btnCommunity.setOnClickListener(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.recycler_view), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    public void onClick(View view) {
        if (view == btnImageSearch) {
            Log.v("Image", "Image");
            Intent imageSearchIntent = new Intent(MainActivity.this, ImgSearchActivity.class);
            //Intent imageSearchIntent = new Intent(MainActivity.this, RtImageActivity.class);
            startActivity(imageSearchIntent);
        } else if (view == btnVoiceSearch) {
            Log.v("VoiceSearch", "VoiceSearch");
            Intent noticeIntent = new Intent(MainActivity.this, VoiceSearchActivity.class);
            startActivity(noticeIntent);
        } else if (view == btnRecyclingGuide) {
            Log.v("RecyclingGuide", "RecyclingGuide");
            Intent recyclingGuideIntent = new Intent(MainActivity.this, RecycleGuideActivity.class);
            startActivity(recyclingGuideIntent);
        } else if (view == btnNotice) {
            Log.v("Notice", "Notice");
            Intent noticeIntent = new Intent(MainActivity.this, NoticeActivity.class);
            startActivity(noticeIntent);
        } else if (view == btnCommunity) {
            Log.v("Community", "Community");
            Intent noticeIntent = new Intent(MainActivity.this, CommunityActivity.class);
            startActivity(noticeIntent);
        }
    }
}