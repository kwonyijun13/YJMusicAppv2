package com.kwonyijun.yjmusicappv2;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.kwonyijun.yjmusicappv2.playlist.Playlist;
import com.kwonyijun.yjmusicappv2.song.Song;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "music_db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_PLAYLISTS = "playlists";
    private static final String KEY_PLAYLIST_ID = "playlist_id";
    private static final String KEY_NAME = "name";
    private static final String KEY_IMAGE = "image_data";
    private static final String TABLE_SONGS = "songs";
    private static final String KEY_ID = "id";
    private static final String KEY_SONG_ID = "song_id";
    private static final String TABLE_PLAYLIST_SONGS = "playlist_songs";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // CREATE PLAYLIST TABLE
        final String CREATE_TABLE_PLAYLISTS =
                "CREATE TABLE " + TABLE_PLAYLISTS + "(" +
                        KEY_PLAYLIST_ID + " INTEGER PRIMARY KEY," +
                        KEY_NAME + " TEXT," +
                        KEY_IMAGE + " BLOB)";
        db.execSQL(CREATE_TABLE_PLAYLISTS);

        // CREATE SONGS TABLE
        final String CREATE_TABLE_SONGS =
                "CREATE TABLE " + TABLE_SONGS + "(" +
                        KEY_ID + " INTEGER PRIMARY KEY," +
                        KEY_NAME + " TEXT," +
                        KEY_SONG_ID + " INTEGER)";
        db.execSQL(CREATE_TABLE_SONGS);

        // CREATE PLAYLIST_SONGS TABLE
        final String CREATE_TABLE_PLAYLIST_SONGS =
                "CREATE TABLE " + TABLE_PLAYLIST_SONGS + "(" +
                        KEY_ID + " INTEGER PRIMARY KEY," +
                        KEY_PLAYLIST_ID + " INTEGER," +
                        KEY_SONG_ID + " INTEGER)";
        db.execSQL(CREATE_TABLE_PLAYLIST_SONGS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    // OPERATIONS
    public long addPlaylist(Playlist playlist, byte[] playlistCover) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, playlist.getPlaylistName());
        values.put(KEY_IMAGE, playlistCover);
        long playlistId = db.insert(TABLE_PLAYLISTS, null, values);
        db.close();

        return playlistId;
    }

    public void deletePlaylist(long playlistId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PLAYLISTS, KEY_PLAYLIST_ID + " = ?", new String[]{String.valueOf(playlistId)});
        db.close();
    }

    public int updatePlaylistName(long playlistId, String newName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, newName);

        // UPDATE ROW
        return db.update(TABLE_PLAYLISTS, values, KEY_PLAYLIST_ID + " = ?", new String[]{String.valueOf(playlistId)});
    }

    public int updatePlaylistCover(long playlistId, byte[] newCover) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_IMAGE, newCover);

        // UPDATE ROW
        return db.update(TABLE_PLAYLISTS, values, KEY_PLAYLIST_ID + " = ?", new String[]{String.valueOf(playlistId)});
    }

    @SuppressLint("Range")
    public List<Playlist> getAllPlaylists() {
        List<Playlist> playlists = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_PLAYLISTS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                Playlist playlist = new Playlist();
                playlist.setId(cursor.getLong(cursor.getColumnIndex(KEY_PLAYLIST_ID)));
                playlist.setPlaylistName(cursor.getString(cursor.getColumnIndex(KEY_NAME)));
                playlist.setPlaylistImage(cursor.getBlob(cursor.getColumnIndex(KEY_IMAGE)));
                playlists.add(playlist);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return playlists;
    }

    @SuppressLint("Range")
    public Playlist getPlaylistById(long playlistId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_PLAYLISTS, null, KEY_PLAYLIST_ID + " = ?", new String[]{String.valueOf(playlistId)}, null, null, null);

        Playlist playlist = new Playlist();
        if (cursor.moveToFirst()) {
            playlist.setId(cursor.getLong(cursor.getColumnIndex(KEY_PLAYLIST_ID)));
            playlist.setPlaylistName(cursor.getString(cursor.getColumnIndex(KEY_NAME)));
            playlist.setPlaylistImage(cursor.getBlob(cursor.getColumnIndex(KEY_IMAGE)));
        }

        cursor.close();
        return playlist;
    }

    public long addSongToPlaylist(long playlistId, long songId) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_PLAYLIST_ID, playlistId);
        values.put(KEY_SONG_ID, songId);

        long rowId = db.insert(TABLE_PLAYLIST_SONGS, null, values);
        db.close();

        return rowId;
    }

    @SuppressLint("Range")
    public ArrayList<Long> getSongsByPlaylist(long playlistId) {
        ArrayList<Long> songIds = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_PLAYLIST_SONGS, new String[]{KEY_SONG_ID}, KEY_PLAYLIST_ID + " = ?", new String[]{String.valueOf(playlistId)}, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                long songId = cursor.getLong(cursor.getColumnIndex(KEY_SONG_ID));
                songIds.add(songId);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return songIds;
    }
}