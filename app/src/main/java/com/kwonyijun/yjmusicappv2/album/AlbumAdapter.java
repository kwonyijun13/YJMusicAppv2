package com.kwonyijun.yjmusicappv2.album;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kwonyijun.yjmusicappv2.R;

import java.util.ArrayList;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.ViewHolder> {
    private Context context;
    private ArrayList<Album> albumList;
    private static ItemClickListener itemClickListener;
    public AlbumAdapter(Context context, ArrayList<Album> albumList) {
        this.context = context;
        this.albumList = albumList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_album, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Album album = albumList.get(position);
        // SET ALBUM IMAGE
        if (album.getAlbumImage() != null) {
            holder.albumImageButton.setImageBitmap(album.getAlbumImage());
        } else {
            // IF NO ALBUM IMAGE, SET A DEFAULT ONE
            holder.albumImageButton.setImageResource(R.drawable.placeholder_album);
        }
        holder.albumTextView.setText(album.getAlbumTitle());
    }

    @Override
    public int getItemCount() {
        return albumList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageButton albumImageButton;
        TextView albumTextView;
        private int position;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            albumImageButton = itemView.findViewById(R.id.album_imageButton);
            albumTextView = itemView.findViewById(R.id.album_title_textView);

            albumImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    position = getAdapterPosition();
                    if (itemClickListener != null) {
                        itemClickListener.onItemClick(position);
                    }
                }
            });
        }
    }

    public void setItemClickListener(ItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    // ITEM CLICK LISTENER INTERFACE
    public interface ItemClickListener {
        void onItemClick(int position);
    }
}
