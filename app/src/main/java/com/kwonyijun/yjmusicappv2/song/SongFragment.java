package com.kwonyijun.yjmusicappv2.song;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.kwonyijun.yjmusicappv2.AppData;
import com.kwonyijun.yjmusicappv2.DatabaseHelper;
import com.kwonyijun.yjmusicappv2.MainActivity;
import com.kwonyijun.yjmusicappv2.MediaPlayerService;
import com.kwonyijun.yjmusicappv2.R;
import com.kwonyijun.yjmusicappv2.playlist.Playlist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class SongFragment extends Fragment implements SongAdapter.ItemClickListener, AddToPlaylistAdapter.ItemClickListener {
    private RecyclerView recyclerView;
    private DatabaseHelper databaseHelper;
    private SongAdapter songAdapter;
    private ArrayList<Song> songArrayList;
    private Song song;
    private List<Playlist> playlists;
    private AddToPlaylistAdapter addToPlaylistAdapter;
    private BottomSheetDialog playlistBottomSheetDialog;
    private boolean isAscending, sortByTitle = true, sortByArtist, sortByDate, isShuffle = false, isLooping = true, isRepeatSong = false;
    private MediaPlayerService mediaPlayerService;
    // BIND TO THE MEDIAPLAYERSERVICE USING SERVICECONNECTION INTERFACE
    private boolean isServiceBound;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MediaPlayerService.MediaPlayerBinder binder = (MediaPlayerService.MediaPlayerBinder) iBinder;
            mediaPlayerService = binder.getService();
            mediaPlayerService.setSongsFragment(SongFragment.this);
            isServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isServiceBound = false;
        }
    };

    public SongFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // BINDING OF SERVICE
        Intent serviceIntent = new Intent(getContext(), MediaPlayerService.class);
        getActivity().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        // CREATE NOTIFICATION CHANNEL
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence channelName = "Show currently playing";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("CHANNEL_ID", channelName, importance);
            NotificationManager notificationManager = getActivity().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_song, container, false);

        // SET UP RECYCLERVIEW
        recyclerView = rootView.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        // RETRIEVE MP3 FILES FROM APPDATA
        retrievemp3Files();

        // SHUFFLE BUTTON
        ImageButton shuffleImageButton = rootView.findViewById(R.id.shuffle_imageButton);
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
                showCustomNotification();
            }
        });

        // REPEAT SONG BUTTON
        ImageButton repeatSongImageButton = rootView.findViewById(R.id.repeat_one_imageButton);
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
        ImageButton repeatImageButton = rootView.findViewById(R.id.repeat_imageButton);
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

        // SORT BUTTON
        ImageButton sortImageButton = rootView.findViewById(R.id.sort_imageButton);
        sortImageButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("NonConstantResourceId")
            @Override
            public void onClick(View view) {
                // CREATE BOTTOMSHEETDIALOG WITH CUSTOM LAYOUT
                BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
                bottomSheetDialog.setContentView(R.layout.layout_bottom_sort_popup);

                RadioGroup radioGroup = bottomSheetDialog.findViewById(R.id.sort_radioGroup);
                RadioGroup alphaRadioGroup = bottomSheetDialog.findViewById(R.id.sort_alpha_radioGroup);

                // LOAD SAVED SELECTION FROM SHAREDPREFERENCES
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                int savedSelection = sharedPreferences.getInt("selectedRadioButton", -1);
                if (savedSelection != -1) {
                    RadioButton radioButton = radioGroup.findViewById(savedSelection);
                    radioButton.setChecked(true);
                }

                int savedAlphaSelection = sharedPreferences.getInt("selectedAlphaRadioButton", -1);
                if (savedAlphaSelection != -1) {
                    RadioButton radioButton = alphaRadioGroup.findViewById(savedAlphaSelection);
                    radioButton.setChecked(true);
                }

                // SAVE THE SELECTED RADIOBUTTON WITH SHAREDPREFERENCES
                radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
                    RadioButton selectedRadioButton = bottomSheetDialog.findViewById(checkedId);
                    if (selectedRadioButton != null) {
                        if (selectedRadioButton.getId() == R.id.title_radioButton) {
                            sortByTitle = true;
                            sortByArtist = false;
                            sortByDate = false;
                        } else if (selectedRadioButton.getId() == R.id.artist_radioButton) {
                            sortByTitle = false;
                            sortByArtist = true;
                            sortByDate = false;
                        } else {
                            sortByTitle = false;
                            sortByArtist = false;
                            sortByDate = true;
                        }
                    }

                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putInt("selectedRadioButton", checkedId);
                    editor.apply();
                });

                alphaRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
                    RadioButton selectedRadioButton = bottomSheetDialog.findViewById(checkedId);
                    if (selectedRadioButton != null) {
                        if (selectedRadioButton.getId() == R.id.ascending_radioButton) {
                            isAscending = true;
                        } else if (selectedRadioButton.getId() == R.id.descending_radioButton) {
                            isAscending = false;
                        }
                    }

                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putInt("selectedAlphaRadioButton", checkedId);
                    editor.apply();
                });

                Button sortButton = bottomSheetDialog.findViewById(R.id.sort_button);
                sortButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        sortMusic();
                        bottomSheetDialog.dismiss();
                    }
                });
                bottomSheetDialog.show();
            }
        });

        return rootView;
    }

    public void retrievemp3Files() {
        // RETRIEVE FROM APPDATA
        songArrayList = AppData.getInstance().getSongList();
        songAdapter = new SongAdapter(getActivity(), songArrayList);
        recyclerView.setAdapter(songAdapter);
        songAdapter.setItemClickListener(this);
    }

    @Override
    public void onItemClick(int position, String filename, ImageView imageView, TextView title, TextView artist, int duration) {
        mediaPlayerService.setSelectedSong(songArrayList, position);
        showCustomNotification();
    }

    @Override
    public void onOptionsIconClick(int position) {
        song = songArrayList.get(position);

        // CREATE BOTTOMSHEETDIALOG WITH CUSTOM LAYOUT
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        bottomSheetDialog.setContentView(R.layout.layout_bottom_music_popup);

        // SET UP VIEWS
        ImageView albumImageView = bottomSheetDialog.findViewById(R.id.album_imageView);
        albumImageView.setImageBitmap(song.getAlbumImage());
        TextView titleTextView = bottomSheetDialog.findViewById(R.id.title_textView);
        titleTextView.setText(song.getTitle());
        TextView artistTextView = bottomSheetDialog.findViewById(R.id.artist_name_textView);
        artistTextView.setText(song.getArtist());

        // ADD TO PLAYLIST
        TextView addToPlaylistTextView = bottomSheetDialog.findViewById(R.id.add_to_playlist_textView);
        addToPlaylistTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPlaylistsBottomSheetDialog();
                bottomSheetDialog.dismiss();
            }
        });

        bottomSheetDialog.show();
    }

    public void showCustomNotification() {
        // CUSTOM NOTIFICATION LAYOUT
        RemoteViews notificationLayout = new RemoteViews(getActivity().getPackageName(), R.layout.notification_layout);

        // SET UP THE VIEWS IN NOTIFICATION
        Bitmap currentSongAlbumImage = mediaPlayerService.getAlbumImage();
        String currentSongTitle = mediaPlayerService.getSongTitle();
        String currentSongArtist = mediaPlayerService.getArtist();
        String currentSongAlbum = mediaPlayerService.getAlbum();
        notificationLayout.setImageViewBitmap(R.id.album_imageView, currentSongAlbumImage);
        notificationLayout.setTextViewText(R.id.song_title_textView, currentSongTitle);
        notificationLayout.setTextViewText(R.id.artist_name_textView, currentSongArtist);
        notificationLayout.setTextViewText(R.id.album_textView, currentSongAlbum);

        // BUILD THE NOTIFICATION
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext(), "CHANNEL_ID")
                .setSmallIcon(R.drawable.icon)
                .setCustomContentView(notificationLayout)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setSilent(true)
                .setOngoing(true);

        // CLICK LISTENER FOR PLAYBACKBUTTON
        Intent toggleIntent = new Intent(getActivity(), MediaPlayerService.class);
        toggleIntent.setAction("ACTION_TOGGLE");
        PendingIntent togglePendingIntent = PendingIntent.getService(getContext(), 0, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationLayout.setOnClickPendingIntent(R.id.playback_imageButton, togglePendingIntent);

        // CLICK LISTENER FOR PREVIOUS AND NEXT BUTTON
        Intent previousIntent = new Intent(getActivity(), MediaPlayerService.class);
        previousIntent.setAction("ACTION_PREVIOUS");
        PendingIntent previousPendingIntent = PendingIntent.getService(getContext(), 0, previousIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationLayout.setOnClickPendingIntent(R.id.previous_imageButton, previousPendingIntent);

        Intent nextIntent = new Intent(getActivity(), MediaPlayerService.class);
        nextIntent.setAction("ACTION_NEXT");
        PendingIntent nextPendingIntent = PendingIntent.getService(getContext(), 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationLayout.setOnClickPendingIntent(R.id.next_imageButton, nextPendingIntent);

        // SET UP NOTIFICATION INTENT (ON PRESS)
        Intent notificationIntent = new Intent(getActivity(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getContext());

        // CHECK IF PERMISSION IS GRANTED
        if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_NOTIFICATION_POLICY) != PackageManager.PERMISSION_GRANTED) {
            // REQUEST PERMISSION
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_NOTIFICATION_POLICY}, 2);
        } else {
            // SHOW NOTIFICATION
            notificationManager.notify(5, builder.build());
        }
    }

    public void showPlaylistsBottomSheetDialog() {
        playlistBottomSheetDialog = new BottomSheetDialog(getContext());
        playlistBottomSheetDialog.setContentView(R.layout.layout_bottom_show_playlist_popup);

        // RETRIEVE PLAYLISTS FROM DATABASE
        databaseHelper = new DatabaseHelper(getActivity());
        playlists = databaseHelper.getAllPlaylists();

        // SET UP PLAYLISTADAPTER
        addToPlaylistAdapter = new AddToPlaylistAdapter(getContext(), playlists);
        addToPlaylistAdapter.setItemClickListener(this);

        // SET UP RECYCLERVIEW
        RecyclerView showPlaylistRecyclerView = playlistBottomSheetDialog.findViewById(R.id.show_playlist_recyclerView);
        showPlaylistRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        showPlaylistRecyclerView.setAdapter(addToPlaylistAdapter);

        playlistBottomSheetDialog.show();
    }

    @Override
    public void onItemClick(int position, String title, ImageView imageView) {
        long playlistId = playlists.get(position).getId();
        databaseHelper.addSongToPlaylist(playlistId, song.getId());
        playlistBottomSheetDialog.dismiss();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isServiceBound) {
            getActivity().unbindService(serviceConnection);
            isServiceBound = false;
        }

    }

    // SORT FUNCTIONS
    private void sortMusic() {
        if (sortByTitle) {
            if (isAscending) {
                sortSongsByTitleAsc();
            } else {
                sortSongsByTitleDesc();
            }
        } else if (sortByArtist) {
            if (isAscending) {
                sortSongsByArtistAsc();
            } else {
                sortSongsByArtistDesc();
            }
        } else {
            if (isAscending) {
                sortSongsByDateAsc();
            } else {
                sortSongsByDateDesc();
            }
        }
    }

    public void sortSongsByTitleAsc() {
        if (songArrayList != null) {
            Collections.sort(songArrayList, new Comparator<Song>() {
                @Override
                public int compare(Song song1, Song song2) {
                    // COMPARE TITLES IN ASC ORDER
                    return song1.getTitle().compareToIgnoreCase(song2.getTitle());
                }
            });
            songAdapter.notifyDataSetChanged();
        }
        isAscending = true;
    }

    public void sortSongsByTitleDesc() {
        if (songArrayList != null) {
            Collections.sort(songArrayList, new Comparator<Song>() {
                @Override
                public int compare(Song song1, Song song2) {
                    // COMPARE TITLES IN DESC ORDER
                    return song2.getTitle().compareToIgnoreCase(song1.getTitle());
                }
            });
            songAdapter.notifyDataSetChanged();
        }
        isAscending = false;
    }

    public void sortSongsByArtistAsc() {
        if (songArrayList != null) {
            Collections.sort(songArrayList, new Comparator<Song>() {
                @Override
                public int compare(Song song1, Song song2) {
                    return song1.getArtist().compareToIgnoreCase(song2.getArtist());
                }
            });
            songAdapter.notifyDataSetChanged();
        }
        isAscending = true;
    }

    public void sortSongsByArtistDesc() {
        if (songArrayList != null) {
            Collections.sort(songArrayList, new Comparator<Song>() {
                @Override
                public int compare(Song song1, Song song2) {
                    return song2.getArtist().compareToIgnoreCase(song1.getArtist());
                }
            });
            songAdapter.notifyDataSetChanged();
        }
        isAscending = false;
    }

    public void sortSongsByDateAsc() {
        if (songArrayList != null) {
            Collections.sort(songArrayList, new Comparator<Song>() {
                @Override
                public int compare(Song song1, Song song2) {
                    return song1.getDate().compareToIgnoreCase(song2.getDate());
                }
            });
            songAdapter.notifyDataSetChanged();
        }
        isAscending = true;
    }

    public void sortSongsByDateDesc() {
        if (songArrayList != null) {
            Collections.sort(songArrayList, new Comparator<Song>() {
                @Override
                public int compare(Song song1, Song song2) {
                    return song2.getDate().compareToIgnoreCase(song1.getDate());
                }
            });
            songAdapter.notifyDataSetChanged();
        }
        isAscending = false;
    }
    // END OF SORT FUNCTIONS
}