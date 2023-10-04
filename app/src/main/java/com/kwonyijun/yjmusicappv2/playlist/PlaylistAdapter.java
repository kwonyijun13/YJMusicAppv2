package com.kwonyijun.yjmusicappv2.playlist;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kwonyijun.yjmusicappv2.R;

import java.util.ArrayList;
import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder> {
    private Context context;
    private List<Playlist> playlistArrayList;
    private PlaylistAdapter.ItemClickListener itemClickListener;
    public PlaylistAdapter(Context context, List<Playlist> playlists) {
        this.context = context;
        this.playlistArrayList = playlists;
    }

    @NonNull
    @Override
    public PlaylistAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_playlist, parent, false);
        return new PlaylistAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistAdapter.ViewHolder holder, int position) {
        Playlist playlist = playlistArrayList.get(position);

        // SET PLAYLIST NAME & SONGS COUNT
        // CONVERT BYTE[] TO BITMAP
        Bitmap playlistCover = BitmapFactory.decodeByteArray(playlist.getPlaylistImage(), 0, playlist.getPlaylistImage().length);
        holder.playlistCoverImageView.setImageBitmap(playlistCover);
        holder.titleTextView.setText(playlist.getPlaylistName());
    }

    @Override
    public int getItemCount() {
        return playlistArrayList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, songsTextView;
        ImageView playlistCoverImageView, menuImageView;
        private int position;
        private String titleText, songsText;
        private long id;
        public ViewHolder(View itemView) {
            super(itemView);
            playlistCoverImageView = itemView.findViewById(R.id.playlist_imageView);
            titleTextView = itemView.findViewById(R.id.playlist_textView);
            menuImageView = itemView.findViewById(R.id.menu_imageview);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (itemClickListener != null) {
                        position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION && itemClickListener != null) {
                            Playlist playlist = playlistArrayList.get(position);
                            titleText = playlist.getPlaylistName();
                            itemClickListener.onItemClick(position, titleText, playlistCoverImageView);
                        }
                    }
                }
            });

            menuImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (itemClickListener != null) {
                        position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            Playlist playlist = playlistArrayList.get(position);
                            id = playlist.getId();
                            itemClickListener.onMenuIconClick(position, id);
                        }
                    }
                }
            });
        }
    }

    public void setItemClickListener(PlaylistAdapter.ItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    // ITEM CLICK LISTENER INTERFACE
    public interface ItemClickListener {
        void onItemClick(int position, String title, ImageView imageView);
        void onMenuIconClick(int position, Long id);
    }
}