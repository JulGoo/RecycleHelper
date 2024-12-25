package kr.akotis.recyclehelper.community;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_community);

        /*
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
         */

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


        // 권한 요청
        checkAndRequestPermissions();

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
            adapter.startListening();
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



    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // 권한이 없는 경우 요청
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            } else {
                // 권한이 이미 허용됨
                //Toast.makeText(this, "이미지 저장 권한이 이미 허용되었습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "이미지 저장 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show();
                // 권한이 허용된 경우 수행할 작업
            } else {
                Toast.makeText(this, "이미지 저장 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }



}