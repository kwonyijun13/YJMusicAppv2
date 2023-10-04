package com.kwonyijun.yjmusicappv2.song;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kwonyijun.yjmusicappv2.R;

import java.util.ArrayList;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder> {
    private Context context;
    private ArrayList<Song> songArrayList;
    private ItemClickListener itemClickListener;
    public SongAdapter(Context context, ArrayList<Song> songFiles) {
        this.context = context;
        this.songArrayList = songFiles;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_song, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongAdapter.ViewHolder holder, int position) {
        Song songFile = songArrayList.get(position);

        // SET ALBUM IMAGE
        if (songFile.getAlbumImage() != null) {
            holder.albumImageView.setImageBitmap(songFile.getAlbumImage());
        } else {
            // IF NO ALBUM IMAGE, SET A DEFAULT ONE
            holder.albumImageView.setImageResource(R.drawable.placeholder_album);
        }

        // SET TITLE & ARTIST
        holder.titleTextView.setText(songFile.getTitle());
        holder.artistTextView.setText(songFile.getArtist());
    }

    @Override
    public int getItemCount() {
        return songArrayList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        // THIS CLASS EXTENDS SONGSFRAGMENT'S RECYCLERVIEW VIEWHOLDER (ITEM_SONG)
        TextView titleTextView, artistTextView;
        ImageView albumImageView, sortImageView;

        private int position;
        private String filename;

        public ViewHolder(View itemView) {
            super(itemView);
            albumImageView = itemView.findViewById(R.id.album_image);
            titleTextView = itemView.findViewById(R.id.song_title_textView);
            artistTextView = itemView.findViewById(R.id.artist_name_textView);
            sortImageView = itemView.findViewById(R.id.sort_imageview);

            // ON SONG PRESS
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (itemClickListener != null) {
                        // GET POSITION OF SELECTED MUSIC
                        position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION && itemClickListener != null) {
                            // Pass the position and filename to the item click listener
                            Song song = songArrayList.get(position);
                            filename = song.getFilePath();
                            int duration = song.getDuration();
                            itemClickListener.onItemClick(position, filename, albumImageView, titleTextView, artistTextView, duration);
                        }
                    }
                }
            });

            // ON SORT ICON PRESS
            sortImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (itemClickListener != null) {
                        position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            itemClickListener.onOptionsIconClick(position);
                        }
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
        void onItemClick(int position, String filename, ImageView imageView, TextView title, TextView artist, int duration);
        void onOptionsIconClick(int position);

    }
}
