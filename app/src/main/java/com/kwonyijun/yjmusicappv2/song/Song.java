package com.kwonyijun.yjmusicappv2.song;

import android.graphics.Bitmap;

public class Song {
    private Bitmap albumImage;
    private String filePath, title, artist, date, album;
    private int id, duration;
    public Song(int id, String filePath, Bitmap albumImage, String title, String artist, String album, int duration, String date) {
        this.id = id;
        this.filePath = filePath;
        this.albumImage = albumImage;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.duration = duration;
        this.date = date;
    }

    public int getId() {
        return id;
    }
    public String getFilePath() {
        return filePath;
    }

    public Bitmap getAlbumImage() {
        return albumImage;
    }
    public Bitmap setAlbumImage(Bitmap albumImage) {
        this.albumImage = albumImage;
        return albumImage;
    }

    public String getTitle() {
        return title;
    }
    public String setTitle(String title) {
        this.title = title;
        return title;
    }

    public String getArtist() {
        return artist;
    }
    public String setArtist(String artist) {
        this.artist = artist;
        return artist;
    }

    public String getAlbum() {
        return album;
    }
    public String setAlbum(String album) {
        this.album = album;
        return album;
    }

    public int getDuration() {
        return duration;
    }
    public String getDate() {
        return date;
    }
}
