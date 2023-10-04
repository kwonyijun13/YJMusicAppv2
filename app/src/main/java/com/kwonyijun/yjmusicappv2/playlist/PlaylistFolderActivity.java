package com.kwonyijun.yjmusicappv2.playlist;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.kwonyijun.yjmusicappv2.AppData;
import com.kwonyijun.yjmusicappv2.DatabaseHelper;
import com.kwonyijun.yjmusicappv2.MainActivity;
import com.kwonyijun.yjmusicappv2.MediaPlayerService;
import com.kwonyijun.yjmusicappv2.MusicManager;
import com.kwonyijun.yjmusicappv2.R;
import com.kwonyijun.yjmusicappv2.song.Song;
import com.kwonyijun.yjmusicappv2.song.SongAdapter;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PlaylistFolderActivity extends AppCompatActivity {
    private DatabaseHelper databaseHelper;
    private RecyclerView recyclerView;
    private MediaPlayerService mediaPlayerService;
    private SongAdapter songAdapter;
    private ArrayList<Song> songArrayList;
    private Playlist playlist;
    private long playlistId;
    private TextView titleTextView;
    private LinearLayout bottomView;
    private ImageView albumImage;
    private TextView bottomTitleTextView, bottomArtistTextView;
    private ImageButton playbackButton, previousButton, nextButton;
    private SeekBar seekbar;
    private int newSongTiming;
    private boolean isAscending, sortByTitle = true, sortByArtist, sortByDate, isShuffle = false, isLooping = true, isRepeatSong = false;
    private boolean isServiceBound = false;
    // USE SERVICECONNECTION TO HANDLE CONNECTION TO MEDIAPLAYERSERVICE
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            // AFTER CONNECTION, CAST IBINDER TO MEDIAPLAYERBINDER AND GET THE SERVICE INSTANCE
            MediaPlayerService.MediaPlayerBinder binder = (MediaPlayerService.MediaPlayerBinder) iBinder;
            mediaPlayerService = binder.getService();
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_folder);

        // INITIALIZE DATABASEHELPER
        databaseHelper = new DatabaseHelper(this);
        playlist = new Playlist();

        // BIND TO MEDIAPLAYERSERVICE
        Intent serviceIntent = new Intent(this, MediaPlayerService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        // SET UP RECYCLERVIEW
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // BACK BUTTON
        ImageButton backButton = findViewById(R.id.back_imageButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        // RETRIEVE INTENT DATA
        Intent intent = getIntent();
        playlistId = intent.getLongExtra("id", 0);
        playlist = databaseHelper.getPlaylistById(playlistId);

        if (playlist != null) {
            // SET TITLE
            titleTextView = findViewById(R.id.title_textView);
            titleTextView.setText(playlist.getPlaylistName());

            // CONVERT BYTE[] TO BITMAP
            Bitmap retrievedImage = BitmapFactory.decodeByteArray(playlist.getPlaylistImage(), 0, playlist.getPlaylistImage().length);
            // SET ALBUM IMAGE
            ImageView albumImage = findViewById(R.id.album_imageView);
            albumImage.setImageBitmap(retrievedImage);

            // RETRIEVE FROM DATABASE
            songArrayList = MusicManager.retrieveSongsByPlaylist(this, playlistId);

            // SET UP RECYCLERVIEW
            recyclerView = findViewById(R.id.recyclerView);
            songAdapter = new SongAdapter(this, songArrayList);
            songAdapter.setItemClickListener(new SongAdapter.ItemClickListener() {
                @Override
                public void onItemClick(int position, String filename, ImageView imageView, TextView title, TextView artist, int duration) {
                    mediaPlayerService.setSelectedSong(songArrayList, position);
                    updateUI();
                }

                @Override
                public void onOptionsIconClick(int position) {}
            });
            recyclerView.setAdapter(songAdapter);
        }

        // SHUFFLE BUTTON
        ImageButton shuffleImageButton = findViewById(R.id.shuffle_imageButton);
        shuffleImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isShuffle = !isShuffle;
                if (isShuffle) {
                    shuffleImageButton.setImageResource(R.drawable.ic_shuffle_on);
                    if (!mediaPlayerService.isPlaying()) {
                        int randomIndex = new Random().nextInt(songArrayList.size() + 1);
                        mediaPlayerService.setSelectedSong(songArrayList, randomIndex);
                        mediaPlayerService.toggleShuffle();
                    } else {
                        mediaPlayerService.toggleShuffle();
                    }
                } else {
                    shuffleImageButton.setImageResource(R.drawable.ic_shuffle);
                }
            }
        });

        // REPEAT SONG BUTTON
        ImageButton repeatSongImageButton = findViewById(R.id.repeat_one_imageButton);
        repeatSongImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mediaPlayerService.toggleRepeat();
                isRepeatSong = !isRepeatSong;
                if (isRepeatSong) {
                    repeatSongImageButton.setImageResource(R.drawable.ic_repeat_one_on);
                } else {
                    repeatSongImageButton.setImageResource(R.drawable.ic_repeat_one);
                }
            }
        });

        // REPEAT LIST BUTTON
        ImageButton repeatImageButton = findViewById(R.id.repeat_imageButton);
        repeatImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mediaPlayerService.toggleLoop();
                isLooping = !isLooping;
                if (isLooping) {
                    repeatImageButton.setImageResource(R.drawable.ic_repeat_on);
                } else {
                    repeatImageButton.setImageResource(R.drawable.ic_repeat);
                }
            }
        });

        // BOTTOM VIEW
        bottomView = findViewById(R.id.custom_bottom_view);
        albumImage = findViewById(R.id.bottom_album_imageView);
        bottomTitleTextView = findViewById(R.id.song_title_textView);
        bottomArtistTextView = findViewById(R.id.artist_name_textView);
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
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mediaPlayerService.playNextSong();
                updateUI();
            }
        });

        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mediaPlayerService.playPreviousSong();
                updateUI();
            }
        });
    }

    public void updateUI() {
        if (mediaPlayerService != null) {
            if (mediaPlayerService.isPlaying()) {
                bottomView.setVisibility(View.VISIBLE);
                albumImage.setImageBitmap(mediaPlayerService.getAlbumImage());
                bottomTitleTextView.setText(mediaPlayerService.getSongTitle());
                bottomArtistTextView.setText(mediaPlayerService.getArtist());
                playbackButton.setImageResource(R.drawable.ic_pause);
                setUpSeekBar(mediaPlayerService.getDuration());

                // TEXTVIEWS ANIMATION
                bottomTitleTextView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                bottomTitleTextView.setSelected(true);
                bottomArtistTextView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                bottomArtistTextView.setSelected(true);
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