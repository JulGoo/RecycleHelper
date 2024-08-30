package kr.kro.barrierfree.recyclehelper;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NoticeAdapter extends RecyclerView.Adapter<NoticeAdapter.NoticeViewHolder> {

    private List<Notice> noticeList;

    public NoticeAdapter(List<Notice> noticeList)  {
        this.noticeList = noticeList;
    }

    @NonNull
    @Override
    public NoticeAdapter.NoticeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notice, parent, false);
        return new NoticeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoticeAdapter.NoticeViewHolder holder, int position) {
        Notice notice = noticeList.get(position);
        holder.tvTitle.setText(notice.getTitle());
    }

    @Override
    public int getItemCount() {
        return noticeList.size();
    }

    public static class NoticeViewHolder extends RecyclerView.ViewHolder{
        TextView tvTitle;

        public NoticeViewHolder(@NonNull View itemView){
            super(itemView);
            tvTitle=itemView.findViewById(R.id.tv_title);
        }
    }
}
