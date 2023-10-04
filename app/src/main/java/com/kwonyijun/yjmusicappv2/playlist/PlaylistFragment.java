package com.kwonyijun.yjmusicappv2.playlist;

import static android.app.Activity.RESULT_OK;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.kwonyijun.yjmusicappv2.DatabaseHelper;
import com.kwonyijun.yjmusicappv2.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Random;

public class PlaylistFragment extends Fragment implements PlaylistAdapter.ItemClickListener{
    private RecyclerView recyclerView;
    private PlaylistAdapter playlistAdapter;
    private List<Playlist> playlists;
    private DatabaseHelper databaseHelper;
    private BottomSheetDialog bottomSheetDialog;
    private byte[] selectedImageByte;
    private Playlist playlist;
    public PlaylistFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_playlist, container, false);

        // RETRIEVE PLAYLISTS FROM DATABASE
        databaseHelper = new DatabaseHelper(getActivity());
        playlists = databaseHelper.getAllPlaylists();

        // SET UP PLAYLISTADAPTER
        playlistAdapter = new PlaylistAdapter(getContext(), playlists);
        playlistAdapter.setItemClickListener(this);

        // SET UP RECYCLERVIEW
        recyclerView = rootView.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(playlistAdapter);

        // ADD NEW PLAYLIST
        Button addPlaylistButton = rootView.findViewById(R.id.add_button);
        addPlaylistButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddPlaylistDialog();
            }
        });

        return rootView;
    }
    @SuppressLint("NotifyDataSetChanged")
    private void showAddPlaylistDialog() {
        // BITMAP ARRAY OF CATS
        Bitmap[] bitmapArray = new Bitmap[4];
        bitmapArray[0] = BitmapFactory.decodeResource(getResources(), R.drawable.cat_one);
        bitmapArray[1] = BitmapFactory.decodeResource(getResources(), R.drawable.cat_two);
        bitmapArray[2] = BitmapFactory.decodeResource(getResources(), R.drawable.cat_three);
        bitmapArray[3] = BitmapFactory.decodeResource(getResources(), R.drawable.cat_four);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_playlist, null);

        EditText playlistNameEditText = dialogView.findViewById(R.id.title_editText);

        builder.setView(dialogView)
                .setTitle("Add Playlist")
                .setPositiveButton("Create", (dialog, which) -> {
                    String playlistName = playlistNameEditText.getText().toString();
                    if (!TextUtils.isEmpty(playlistName)) {
                        Playlist newPlaylist = new Playlist();
                        newPlaylist.setPlaylistName(playlistName);

                        // SET RANDOM PLAYLIST COVER
                        Random random = new Random();
                        int randomIndex = random.nextInt(bitmapArray.length);
                        Bitmap selectedImage = bitmapArray[randomIndex];

                        // CONVERT BITMAP TO BYTE[] TO SAVE IN DATABASE
                        byte[] imageByteArray = convertImageToByteArray(selectedImage);

                        // ADD PLAYLIST COVER
                        newPlaylist.setPlaylistImage(imageByteArray);

                        // ADD NEW PLAYLIST TO DATABASE
                        databaseHelper.addPlaylist(newPlaylist, imageByteArray);
                        // ADD NEW PLAYLIST TO LIST
                        playlists.add(newPlaylist);
                        playlistAdapter.notifyDataSetChanged();

                        // RETRIEVE THE PLAYLISTS AGAIN
                        playlists = databaseHelper.getAllPlaylists();
                    }
                })
                .setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private byte[] convertImageToByteArray(Bitmap image) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    @Override
    public void onItemClick(int position, String title, ImageView imageView) {
        long playlistId = playlists.get(position).getId();
        Intent intent = new Intent(getActivity(), PlaylistFolderActivity.class);
        intent.putExtra("id", playlistId);
        startActivity(intent);
    }
    @Override
    public void onMenuIconClick(int position, Long id) {
        playlist = playlists.get(position);

        // CREATE BOTTOMSHEETDIALOG WITH CUSTOM LAYOUT
        bottomSheetDialog = new BottomSheetDialog(getContext());
        bottomSheetDialog.setContentView(R.layout.layout_bottom_playlist_popup);

        // EDIT PLAYLIST COVER
        TextView editCoverTextView = bottomSheetDialog.findViewById(R.id.playlist_cover_textView);
        editCoverTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Dialog dialog = new Dialog(getContext());
                dialog.setContentView(R.layout.dialog_image_selection);

                Button galleryButton = dialog.findViewById(R.id.gallery_button);
                Button webButton = dialog.findViewById(R.id.browse_button);

                galleryButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(intent, 3);
                        dialog.dismiss();
                    }
                });

                webButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/imghp?hl=en&ogbl"));
                        startActivity(browserIntent);
                        dialog.dismiss();
                    }
                });

                dialog.show();
            }
        });

        // EDIT PLAYLIST TITLE
        TextView editTitleTextView = bottomSheetDialog.findViewById(R.id.playlist_title_textView);
        editTitleTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
                LayoutInflater inflater = requireActivity().getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.dialog_add_playlist, null);

                EditText playlistNameEditText = dialogView.findViewById(R.id.title_editText);

                builder.setView(dialogView)
                        .setTitle("Edit Title")
                        .setPositiveButton("Edit", (dialog, which) -> {
                            String playlistName = playlistNameEditText.getText().toString();
                            if (!TextUtils.isEmpty(playlistName)) {
                                // UPDATE CHANGES IN DATABASE
                                databaseHelper.updatePlaylistName(id, playlistName);
                                // UPDATE CHANGES IN LIST
                                playlists.get(position).setPlaylistName(playlistName);
                                playlistAdapter.notifyDataSetChanged();
                            }
                        })
                        .setNegativeButton("Cancel", null);
                builder.create().show();
                bottomSheetDialog.dismiss();
            }
        });

        // DELETE PLAYLIST
        TextView deleteTextView = bottomSheetDialog.findViewById(R.id.deleteTextView);
        deleteTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
                builder.setTitle("Confirm Delete Playlist")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            // DELETE FROM DATABASE
                            databaseHelper.deletePlaylist(id);
                            // DELETE FROM LIST
                            playlists.remove(position);
                            playlistAdapter.notifyItemRemoved(position);
                        })
                        .setNegativeButton("Cancel", null);
                builder.create().show();
                bottomSheetDialog.dismiss();
            }
        });

        bottomSheetDialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 3 && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            // CONVERT TYPE URI TO BYTE[]
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try {
                InputStream inputStream = getContext().getContentResolver().openInputStream(selectedImageUri);
                if (inputStream != null) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        byteArrayOutputStream.write(buffer, 0, bytesRead);
                    }
                    inputStream.close();
                    selectedImageByte = byteArrayOutputStream.toByteArray();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // UPDATE IN DATABASE
            databaseHelper.updatePlaylistCover(playlist.getId(), selectedImageByte);

            // SET THE NEW IMAGE IN LIST
            playlist.setPlaylistImage(selectedImageByte);
            playlistAdapter.notifyDataSetChanged();
        }
    }
}