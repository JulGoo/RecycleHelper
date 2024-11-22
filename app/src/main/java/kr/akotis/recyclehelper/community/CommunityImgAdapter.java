package kr.akotis.recyclehelper.community;

import android.content.Intent;
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
import kr.akotis.recyclehelper.FullScreenImgActivity;

public class CommunityImgAdapter extends RecyclerView.Adapter<CommunityImgAdapter.ImageViewHolder> {

    private final List<String> imgUrls;

    public CommunityImgAdapter(List<String> imgUrls) {
        this.imgUrls = imgUrls;
    }

    @NonNull
    @Override
    public CommunityImgAdapter.ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d("ViewHolder", "onCreateViewHolder called");
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_img_community, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommunityImgAdapter.ImageViewHolder holder, int position) {
        String imgUrl = imgUrls.get(position);
        Log.d("Image URL", imgUrl); // URL을 로그로 출력

        if (imgUrl.startsWith("gs://")) {
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReferenceFromUrl(imgUrl);

            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                Glide.with(holder.itemView.getContext())
                        .load(uri)
                        .placeholder(R.drawable.placeholder) // 로딩 중일 때 보여줄 이미지
                        .error(R.drawable.error) // 에러 시 보여줄 이미지
                        .into(holder.ivImage);

                holder.ivImage.setOnClickListener(v -> {
                    Intent intent = new Intent(holder.itemView.getContext(), FullScreenImgActivity.class);
                    intent.putExtra("imageUrl", uri.toString());
                    holder.itemView.getContext().startActivity(intent);
                });

            }).addOnFailureListener(e -> {
                Log.e("Glide Error", "Failed to load image: " + e.getMessage());
            });
        } else {
            Log.d("Image Binding", "**************Binding URL at position " + position + ": " + imgUrl);

            Glide.with(holder.itemView.getContext())
                    .load(imgUrl)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.error)
                    .into(holder.ivImage);

            holder.ivImage.setOnClickListener(v -> {
                Intent intent = new Intent(holder.itemView.getContext(), FullScreenImgActivity.class);
                intent.putExtra("imageUrl", imgUrl);
                holder.itemView.getContext().startActivity(intent);
            });
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
