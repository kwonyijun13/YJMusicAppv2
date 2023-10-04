package com.kwonyijun.yjmusicappv2.album;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kwonyijun.yjmusicappv2.MusicManager;
import com.kwonyijun.yjmusicappv2.R;

import java.util.ArrayList;

public class AlbumFragment extends Fragment implements AlbumAdapter.ItemClickListener {
    private RecyclerView recyclerView;
    private AlbumAdapter albumAdapter;
    private MusicManager musicManager;
    private ArrayList<Album> albumList;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_album, container, false);

        recyclerView = rootView.findViewById(R.id.albums_recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 2)); // 2 columns

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // Update UI with loaded albums on the main thread
                view.post(new Runnable() {
                    @Override
                    public void run() {
                        getAlbumList();
                    }
                });
            }
        });
        thread.start();
    }

    private void getAlbumList() {
        albumList = new ArrayList<>();

        albumList = musicManager.retrieveAlbums(getContext(), null);
        albumAdapter = new AlbumAdapter(getActivity(), albumList);
        if (recyclerView != null) {
            recyclerView.setAdapter(albumAdapter);
        }
        albumAdapter.setItemClickListener(this);
    }

    @Override
    public void onItemClick(int position) {
        if (albumList != null && position >= 0 && position < albumList.size()) {
            Album selectedAlbum = albumList.get(position);
        }
    }
}
