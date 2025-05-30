package com.example.pineapplegame;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences("MusicPrefs", MODE_PRIVATE);
        // 자동으로 음악 재생
        boolean isMusicPlaying = sharedPreferences.getBoolean("isMusicPlaying", false);
        if (!isMusicPlaying) {
            startService(new Intent(this, BackgroundMusicService.class));
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isMusicPlaying", true);
            editor.apply();
        }
        ImageView logo = findViewById(R.id.logoImage);
        logo.setVisibility(View.VISIBLE);
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.pop_in);
        logo.startAnimation(anim);

        // 버튼 리스너 설정
        findViewById(R.id.btnRanking).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RankingActivity.class));
        });

        findViewById(R.id.btnStartGame).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, GameActivity.class));
        });

        findViewById(R.id.buttonScore).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ScoreActivity.class));
        });

        findViewById(R.id.buttonSetting).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, OptionActivity.class));
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 앱 종료 시 음악도 중지 (원하는 경우 유지 가능)
        stopService(new Intent(this, BackgroundMusicService.class));

        sharedPreferences = getSharedPreferences("MusicPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isMusicPlaying", false);
        editor.apply();
    }
}
