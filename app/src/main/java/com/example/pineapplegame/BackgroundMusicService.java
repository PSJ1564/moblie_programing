package com.example.pineapplegame;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class BackgroundMusicService extends Service {
    private MediaPlayer mediaPlayer;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            AssetFileDescriptor afd = getResources().openRawResourceFd(R.raw.test);
            if (afd == null) return;

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();

            mediaPlayer.setLooping(true);

            float volume = getSharedPreferences("MusicPrefs", MODE_PRIVATE)
                    .getFloat("musicVolume", 1.0f);
            mediaPlayer.setVolume(volume, volume);

            mediaPlayer.prepare();
        } catch (Exception e) {
            // silent fail
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "SET_VOLUME".equals(intent.getAction())) {
            float volume = intent.getFloatExtra("volume", 1.0f);
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(volume, volume);
            }
            return START_NOT_STICKY;
        }

        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
