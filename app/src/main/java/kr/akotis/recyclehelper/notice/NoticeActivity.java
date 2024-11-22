package kr.akotis.recyclehelper.notice;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import kr.akotis.recyclehelper.R;
import kr.akotis.recyclehelper.community.Community;

public class NoticeActivity extends AppCompatActivity {

    private RecyclerView rv;
    private FirebaseRecyclerAdapter<Notice, NoticeAdapter.NoticeViewHolder> adapter;
    private DatabaseReference noticeRef;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notice);

        rv = findViewById(R.id.recycler_notice);
        rv.setLayoutManager(new LinearLayoutManager(this));

        // Firebase 데이터 초기화
        noticeRef = FirebaseDatabase.getInstance().getReference().child("Notice");
        FirebaseRecyclerOptions<Notice> options = new FirebaseRecyclerOptions.Builder<Notice>()
                .setQuery(noticeRef.orderByChild("date"), Notice.class)
                .build();

        adapter = new NoticeAdapter(options);
        rv.setAdapter(adapter);

        // 시스템 바 패딩 처리
        ViewCompat.setOnApplyWindowInsetsListener(rv, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.notifyDataSetChanged(); // 데이터를 강제로 다시 표시
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
