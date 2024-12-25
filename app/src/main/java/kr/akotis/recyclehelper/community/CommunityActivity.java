package kr.akotis.recyclehelper.community;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;

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
import com.google.firebase.database.Query;

import java.util.HashSet;
import java.util.Set;

import kr.akotis.recyclehelper.R;
import kr.akotis.recyclehelper.notice.Notice;
import kr.akotis.recyclehelper.notice.NoticeAdapter;

public class CommunityActivity extends AppCompatActivity {

    private RecyclerView rv;
    private DatabaseReference communityRef;
    private FirebaseRecyclerAdapter<Community, CommunityAdapter.CommunityViewHolder> adapter;
    private FloatingActionButton fab;
    private EditText etSearch;
    private ImageButton btnSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_community);

        etSearch = findViewById(R.id.et_search);
        btnSearch = findViewById(R.id.btn_search);

        btnSearch.setOnClickListener(v -> {
            String searchText = etSearch.getText().toString().trim();
            if (!searchText.isEmpty()) {
                searchCommunity(searchText);
            }else {
                // 검색어가 비어있을 시, 전체 데이터 표시
                searchCommunity("");
            }
        });

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

    private void searchCommunity(String searchText) {
        // 검색어를 소문자로 변환하고, trim하여 공백 제거
        String searchQuery = searchText.toLowerCase().trim();

        // 2-gram으로 변환한 검색어를 사용
        String twoGramSearchQuery = generateTwoGram(searchQuery);

        Query firebaseQuery = communityRef.orderByChild("searchField").startAt(twoGramSearchQuery).endAt(twoGramSearchQuery + "\uf8ff");

        FirebaseRecyclerOptions<Community> options = new FirebaseRecyclerOptions.Builder<Community>()
                .setQuery(firebaseQuery, Community.class)
                .build();

        Log.d("SearchQuery", "Search Query: " + twoGramSearchQuery);

        // 기존 adapter가 있으면 stopListening
        if (adapter != null) {
            adapter.stopListening(); // 기존 리스닝 중지
        }

        // 새로운 options로 adapter 갱신
        adapter = new CommunityAdapter(options);
        rv.setAdapter(adapter);

        // 새로 리스닝 시작
        adapter.startListening();
    }

    // 2-gram 생성 메소드
    private String generateTwoGram(String text) {
        StringBuilder twoGramBuilder = new StringBuilder();

        // 입력 텍스트를 소문자로 변환하고 공백을 제거
        text = text.toLowerCase().trim().replaceAll("\\s+", " ");  // 중복 공백 처리

        Set<String> nGrams = new HashSet<>();

        // 2글자씩 잘라서 2-gram 생성
        for (int i = 0; i < text.length() - 1; i++) {
            String nGram = text.substring(i, i + 2);  // 2글자씩 잘라냄
            nGrams.add(nGram);  // Set에 추가하여 중복 제거
        }

        // Set에 저장된 2-gram을 공백으로 구분하여 반환
        for (String nGram : nGrams) {
            twoGramBuilder.append(nGram).append(" ");
        }

        return twoGramBuilder.toString().trim();  // 마지막 공백 제거
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