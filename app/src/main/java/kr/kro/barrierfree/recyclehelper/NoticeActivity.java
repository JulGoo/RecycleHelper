package kr.kro.barrierfree.recyclehelper;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class NoticeActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NoticeAdapter noticeAdapter;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private List<Notice> noticeList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notice);

        //RecyclerView 초기화
        recyclerView = findViewById(R.id.rv_notices);
        noticeList = new ArrayList<>();

        //RecyclerView에 어댑터 설정
        noticeAdapter = new NoticeAdapter(noticeList);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(noticeAdapter);

        //Firestore에서 데이터 가져오기
        loadNoticeFromFirestore();
    }

    private void loadNoticeFromFirestore() {
        db.collection("notices")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            noticeList.clear();  // 이전 데이터를 지우고 새로 추가
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String title = document.getString("title");
                                String description = document.getString("description");
                                Date date = document.getDate("date");

                                Log.d("Firestore", "Title: " + title);

                                // Firestore에서 가져온 데이터를 Notice 객체로 변환
                                Notice notice = new Notice(title, description, date);
                                noticeList.add(notice);
                            }

                            // 데이터가 로드된 후 noticeList의 크기를 출력합니다.
                            Log.d("Firestore", "Size of noticeList: " + noticeList.size());

                            noticeAdapter.notifyDataSetChanged(); // 어댑터에 데이터가 변경되었음을 알림
                        } else {
                            Log.d("Firestore", "Error getting documents: ", task.getException());
                        }
                    }
                });
    }
}