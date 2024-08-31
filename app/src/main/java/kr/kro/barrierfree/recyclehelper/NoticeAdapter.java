package kr.kro.barrierfree.recyclehelper;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NoticeAdapter extends RecyclerView.Adapter<NoticeAdapter.NoticeViewHolder> {

    private List<Notice> noticeList;

    public NoticeAdapter(List<Notice> noticeList) {
        this.noticeList = noticeList;
    }

    @NonNull
    @Override
    public NoticeAdapter.NoticeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notice, parent, false);
        return new NoticeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(NoticeViewHolder holder, int position) {
        Notice notice = noticeList.get(position);
        Log.d("Adapter", "Binding position: " + position + ", Title: " + notice.getTitle() + ", Description: " + notice.getDescription() + ", Date: " + notice.getDate());

        holder.textViewTitle.setText(notice.getTitle());

        //날짜 데이터 가공
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String formattedDate = dateFormat.format(notice.getDate());

        // 아이템 클릭 리스너 설정
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), NoticeDetailActivity.class);
            intent.putExtra("title", notice.getTitle());
            intent.putExtra("description", notice.getDescription());
            intent.putExtra("date", formattedDate);  // 날짜 String으로 전달
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return noticeList != null ? noticeList.size() : 0;
    }

    public static class NoticeViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTitle;

        public NoticeViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.textview_title);
        }
    }
}
