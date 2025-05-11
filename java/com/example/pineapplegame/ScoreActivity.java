package com.example.pineapplegame;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ScoreActivity extends AppCompatActivity {

    private TextView tvScoresList;  // tvTopScores를 제거하고 tvScoresList만 사용
    private Button btnBackToMain;
    private ScoreDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_score);

        // UI 요소 초기화
        tvScoresList = findViewById(R.id.tvScoresList);
        btnBackToMain = findViewById(R.id.btnBackToMain);

        // 데이터베이스 헬퍼 객체 생성
        dbHelper = new ScoreDatabaseHelper(this);

        // 상위 10점수 불러오기
        loadTopScores();

        // 돌아가기 버튼 클릭 시 MainActivity로 이동
        btnBackToMain.setOnClickListener(v -> {
            Intent intent = new Intent(ScoreActivity.this, MainActivity.class);
            startActivity(intent);
            finish();  // 현재 ScoreActivity 종료
        });
    }

    private void loadTopScores() {
        // 점수 목록을 데이터베이스에서 조회
        Cursor cursor = dbHelper.getTopScores();

        if (cursor != null && cursor.moveToFirst()) {
            StringBuilder scoresList = new StringBuilder();
            int rank = 1;

            // "score" 컬럼의 인덱스를 가져오기 전에 정확한 컬럼명을 사용했는지 확인
            int scoreColumnIndex = cursor.getColumnIndex("score");

            // 컬럼 인덱스가 -1이 아니면 정상적으로 데이터를 처리
            if (scoreColumnIndex != -1) {
                do {
                    int score = cursor.getInt(scoreColumnIndex);
                    scoresList.append(rank).append(". ").append(score).append("\n");
                    rank++;
                } while (cursor.moveToNext());

                // 점수 목록을 TextView에 설정
                tvScoresList.setText(scoresList.toString());
            } else {
                tvScoresList.setText("Error: Column 'score' not found.");
            }

            cursor.close();
        } else {
            // 점수가 없을 경우
            tvScoresList.setText("No scores available.");
        }
    }

}