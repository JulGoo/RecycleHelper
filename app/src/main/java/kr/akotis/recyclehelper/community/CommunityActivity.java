package kr.akotis.recyclehelper.community;

import android.content.Intent;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import kr.akotis.recyclehelper.R;
import kr.akotis.recyclehelper.notice.Notice;
import kr.akotis.recyclehelper.notice.NoticeAdapter;

public class CommunityActivity extends AppCompatActivity {

    private RecyclerView rv;
    private DatabaseReference communityRef;
    private FirebaseRecyclerAdapter<Community, CommunityAdapter.CommunityViewHolder> adapter;
    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_community);

        rv = findViewById(R.id.recycler_community);
        rv.setLayoutManager(new LinearLayoutManager(this));

        communityRef = FirebaseDatabase.getInstance().getReference().child("Community");
        FirebaseRecyclerOptions<Community> options = new FirebaseRecyclerOptions.Builder<Community>()
                .setQuery(communityRef.orderByChild("date"), Community.class)
                .build();

        adapter = new CommunityAdapter(options);
        rv.setAdapter(adapter);

        fab = findViewById(R.id.fab_add);
        fab.setOnClickListener(v -> {
            // 커뮤니티 작성 화면으로 이동
            startActivity(new Intent(CommunityActivity.this, CommunityWriteActivity.class));
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.recycler_view), (v, insets) -> {
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