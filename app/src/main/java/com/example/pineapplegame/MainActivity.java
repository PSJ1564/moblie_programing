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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private SharedPreferences musicPreferences;
    private Button btnLogin;

    private static final String USER_PREFS = "UserPrefs";
    private static final String MUSIC_PREFS = "MusicPrefs";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 애니메이션 로고
        ImageView logo = findViewById(R.id.logoImage);
        logo.setVisibility(View.VISIBLE);
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.pop_in);
        logo.startAnimation(anim);

        // 음악 재생 상태 관리
        musicPreferences = getSharedPreferences(MUSIC_PREFS, MODE_PRIVATE);
        boolean isMusicPlaying = musicPreferences.getBoolean("isMusicPlaying", false);
        if (!isMusicPlaying) {
            startService(new Intent(this, BackgroundMusicService.class));
            SharedPreferences.Editor editor = musicPreferences.edit();
            editor.putBoolean("isMusicPlaying", true);
            editor.apply();
        }

        // 로그인 관련
        sharedPreferences = getSharedPreferences(USER_PREFS, MODE_PRIVATE);
        btnLogin = findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(v -> {
            boolean isLoggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
            if (!isLoggedIn) {
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
            } else {
                logout();
            }
        });

        updateLoginButton();

        // 버튼들
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
    protected void onResume() {
        super.onResume();
        updateLoginButton();
    }

    private void updateLoginButton() {
        boolean isLoggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
        if (isLoggedIn) {
            String userId = sharedPreferences.getString(KEY_USER_ID, "");
            btnLogin.setText("로그아웃 (" + userId + ")");
        } else {
            btnLogin.setText("로그인");
        }
    }

    private void logout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        editor.putString(KEY_USER_ID, "");
        editor.apply();

        Toast.makeText(this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show();
        updateLoginButton();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 음악 종료
        stopService(new Intent(this, BackgroundMusicService.class));
        SharedPreferences.Editor editor = musicPreferences.edit();
        editor.putBoolean("isMusicPlaying", false);
        editor.apply();
    }
}
