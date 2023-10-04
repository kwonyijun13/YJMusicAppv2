package com.kwonyijun.yjmusicappv2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.kwonyijun.yjmusicappv2.song.Song;
import com.kwonyijun.yjmusicappv2.song.SongFragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class MediaPlayerService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {
    private MediaPlayer mediaPlayer;
    private MediaPlayerServiceCallback callback;
    private Song song;
    private ArrayList<Song> songArrayList;
    private SongFragment songFragment;
    private NotificationManagerCompat notificationManager;
    private NotificationCompat.Builder builder;
    private boolean isMediaPlaying = false, isShuffleOn = false, isRepeatOn = false, isLooping = true;
    private int position;
    // REFERENCE TO SONGSFRAGMENT IS STORED HERE
    public void setSongsFragment(SongFragment fragment) {
        songFragment = fragment;
    }

    // CALLBACK TO USE MAINACTIVITY FUNCTION
    public void setMediaPlayerServiceCallback(MediaPlayerServiceCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        if (isRepeatOn) {
            mediaPlayer.setLooping(true);
        }
        mediaPlayer.start();
        updateBottomViewUI();
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        if (isLooping) {
            playNextSong();
        } else {
            isMediaPlaying = false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        mediaPlayer.release();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(5, builder.build());
        if (intent.getAction() != null) {
            switch (intent.getAction()) {
                case "ACTION_TOGGLE":
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        isMediaPlaying = false;
                    } else {
                        mediaPlayer.start();
                        isMediaPlaying = true;
                    }
                    break;
                case "ACTION_PREVIOUS":
                    playPreviousSong();
                    break;
                case "ACTION_NEXT":
                    playNextSong();
                    break;
            }
            updateNotification();
            updateBottomViewUI();
        }
        return START_STICKY;
    }

    private void updateNotification() {
        // CUSTOM NOTIFICATION LAYOUT
        RemoteViews notificationLayout = new RemoteViews(getPackageName(), R.layout.notification_layout);

        // SET UP THE VIEWS IN NOTIFICATION
        notificationLayout.setImageViewBitmap(R.id.album_imageView, song.getAlbumImage());
        notificationLayout.setTextViewText(R.id.song_title_textView, song.getTitle());
        notificationLayout.setTextViewText(R.id.artist_name_textView, song.getArtist());
        notificationLayout.setTextViewText(R.id.album_textView, song.getAlbum());

        // Update the playback button icon based on the current playback state
        int playbackIconRes = isMediaPlaying ? R.drawable.ic_pause : R.drawable.ic_play_arrow;
        notificationLayout.setImageViewResource(R.id.playback_imageButton, playbackIconRes);

        // BUILD THE NOTIFICATION
        builder = new NotificationCompat.Builder(this, "CHANNEL_ID")
                .setSmallIcon(R.drawable.icon)
                .setCustomContentView(notificationLayout)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setSilent(true)
                .setOngoing(isMediaPlaying);

        // CLICK LISTENER FOR PLAYBACKBUTTON
        Intent toggleIntent = new Intent(this, MediaPlayerService.class);
        toggleIntent.setAction("ACTION_TOGGLE");
        PendingIntent togglePendingIntent = PendingIntent.getService(this, 0, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationLayout.setOnClickPendingIntent(R.id.playback_imageButton, togglePendingIntent);

        // CLICK LISTENER FOR PREVIOUS AND NEXT BUTTON
        Intent previousIntent = new Intent(this, MediaPlayerService.class);
        previousIntent.setAction("ACTION_PREVIOUS");
        PendingIntent previousPendingIntent = PendingIntent.getService(this, 0, previousIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationLayout.setOnClickPendingIntent(R.id.previous_imageButton, previousPendingIntent);

        Intent nextIntent = new Intent(this, MediaPlayerService.class);
        nextIntent.setAction("ACTION_NEXT");
        PendingIntent nextPendingIntent = PendingIntent.getService(this, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationLayout.setOnClickPendingIntent(R.id.next_imageButton, nextPendingIntent);

        // SET UP NOTIFICATION INTENT (ON PRESS)
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        notificationManager = NotificationManagerCompat.from(this);

        // SHOW NOTIFICATION
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationManager.notify(5, builder.build());
    }

    public void setSelectedSong(ArrayList<Song> songArrayList, int position) {
        this.songArrayList = songArrayList;
        this.position = position;
        song = songArrayList.get(position);
        playSong();
    }

    public void playSong() {
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(song.getFilePath());
            mediaPlayer.prepareAsync();
            isMediaPlaying = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void playNextSong() {
        if (position < songArrayList.size() - 1) {
            if (isShuffleOn) {
                int randomIndex = new Random().nextInt(songArrayList.size() + 1);
                position = randomIndex;
            } else {
                position++;
            }
            song = songArrayList.get(position);
            try {
                mediaPlayer.reset();
                mediaPlayer.setDataSource(song.getFilePath());
                mediaPlayer.prepareAsync();
                isMediaPlaying = true;
                updateNotification();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public void playPreviousSong() {
        if (position > 0) {
            if (isShuffleOn) {
                int randomIndex = new Random().nextInt(songArrayList.size() + 1);
                position = randomIndex;
            } else {
                position--;
            }
            song = songArrayList.get(position);
            try {
                mediaPlayer.reset();
                mediaPlayer.setDataSource(song.getFilePath());
                mediaPlayer.prepareAsync();
                isMediaPlaying = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void toggleShuffle() {
        isShuffleOn = !isShuffleOn;
        if (isShuffleOn = true) {
            int randomIndex = new Random().nextInt(songArrayList.size() + 1);
            position = randomIndex;
        } else {
            if (position < songArrayList.size() - 1) {
                position++;
            } else {
                position = 0;
            }
        }
    }

    public void toggleRepeat() {
        isRepeatOn = !isRepeatOn;
        mediaPlayer.setLooping(isRepeatOn);
    }

    public void toggleLoop() {
        isLooping = !isLooping;
    }

    public void pauseSong() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isMediaPlaying = false;
        }
    }

    public void continueSong() {
        mediaPlayer.start();
        isMediaPlaying = true;
    }

    public void stopSong() {
        mediaPlayer.stop();
        mediaPlayer.reset();
        isMediaPlaying = false;
    }

    public int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    public void seekTo(int timing) {
        mediaPlayer.seekTo(timing);
    }

    public Bitmap getAlbumImage() {
        return song.getAlbumImage();
    }

    public String getSongTitle() {
        return song.getTitle();
    }

    public String getArtist() {
        return song.getArtist();
    }

    public String getAlbum() {
        return song.getAlbum();
    }

    public int getDuration() {
        return song.getDuration();
    }

    public boolean isPlaying() {
        if (isMediaPlaying) {
            return true;
        } else {
            return false;
        }
    }

    // CALLBACK FROM MAINACTIVITY
    private void updateBottomViewUI() {
        if (callback != null) {
            callback.updateUI();
        }
    }

    // BINDER CLASS FOR COMMUNICATION BETWEEN ACTIVITY & SERVICE
    public class MediaPlayerBinder extends Binder {
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }

    private final IBinder binder = new MediaPlayerBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
