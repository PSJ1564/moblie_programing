package com.example.pineapplegame;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class OptionActivity extends AppCompatActivity {
    private boolean isMusicPlaying = false;  // 음악이 재생 중인지 확인
    private Intent musicIntent;
    private SharedPreferences sharedPreferences;  // SharedPreferences 객체

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_option);

        Button musicButton = findViewById(R.id.btnMusic);  // 음악 제어 버튼
        Button mainButton = findViewById(R.id.btnMain);  // 메인 화면으로 돌아가는 버튼

        // SharedPreferences를 이용해 이전 상태 복원
        sharedPreferences = getSharedPreferences("MusicPrefs", MODE_PRIVATE);
        isMusicPlaying = sharedPreferences.getBoolean("isMusicPlaying", false);

        // 서비스 시작을 위한 Intent
        musicIntent = new Intent(this, BackgroundMusicService.class);

        // 음악 상태에 맞는 버튼 텍스트 설정
        if (isMusicPlaying) {
            musicButton.setText("Stop Music");  // 음악이 켜져 있으면 "Stop Music"
        } else {
            musicButton.setText("Play Music");  // 음악이 꺼져 있으면 "Play Music"
        }

        // 음악 켜고 끄는 버튼 클릭 리스너
        musicButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isMusicPlaying) {
                    // 음악 켜기
                    startService(musicIntent);
                    musicButton.setText("Stop Music");  // 버튼 텍스트 변경
                } else {
                    // 음악 끄기
                    stopService(musicIntent);
                    musicButton.setText("Play Music");  // 버튼 텍스트 변경
                }
                isMusicPlaying = !isMusicPlaying;  // 상태 토글

                // SharedPreferences에 상태 저장
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("isMusicPlaying", isMusicPlaying);
                editor.apply();  // 저장
            }
        });

        // 메인 화면으로 돌아가는 버튼 클릭 리스너
        mainButton.setOnClickListener(v -> {
            Intent intent = new Intent(OptionActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}