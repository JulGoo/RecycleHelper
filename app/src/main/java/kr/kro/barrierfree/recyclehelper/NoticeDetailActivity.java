package kr.kro.barrierfree.recyclehelper;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class NoticeDetailActivity extends AppCompatActivity {

    private TextView tvTitle;
    private TextView tvDescription;
    private TextView tvDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notice_detail);

        tvTitle = findViewById(R.id.tv_title);
        tvDescription = findViewById(R.id.tv_description);
        tvDate = findViewById(R.id.tv_date);

        // NullPointerException 방지를 위해 확인
        if (tvTitle == null) {
            Log.e("NoticeDetailActivity", "tvTitle is null");
        }
        if (tvDescription == null) {
            Log.e("NoticeDetailActivity", "tvDescription is null");
        }
        if (tvDate == null) {
            Log.e("NoticeDetailActivity", "tvDate is null");
        }

        // Intent로부터 데이터 받아오기
        Intent intent = getIntent();
        String title = intent.getStringExtra("title");
        String description = intent.getStringExtra("description");
        String date = intent.getStringExtra("date");

        tvTitle.setText(title);
        tvDescription.setText(Html.fromHtml(description));
        tvDate.setText(date);
    }
}