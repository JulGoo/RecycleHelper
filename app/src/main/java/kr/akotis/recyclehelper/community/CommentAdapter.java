package kr.akotis.recyclehelper.community;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import kr.akotis.recyclehelper.R;

public class CommentAdapter extends FirebaseRecyclerAdapter<Comment, CommentAdapter.CommentViewHolder> {
    public CommentAdapter(@NonNull FirebaseRecyclerOptions<Comment> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull CommentViewHolder holder, int position, @NonNull Comment model) {
        holder.tvContent.setText(model.getContent());

        // 타임스탬프를 읽어 날짜로 변환
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String formattedDate = sdf.format(new Date(model.getDate()));
        holder.tvDate.setText(formattedDate);

        holder.btnMenu.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
            MenuInflater inflater = popupMenu.getMenuInflater();
            inflater.inflate(R.menu.popup, popupMenu.getMenu());

            // 메뉴 아이템 클릭 이벤트 처리
            popupMenu.setOnMenuItemClickListener(item -> {
                Log.d("menu", "menu_title: " + item.getTitle());
                if(item.getTitle().equals("삭제")) {
                    ((CommunityDetailActivity) v.getContext()).showDeleteDialog(model);
                    return true;
                } else if (item.getTitle().equals("신고")) {
                    ((CommunityDetailActivity) v.getContext()).showReportDialog(model);
                    return true;
                } else {
                    return false;
                }
            });

            // PopupMenu 표시
            popupMenu.show();
        });
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent, tvDate;
        ImageButton btnMenu;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tv_comment_content);
            tvDate = itemView.findViewById(R.id.tv_comment_date);
            btnMenu = itemView.findViewById(R.id.btn_comment_menu);
        }
    }
}
