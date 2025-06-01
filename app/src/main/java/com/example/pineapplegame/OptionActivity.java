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
        isMusicPlaying = sharedPreferences.getBoolean(KEY_MUSIC_PLAYING, false);
        isSfxEnabled = sharedPreferences.getBoolean(KEY_SFX_ENABLED, true);
        float savedVolume = sharedPreferences.getFloat(KEY_MUSIC_VOLUME, 1.0f);

        // 초기 버튼 텍스트 설정
        btnMusic.setText(isMusicPlaying ? "Stop Music" : "Play Music");
        btnSfx.setText(isSfxEnabled ? "SFX OFF" : "SFX ON");
        volSeek.setProgress((int) (savedVolume * 100));

        // 배경음 서비스 intent
        musicIntent = new Intent(this, BackgroundMusicService.class);

        btnMusic.setOnClickListener(v -> {
            isMusicPlaying = !isMusicPlaying;

            if (isMusicPlaying) {
                startService(musicIntent);
                btnMusic.setText("Stop Music");
            } else {
                stopService(musicIntent);
                btnMusic.setText("Play Music");
            }

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_MUSIC_PLAYING, isMusicPlaying);
            editor.apply();
        });

        btnSfx.setOnClickListener(v -> {
            isSfxEnabled = !isSfxEnabled;
            btnSfx.setText(isSfxEnabled ? "SFX ON" : "SFX OFF");

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_SFX_ENABLED, isSfxEnabled);
            editor.apply();
        });

        volSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float volume = progress / 100f;

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putFloat(KEY_MUSIC_VOLUME, volume);
                editor.apply();

                // 실시간 반영 (옵션): BackgroundMusicService에 intent 전송 가능
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
