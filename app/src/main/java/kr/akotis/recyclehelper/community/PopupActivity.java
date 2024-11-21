package kr.akotis.recyclehelper.community;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import kr.akotis.recyclehelper.R;

public class PopupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // CommunityDetailActivity에서 전달된 menu_id 가져오기
        int menuId = getIntent().getIntExtra("menu_id", -1);

        // menu_id에 따라 동작 처리
        if (menuId != -1) {
            if (menuId == R.id.delete_menu) {
                // 삭제 처리 로직 추가
                showDeleteDialog();
            } else if (menuId == R.id.report_menu) {
                // 신고 처리 로직 추가
                showReportDialog();
            }
        }
    }

    // 삭제 다이얼로그 띄우기
    private void showDeleteDialog() {
        // 레이아웃 인플레이터 사용
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.community_popup_delete, null);

        // EditText 및 버튼 초기화
        final EditText etPassword = dialogView.findViewById(R.id.et_password);
        Button btnYes = dialogView.findViewById(R.id.btn_yes);
        Button btnNo = dialogView.findViewById(R.id.btn_no);

        // AlertDialog.Builder를 사용하여 다이얼로그 생성
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView)
                .setCancelable(false); // 다이얼로그 외부 클릭 시 닫히지 않도록 설정

        // 다이얼로그 생성
        AlertDialog dialog = builder.create();
        Log.d("PopupActivity", "showDeleteDialog called");

        // "예" 버튼 클릭 시 비밀번호 확인 후 삭제
        btnYes.setOnClickListener(v -> {
            String password = etPassword.getText().toString();
            if (password.equals("your_password")) { // 실제 비밀번호 확인 로직 추가
                Toast.makeText(PopupActivity.this, "삭제가 완료되었습니다.", Toast.LENGTH_SHORT).show();
                // 삭제 작업을 여기에 추가
                // 이 부분에서 삭제한 항목에 대한 데이터 처리 후, 리사이클러뷰 상태를 갱신하는 로직이 필요합니다.
                finish();  // 다이얼로그 종료 후 액티비티 종료
            } else {
                Toast.makeText(PopupActivity.this, "비밀번호가 틀렸습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        // "아니오" 버튼 클릭 시 다이얼로그 닫기
        btnNo.setOnClickListener(v -> {
            dialog.dismiss();  // 다이얼로그 닫기
            Log.d("PopupActivity", "btnNo clicked");

            finish();  // 현재 액티비티 종료
        });

        dialog.show();  // 다이얼로그 띄우기
    }

    // 신고 다이얼로그 띄우기
    private void showReportDialog() {
        // 레이아웃 인플레이터 사용
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.community_popup_report, null);

        // 버튼 초기화
        Button btnYes = dialogView.findViewById(R.id.btn_yes);
        Button btnNo = dialogView.findViewById(R.id.btn_no);

        // AlertDialog.Builder를 사용하여 다이얼로그 생성
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView)
                .setCancelable(false); // 다이얼로그 외부 클릭 시 닫히지 않도록 설정

        // 다이얼로그 생성
        AlertDialog dialog = builder.create();
        Log.d("PopupActivity", "showReportDialog called");

        // "예" 버튼 클릭 시 신고 작업 수행
        btnYes.setOnClickListener(v -> {
            // 신고 작업 수행 예시
            Toast.makeText(PopupActivity.this, "신고가 완료되었습니다.", Toast.LENGTH_SHORT).show();
            dialog.dismiss();  // 다이얼로그 닫기
        });

        // "아니오" 버튼 클릭 시 다이얼로그 닫기
        btnNo.setOnClickListener(v -> {
            dialog.dismiss();  // 다이얼로그 닫기
        });

        dialog.show();  // 다이얼로그 띄우기
    }
}
