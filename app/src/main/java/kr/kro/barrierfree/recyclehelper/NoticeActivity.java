package kr.kro.barrierfree.recyclehelper;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;


public class NoticeActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NoticeAdapter noticeAdapter;
    private FirebaseFirestore db;
    private List<Notice> noticeList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notice);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //RecyclerView 초기화
        recyclerView = findViewById(R.id.rv_notices);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        //Firestore 초기화
        db = FirebaseFirestore.getInstance();

        //RecyclerView에 어댑터 설정
        noticeAdapter = new NoticeAdapter(noticeList);
        recyclerView.setAdapter(noticeAdapter);

        //Firestore에서 데이터 가져오기
        loadNoticeFromFirestore();
    }

    private void loadNoticeFromFirestore() {
        db.collection("notices")
                .get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null){
                            noticeList.clear();
                            for (QueryDocumentSnapshot document : querySnapshot) {
                                Notice notice = document.toObject(Notice.class);
                                noticeList.add(notice);
                            }
                            //어댑터에 데이터 변경 알림
                            noticeAdapter.notifyDataSetChanged();
                        }
                    } else {
                        //실패 시
                        Log.e("NoticeActivity", "Error getting documents: ", task.getException());
                    }
                });
    }
}