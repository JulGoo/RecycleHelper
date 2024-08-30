package kr.kro.barrierfree.recyclehelper;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnImageSearch;
    private Button btnRecyclingGuide;
    private Button btnNotice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        btnImageSearch = (Button) findViewById(R.id.btn_image_search);
        btnRecyclingGuide = (Button) findViewById(R.id.btn_recycling_guide);
        btnNotice = (Button) findViewById(R.id.btn_notice);

        btnImageSearch.setOnClickListener(this);
        btnRecyclingGuide.setOnClickListener(this);
        btnNotice.setOnClickListener(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    public void onClick(View view) {
        if (view == btnImageSearch) {
            Log.v("Image", "Image");
            Intent imageSearchIntent = new Intent(MainActivity.this, ImageSearchActivity.class);
            startActivity(imageSearchIntent);
        } else if (view == btnRecyclingGuide) {
            Log.v("Recycling Guide", "Recycling Guide");
            Intent recyclingGuideIntent = new Intent(MainActivity.this, RecyclingGuideActivity.class);
            startActivity(recyclingGuideIntent);
        } else if (view == btnNotice) {
            Log.v("Notice", "Notice");
            Intent noticeIntent = new Intent(MainActivity.this, NoticeActivity.class);
            startActivity(noticeIntent);
        }
    }
}