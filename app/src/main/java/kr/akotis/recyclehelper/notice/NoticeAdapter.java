package kr.akotis.recyclehelper.notice;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;

import java.util.List;

import kr.akotis.recyclehelper.R;

public class NoticeAdapter extends FirebaseRecyclerAdapter<Notice, NoticeAdapter.NoticeViewHolder> {

    public NoticeAdapter(@NonNull FirebaseRecyclerOptions<Notice> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull NoticeViewHolder holder, int position, @NonNull Notice model) {
        holder.tvNoticeTitle.setText(model.getTitle());
        holder.tvNoticeDate.setText(model.getDate());

        // 클릭 이벤트 처리
        holder.itemView.setOnClickListener(v -> {
            // NoticeDetailActivity로 이동
            Intent intent = new Intent(holder.itemView.getContext(), NoticeDetailActivity.class);
            intent.putExtra("notice", model);  // Parcelable 객체를 인텐트에 전달
            holder.itemView.getContext().startActivity(intent);
        });
    }


    @NonNull
    @Override
    public NoticeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notice, parent, false);
        return new NoticeViewHolder(view);
    }

    public static class NoticeViewHolder extends RecyclerView.ViewHolder {
        TextView tvNoticeTitle;
        TextView tvNoticeDate;

        public NoticeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNoticeTitle = itemView.findViewById(R.id.tv_notice_title);
            tvNoticeDate = itemView.findViewById(R.id.tv_notice_date);
        }
    }
}
