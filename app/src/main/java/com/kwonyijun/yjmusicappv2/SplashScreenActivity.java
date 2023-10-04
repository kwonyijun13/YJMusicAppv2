package com.kwonyijun.yjmusicappv2;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.kwonyijun.yjmusicappv2.song.Song;

import java.util.ArrayList;

@SuppressLint("CustomSplashScreen")
public class SplashScreenActivity extends AppCompatActivity {
    private ArrayList<Song> songArrayList;
    private MusicManager musicManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        // FETCH MP3 FILES & STORE IN APPDATA
        ArrayList<Song> songs = fetchMp3Files();
        AppData.getInstance().setSongList(songs);

        // TODO: FETCH ALBUMS & STORE IN APPDATA

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashScreenActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }, 2500);
    }
    private ArrayList<Song> fetchMp3Files() {
        songArrayList = musicManager.retrieveSongs(this, null);
        return songArrayList;
    }
}