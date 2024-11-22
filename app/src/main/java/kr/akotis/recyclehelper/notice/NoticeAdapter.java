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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import kr.akotis.recyclehelper.R;
import kr.akotis.recyclehelper.community.Community;

public class NoticeAdapter extends FirebaseRecyclerAdapter<Notice, NoticeAdapter.NoticeViewHolder> {

    public NoticeAdapter(@NonNull FirebaseRecyclerOptions<Notice> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull NoticeViewHolder holder, int position, @NonNull Notice model) {
        // 최신순 정렬: position을 역순으로 처리
        int reversePosition = getItemCount() - 1 - position;
        Notice reverseModel = getSnapshots().get(reversePosition);


        holder.tvNoticeTitle.setText(reverseModel.getTitle());
        holder.tvNoticeDate.setText(reverseModel.getDate());

        // 타임스탬프를 읽어 날짜로 변환
        //long timestamp = Long.parseLong(reverseModel.getDate());
        //SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        //String formattedDate = sdf.format(new Date(timestamp));
        holder.tvNoticeDate.setText(reverseModel.getDate());


        // 클릭 이벤트 처리
        holder.itemView.setOnClickListener(v -> {
            // NoticeDetailActivity로 이동
            Intent intent = new Intent(holder.itemView.getContext(), NoticeDetailActivity.class);
            intent.putExtra("notice", reverseModel);  // Parcelable 객체를 인텐트에 전달
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
