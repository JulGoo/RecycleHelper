package kr.akotis.recyclehelper.notice;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import kr.akotis.recyclehelper.R;

public class NoticeAdapter extends RecyclerView.Adapter<NoticeAdapter.NoticeViewHolder> {

    private final List<Notice> noticeList;
    private final OnNoticeClickListener onNoticeClickListener;

    public NoticeAdapter(List<Notice> noticeList, OnNoticeClickListener listener) {
        this.noticeList = noticeList;
        this.onNoticeClickListener = listener;
    }

    @NonNull
    @Override
    public NoticeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(kr.akotis.recyclehelper.R.layout.item_notice, parent, false);
        return new NoticeViewHolder(view, onNoticeClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull NoticeViewHolder holder, int position) {
        Notice notice = noticeList.get(position);
        holder.title.setText(notice.getTitle());
        holder.date.setText(notice.getDate());
    }

    @Override
    public int getItemCount() {
        return noticeList.size();
    }

    static class NoticeViewHolder extends RecyclerView.ViewHolder {
        TextView title, date;

        public NoticeViewHolder(@NonNull View itemView, OnNoticeClickListener listener) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_notice_title);
            date = itemView.findViewById(R.id.tv_notice_date);


            // 클릭 이벤트 설정
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onNoticeClick(position);
                    }
                }
            });
        }
    }

    // 클릭 리스너 인터페이스
    public interface OnNoticeClickListener {
        void onNoticeClick(int position);
    }
}
