package com.kwonyijun.yjmusicappv2;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.kwonyijun.yjmusicappv2.album.AlbumFragment;
import com.kwonyijun.yjmusicappv2.playlist.PlaylistFragment;
import com.kwonyijun.yjmusicappv2.song.Song;
import com.kwonyijun.yjmusicappv2.song.SongAdapter;
import com.kwonyijun.yjmusicappv2.song.SongFragment;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements MediaPlayerServiceCallback {
    private TabLayout tabLayout;
    private ViewPager2 viewPager2;
    private ViewPagerAdapter viewPagerAdapter;
    private MediaPlayerService mediaPlayerService;
    private LinearLayout bottomView;
    private ImageView albumImage;
    private TextView titleTextView, artistTextView;
    private ImageButton playbackButton, previousButton, nextButton;
    private SeekBar seekbar;
    private int newSongTiming;
    private boolean isServiceBound = false, isPauseState = false;
    // USE SERVICECONNECTION TO HANDLE CONNECTION TO MEDIAPLAYERSERVICE
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            // AFTER CONNECTION, CAST IBINDER TO MEDIAPLAYERBINDER AND GET THE SERVICE INSTANCE
            MediaPlayerService.MediaPlayerBinder binder = (MediaPlayerService.MediaPlayerBinder) iBinder;
            mediaPlayerService = binder.getService();
            mediaPlayerService.setMediaPlayerServiceCallback(MainActivity.this); // Register the callback
            isServiceBound = true;

            updateUI();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mediaPlayerService.setMediaPlayerServiceCallback(null); // Unregister the callback
            isServiceBound = false;
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unbind from the MediaPlayerService when your activity is destroyed
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // SET ONLY PORTRAIT
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // SET UP ACTIONBAR
        setCustomActionBar();

        // TABLAYOUT, VIEWPAGER2 & FRAGMENTS
        tabLayout = findViewById(R.id.tabLayout);
        viewPager2 = findViewById(R.id.viewPager2);
        setViewPager();

        // REQUEST PERMISSIONS
        requestStoragePermission();

        // BIND TO MEDIAPLAYERSERVICE
        Intent intent = new Intent(this, MediaPlayerService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        // CREATE NOTIFICATION CHANNEL
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence channelName = "Show currently playing";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("CHANNEL_ID", channelName, importance);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        // BOTTOM VIEW
        bottomView = findViewById(R.id.custom_bottom_view);
        albumImage = findViewById(R.id.album_imageView);
        titleTextView = findViewById(R.id.song_title_textView);
        artistTextView = findViewById(R.id.artist_name_textView);
        playbackButton = findViewById(R.id.playback_imageButton);
        previousButton = findViewById(R.id.previous_imageButton);
        nextButton = findViewById(R.id.next_imageButton);
        seekbar = findViewById(R.id.seekBar);

        playbackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mediaPlayerService.isPlaying()) {
                    mediaPlayerService.pauseSong();
                    playbackButton.setImageResource(R.drawable.ic_play_arrow);
                } else {
                    mediaPlayerService.continueSong();
                    playbackButton.setImageResource(R.drawable.ic_pause);
                }
                showCustomNotification();
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mediaPlayerService.playNextSong();
                showCustomNotification();
            }
        });

        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mediaPlayerService.playPreviousSong();
                showCustomNotification();
            }
        });
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
        if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_NOTIFICATION_POLICY) != PackageManager.PERMISSION_GRANTED) {
            // REQUEST PERMISSION
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_NOTIFICATION_POLICY}, 2);
        } else {
            // SHOW NOTIFICATION
            notificationManager.notify(5, builder.build());
        }
    }

    private void setCustomActionBar() {
        ActionBar actionBar = getSupportActionBar();

        // SET CUSTOM VIEW FOR ACTIONBAR
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(R.layout.actionbar_custom_layout);

        View customActionBarView = actionBar.getCustomView();
        ImageButton settingsImageButton = customActionBarView.findViewById(R.id.settings_imageButton);
        ImageButton searchImageButton = customActionBarView.findViewById(R.id.search_imageButton);

        searchImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                startActivity(intent);
            }
        });
    }
    private void setViewPager() {
        // CREATE & SET ADAPTER FOR VIEWPAGER2
        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), getLifecycle());
        viewPagerAdapter.addFragment(new PlaylistFragment(), "PLAYLISTS");
        viewPagerAdapter.addFragment(new SongFragment(), "SONGS");
        viewPagerAdapter.addFragment(new AlbumFragment(), "ALBUMS");

        viewPager2.setAdapter(viewPagerAdapter);

        // CONNECT TABLAYOUT WITH VIEWPAGER2
        new TabLayoutMediator(tabLayout, viewPager2, (tab, position) -> tab.setText(viewPagerAdapter.getPageTitle(position))).attach();
    }
    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    showPermissionDeniedDialog();
                }
            });

    private void requestStoragePermission() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }
    private void showPermissionDeniedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permission Required")
                .setMessage("o(TヘTo)")
                .setPositiveButton("Open Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        openAppSettings();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {}
                })
                .show();
    }
    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    public void updateUI() {
        if (mediaPlayerService != null) {
            if (mediaPlayerService.isPlaying()) {
                bottomView.setVisibility(View.VISIBLE);
                albumImage.setImageBitmap(mediaPlayerService.getAlbumImage());
                titleTextView.setText(mediaPlayerService.getSongTitle());
                artistTextView.setText(mediaPlayerService.getArtist());
                playbackButton.setImageResource(R.drawable.ic_pause);
                setUpSeekBar(mediaPlayerService.getDuration());

                // TEXTVIEWS ANIMATION
                titleTextView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                titleTextView.setSelected(true);
                artistTextView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                artistTextView.setSelected(true);
            } else {
                playbackButton.setImageResource(R.drawable.ic_play_arrow);
            }
        }
    }

    private void setUpSeekBar(int duration) {
        seekbar.setMax(duration);

        Handler handler = new Handler();
        Runnable updateSeekBar = new Runnable() {
            @Override
            public void run() {
                int currentTimePosition = 0;
                if (isServiceBound) {
                    currentTimePosition = mediaPlayerService.getCurrentPosition();
                }
                seekbar.setProgress(currentTimePosition);
                handler.postDelayed(this, 100); // UPDATE EVERY 100 MILLISECONDS
            }
        };

        // START MOVING SEEKBAR ACCORDING TO SONG
        handler.postDelayed(updateSeekBar, 100);

        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean userDrag) {
                // UPDATE CURRENT POSITION OF SONG TO MATCH SEEKBAR
                if (userDrag) {
                    newSongTiming = seekbar.getProgress();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mediaPlayerService.pauseSong();
                playbackButton.setImageResource(R.drawable.ic_play_arrow);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mediaPlayerService.seekTo(newSongTiming);
                mediaPlayerService.continueSong();
                playbackButton.setImageResource(R.drawable.ic_pause);
            }
        });
    }
}