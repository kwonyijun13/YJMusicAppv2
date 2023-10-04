package com.kwonyijun.yjmusicappv2;

import com.kwonyijun.yjmusicappv2.song.Song;

import java.util.ArrayList;

public class AppData {
    private static AppData instance;
    private ArrayList<Song> songList;

    private AppData() {
        // Private constructor to prevent direct instantiation
    }

    public static AppData getInstance() {
        if (instance == null) {
            instance = new AppData();
        }
        return instance;
    }

    public void setSongList(ArrayList<Song> songs) {
        this.songList = songs;
    }

    public ArrayList<Song> getSongList() {
        return songList;
    }
}

