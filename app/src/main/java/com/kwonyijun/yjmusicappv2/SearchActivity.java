package com.kwonyijun.yjmusicappv2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.kwonyijun.yjmusicappv2.song.Song;
import com.kwonyijun.yjmusicappv2.song.SongAdapter;

import java.util.ArrayList;

public class SearchActivity extends AppCompatActivity implements SongAdapter.ItemClickListener {
    private EditText searchBarEditText;
    private RecyclerView recyclerView;
    private MusicManager musicManager;
    private MediaPlayerService mediaPlayerService;
    private SongAdapter songAdapter;
    private ArrayList<Song> songArrayList;
    private boolean isServiceBound = false;
    // USE SERVICECONNECTION TO HANDLE CONNECTION TO MEDIAPLAYERSERVICE
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            // AFTER CONNECTION, CAST IBINDER TO MEDIAPLAYERBINDER AND GET THE SERVICE INSTANCE
            MediaPlayerService.MediaPlayerBinder binder = (MediaPlayerService.MediaPlayerBinder) iBinder;
            mediaPlayerService = binder.getService();
            isServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isServiceBound = false;
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        ImageButton backImageButton = findViewById(R.id.back_imageButton);
        ImageButton clearImageButton = findViewById(R.id.clear_imageButton);
        searchBarEditText = findViewById(R.id.search_editText);
        searchBarEditText.requestFocus();

        // BIND TO MEDIAPLAYERSERVICE
        Intent intent = new Intent(this, MediaPlayerService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        // SET UP RECYCLERVIEW
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(SearchActivity.this));

        backImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        searchBarEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    retrieveMp3Files();
                    hideKeyboard();
                    return true; // Returning true consumes the event
                }
                return false; // Returning false allows the default behavior to occur.
            }
        });

        clearImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchBarEditText.setText("");
            }
        });

        // CREATE NOTIFICATION CHANNEL
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence channelName = "Show currently playing";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("CHANNEL_ID", channelName, importance);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void retrieveMp3Files() {
        String searchQuery = searchBarEditText.getText().toString();
        songArrayList = musicManager.retrieveSongs(SearchActivity.this, searchQuery);
        songAdapter = new SongAdapter(SearchActivity.this, songArrayList);
        recyclerView.setAdapter(songAdapter);
        songAdapter.setItemClickListener(this);
    }

    @Override
    public void onItemClick(int position, String filename, ImageView imageView, TextView title, TextView artist, int duration) {
        mediaPlayerService.setSelectedSong(songArrayList, position);
        showCustomNotification();
    }

    @Override
    public void onOptionsIconClick(int position) {}

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unbind from the MediaPlayerService when your activity is destroyed
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
    }

    public void showCustomNotification() {
        // CUSTOM NOTIFICATION LAYOUT
        RemoteViews notificationLayout = new RemoteViews(getPackageName(), R.layout.notification_layout);

        // SET UP THE VIEWS IN NOTIFICATION
        Bitmap currentSongAlbumImage = mediaPlayerService.getAlbumImage();
        String currentSongTitle = mediaPlayerService.getSongTitle();
        String currentSongArtist = mediaPlayerService.getArtist();
        String currentSongAlbum = mediaPlayerService.getAlbum();
        notificationLayout.setImageViewBitmap(R.id.album_imageView, currentSongAlbumImage);
        notificationLayout.setTextViewText(R.id.song_title_textView, currentSongTitle);
        notificationLayout.setTextViewText(R.id.artist_name_textView, currentSongArtist);
        notificationLayout.setTextViewText(R.id.album_textView, currentSongAlbum);

        // Update the playback button icon based on the current playback state
        int playbackIconRes = mediaPlayerService.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play_arrow;
        notificationLayout.setImageViewResource(R.id.playback_imageButton, playbackIconRes);

        // BUILD THE NOTIFICATION
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "CHANNEL_ID")
                .setSmallIcon(R.drawable.icon)
                .setCustomContentView(notificationLayout)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setSilent(true)
                .setOngoing(true);

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

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // CHECK IF PERMISSION IS GRANTED
        if (ActivityCompat.checkSelfPermission(SearchActivity.this, android.Manifest.permission.ACCESS_NOTIFICATION_POLICY) != PackageManager.PERMISSION_GRANTED) {
            // REQUEST PERMISSION
            ActivityCompat.requestPermissions(SearchActivity.this, new String[]{Manifest.permission.ACCESS_NOTIFICATION_POLICY}, 2);
        } else {
            // SHOW NOTIFICATION
            notificationManager.notify(5, builder.build());
        }
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}