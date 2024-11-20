package kr.akotis.recyclehelper.notice;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

import kr.akotis.recyclehelper.R;

public class NoticeImgAdapter extends RecyclerView.Adapter<NoticeImgAdapter.ImageViewHolder> {

    private final List<String> imgUrls;

    public NoticeImgAdapter(List<String> imgUrls) {
        this.imgUrls = imgUrls;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_img_notice, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String imgUrl = imgUrls.get(position);
        Log.d("Image URL", imgUrl); // URL을 로그로 출력

        // Firebase Storage에서 HTTP URL로 변환하여 Glide로 이미지 로드
        if (imgUrl.startsWith("gs://")) {
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReferenceFromUrl(imgUrl);

            // Firebase Storage에서 URL을 다운로드 받아서 Glide로 로드
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                Glide.with(holder.itemView.getContext())
                        .load(uri)
                        .placeholder(R.drawable.placeholder) // 로딩 중일 때 보여줄 이미지
                        .error(R.drawable.error) // 에러 시 보여줄 이미지
                        .into(holder.ivImage);
            }).addOnFailureListener(e -> {
                Log.e("Glide Error", "Failed to load image: " + e.getMessage());
            });
        } else {
            // HTTP(S) URL인 경우 바로 Glide로 로드
            Glide.with(holder.itemView.getContext())
                    .load(imgUrl)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.error)
                    .into(holder.ivImage);
        }
    }

    @Override
    public int getItemCount() {
        return imgUrls.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_image);
        }
    }
}
