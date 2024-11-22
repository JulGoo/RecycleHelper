package kr.akotis.recyclehelper.community;

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

public class CommunityAdapter extends FirebaseRecyclerAdapter<Community, CommunityAdapter.CommunityViewHolder> {
    public CommunityAdapter(@NonNull FirebaseRecyclerOptions<Community> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull CommunityViewHolder holder, int position,
                                    @NonNull Community model) {
        // 최신순 정렬: position을 역순으로 처리
        int reversePosition = getItemCount() - 1 - position;
        Community reverseModel = getSnapshots().get(reversePosition);


        holder.tvCommunityTitle.setText(reverseModel.getTitle());

        // 타임스탬프를 읽어 날짜로 변환
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String formattedDate = sdf.format(new Date(reverseModel.getDate()));
        holder.tvCommunityDate.setText(formattedDate);

        // 클릭 이벤트 처리
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), CommunityDetailActivity.class);
            intent.putExtra("community", reverseModel);  // Parcelable 객체를 인텐트에 전달
            holder.itemView.getContext().startActivity(intent);
        });
    }

    @NonNull
    @Override
    public CommunityAdapter.CommunityViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                                   int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_community, parent, false);
        return new CommunityAdapter.CommunityViewHolder(view);
    }

    public static class CommunityViewHolder extends RecyclerView.ViewHolder {
        TextView tvCommunityTitle;
        TextView tvCommunityDate;

        public CommunityViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCommunityTitle = itemView.findViewById(R.id.tv_community_title);
            tvCommunityDate = itemView.findViewById(R.id.tv_community_date);
        }
    }
}