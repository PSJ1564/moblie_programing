package com.example.pineapplegame;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;

public class OptionActivity extends AppCompatActivity {

    private boolean isMusicPlaying;
    private boolean isSfxEnabled;
    private Intent musicIntent;
    private SharedPreferences sharedPreferences;

    private static final String PREFS_NAME = "MusicPrefs";
    private static final String KEY_MUSIC_PLAYING = "isMusicPlaying";
    private static final String KEY_MUSIC_VOLUME = "musicVolume";
    private static final String KEY_SFX_ENABLED = "isSoundEnabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_option);

        Button btnMusic = findViewById(R.id.btnMusic);
        Button btnSfx = findViewById(R.id.btnSfx);
        Button btnMain = findViewById(R.id.btnMain);
        SeekBar volSeek = findViewById(R.id.seekVol);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        isMusicPlaying = sharedPreferences.getBoolean(KEY_MUSIC_PLAYING, true);
        isSfxEnabled = sharedPreferences.getBoolean(KEY_SFX_ENABLED, true);
        float savedVolume = sharedPreferences.getFloat(KEY_MUSIC_VOLUME, 1.0f);

        musicIntent = new Intent(this, BackgroundMusicService.class);

        if (isMusicPlaying) {
            startService(musicIntent);
        }

        btnMusic.setText(isMusicPlaying ? "Stop Music" : "Play Music");
        btnSfx.setText(isSfxEnabled ? "SFX OFF" : "SFX ON");
        volSeek.setProgress((int) (savedVolume * 100));

        btnMusic.setOnClickListener(v -> {
            isMusicPlaying = !isMusicPlaying;

            if (isMusicPlaying) {
                startService(musicIntent);
                btnMusic.setText("Stop Music");
            } else {
                stopService(musicIntent);
                btnMusic.setText("Play Music");
            }

            sharedPreferences.edit().putBoolean(KEY_MUSIC_PLAYING, isMusicPlaying).apply();
        });


        btnSfx.setOnClickListener(v -> {
            isSfxEnabled = !isSfxEnabled;
            btnSfx.setText(isSfxEnabled ? "SFX OFF" : "SFX ON");

            sharedPreferences.edit().putBoolean(KEY_SFX_ENABLED, isSfxEnabled).apply();
        });

        volSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float volume = progress / 100f;
                sharedPreferences.edit().putFloat(KEY_MUSIC_VOLUME, volume).apply();

                Intent volumeIntent = new Intent(OptionActivity.this, BackgroundMusicService.class);
                volumeIntent.setAction("SET_VOLUME");
                volumeIntent.putExtra("volume", volume);
                startService(volumeIntent);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnMain.setOnClickListener(v -> finish());
    }
}
