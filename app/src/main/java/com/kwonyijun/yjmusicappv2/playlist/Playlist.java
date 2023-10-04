package com.kwonyijun.yjmusicappv2.playlist;

import android.graphics.Bitmap;

public class Playlist {
    private long id;
    private byte[] playlistCover;
    private String name;
    public Playlist() {}
    public Playlist(long id, byte[] playlistCover, String name) {
        this.id = id;
        this.playlistCover = playlistCover;
        this.name = name;
    }

    public Long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public byte[] getPlaylistImage() {
        return playlistCover;
    }
    public void setPlaylistImage(byte[] playlistCover) {
        this.playlistCover = playlistCover;
    }
    public String getPlaylistName() {
        return name;
    }
    public void setPlaylistName(String name) {
        this.name = name;
    }
}
