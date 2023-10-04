package com.kwonyijun.yjmusicappv2;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.kwonyijun.yjmusicappv2.album.Album;
import com.kwonyijun.yjmusicappv2.song.Song;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class MusicManager {

    public static ArrayList<Song> retrieveSongs(Context context, String searchResult) {
        ArrayList<Song> songFiles = new ArrayList<>();

        final String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATE_ADDED
        };

        String selection = MediaStore.Audio.Media.DATA + " LIKE ?";

        Cursor cursor = null;
        try {
            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

            // FETCH ONLY FROM "MUSIC" FOLDER
            String musicFolderPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();
            String[] selectionArg = new String[]{musicFolderPath + "%"}; // HERE IS THE '?'

            if (searchResult == null || searchResult.equals("")) {
                cursor = context.getContentResolver().query(
                        uri,
                        projection,
                        selection,
                        selectionArg,
                        null
                );
            } else {
                // % ALLOWS ANY CHARACTERS BEFORE AND AFTER THE SEARCHRESULT
                String[] searchArgs = new String[]{"%" + searchResult + "%", "%" + searchResult + "%"};
                String titleSelection = MediaStore.Audio.Media.TITLE + " LIKE ?";
                String artistSelection = MediaStore.Audio.Media.ARTIST + " LIKE ?";
                String musicFolderSelection = MediaStore.Audio.Media.DATA + " LIKE ?";
                musicFolderPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();
                String musicFolderArg = musicFolderPath + "%";

                selection = "(" + titleSelection + " OR " + artistSelection + ") AND " + musicFolderSelection;

                // Combine the searchArgs and musicFolderArg into a single array
                String[] selectionArgs = new String[]{searchArgs[0], searchArgs[1], musicFolderArg};

                cursor = context.getContentResolver().query(
                        uri,
                        projection,
                        selection,
                        selectionArgs,
                        null
                );
            }

            if (cursor != null && cursor.moveToFirst()) {
                int songIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int filePathColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
                int albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                int dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED);

                do {
                    // RETRIEVE THE VALUES FROM THE COLUMNS
                    int songId = cursor.getInt(songIdColumn);
                    String filePath = cursor.getString(filePathColumnIndex);
                    String title = cursor.getString(titleColumn);
                    String artist = cursor.getString(artistColumn);
                    long albumId = cursor.getLong(albumIdColumn);
                    int duration = cursor.getInt(durationColumn);
                    String date = cursor.getString(dateColumn);
                    String album = cursor.getString(albumColumn);

                    // RETRIEVE ALBUM ART USING ALBUM ID
                    // content://media/external/audio/albumart/ : THIS IS THE LOCATION OF THE ALBUM ART
                    Uri albumArtUri = Uri.parse("content://media/external/audio/albumart/" + albumId);
                    Bitmap albumArt = null;

                    try {
                        // LOAD THE ALBUM ART INTO A BITMAP OBJECT BY USING openInputStream()
                        InputStream inputStream = context.getContentResolver().openInputStream(albumArtUri);
                        // DECODE IT USING decodeStream()
                        albumArt = BitmapFactory.decodeStream(inputStream);
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // CREATE A SONGFILE OBJECT AND ADD TO LIST
                    Song song = new Song(songId, filePath, albumArt, title, artist, album, duration, date);
                    songFiles.add(song);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return songFiles;
    }

    public static ArrayList<Song> retrieveSongsByPlaylist(Context context, long playlistId) {
        ArrayList<Song> songFiles = new ArrayList<>();

        // Fetch song IDs associated with the playlist
        DatabaseHelper databaseHelper = new DatabaseHelper(context);
        ArrayList<Long> songIds = databaseHelper.getSongsByPlaylist(playlistId);

        final String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATE_ADDED
        };

        // Use the songIds to filter the songs
        String selection = MediaStore.Audio.Media._ID + " IN (" + TextUtils.join(",", songIds) + ")";

        Cursor cursor = null;
        try {
            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            cursor = context.getContentResolver().query(
                    uri,
                    projection,
                    selection,
                    null,
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                int songIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int filePathColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
                int albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                int dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED);

                do {
                    // RETRIEVE THE VALUES FROM THE COLUMNS
                    int songId = cursor.getInt(songIdColumn);
                    String filePath = cursor.getString(filePathColumnIndex);
                    String title = cursor.getString(titleColumn);
                    String artist = cursor.getString(artistColumn);
                    long albumId = cursor.getLong(albumIdColumn);
                    int duration = cursor.getInt(durationColumn);
                    String date = cursor.getString(dateColumn);
                    String album = cursor.getString(albumColumn);

                    // RETRIEVE ALBUM ART USING ALBUM ID
                    // content://media/external/audio/albumart/ : THIS IS THE LOCATION OF THE ALBUM ART
                    Uri albumArtUri = Uri.parse("content://media/external/audio/albumart/" + albumId);
                    Bitmap albumArt = null;

                    try {
                        // LOAD THE ALBUM ART INTO A BITMAP OBJECT BY USING openInputStream()
                        InputStream inputStream = context.getContentResolver().openInputStream(albumArtUri);
                        // DECODE IT USING decodeStream()
                        albumArt = BitmapFactory.decodeStream(inputStream);
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // CREATE A SONGFILE OBJECT AND ADD TO LIST
                    Song song = new Song(songId, filePath, albumArt, title, artist, album, duration, date);
                    songFiles.add(song);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return songFiles;
    }

    public static ArrayList<Album> retrieveAlbums(Context context, String searchResult) {
        ArrayList<Album> albums = new ArrayList<>();

        final String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATE_ADDED
        };

        String selection = MediaStore.Audio.Media.DATA + " LIKE ?";

        Cursor cursor = null;
        try {
            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

            // FETCH ONLY FROM "MUSIC" FOLDER
            String musicFolderPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();
            String[] selectionArg = new String[]{musicFolderPath + "%"}; // HERE IS THE '?'

            if (searchResult == null || searchResult.equals("")) {
                cursor = context.getContentResolver().query(
                        uri,
                        projection,
                        selection,
                        selectionArg,
                        null
                );
            } else {
                // % ALLOWS ANY CHARACTERS BEFORE AND AFTER THE SEARCHRESULT
                String[] searchArgs = new String[]{"%" + searchResult + "%", "%" + searchResult + "%"};
                String titleSelection = MediaStore.Audio.Media.TITLE + " LIKE ?";
                String artistSelection = MediaStore.Audio.Media.ARTIST + " LIKE ?";
                String musicFolderSelection = MediaStore.Audio.Media.DATA + " LIKE ?";
                musicFolderPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();
                String musicFolderArg = musicFolderPath + "%";

                selection = "(" + titleSelection + " OR " + artistSelection + ") AND " + musicFolderSelection;

                // Combine the searchArgs and musicFolderArg into a single array
                String[] selectionArgs = new String[]{searchArgs[0], searchArgs[1], musicFolderArg};

                cursor = context.getContentResolver().query(
                        uri,
                        projection,
                        selection,
                        selectionArgs,
                        null
                );
            }

            if (cursor != null && cursor.moveToFirst()) {
                int songIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int filePathColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
                int albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                int dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED);

                do {
                    // RETRIEVE THE VALUES FROM THE COLUMNS
                    int songId = cursor.getInt(songIdColumn);
                    String filePath = cursor.getString(filePathColumnIndex);
                    String title = cursor.getString(titleColumn);
                    String artist = cursor.getString(artistColumn);
                    long albumId = cursor.getLong(albumIdColumn);
                    int duration = cursor.getInt(durationColumn);
                    String date = cursor.getString(dateColumn);
                    String album = cursor.getString(albumColumn);

                    // RETRIEVE ALBUM ART USING ALBUM ID
                    // content://media/external/audio/albumart/ : THIS IS THE LOCATION OF THE ALBUM ART
                    Uri albumArtUri = Uri.parse("content://media/external/audio/albumart/" + albumId);
                    Bitmap albumArt = null;

                    try {
                        // LOAD THE ALBUM ART INTO A BITMAP OBJECT BY USING openInputStream()
                        InputStream inputStream = context.getContentResolver().openInputStream(albumArtUri);
                        // DECODE IT USING decodeStream()
                        albumArt = BitmapFactory.decodeStream(inputStream);
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // CREATE A ALBUM OBJECT AND ADD TO LIST
                    Album albumItem = new Album(albumArt, album);
                    albums.add(albumItem);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return albums;
    }
}
