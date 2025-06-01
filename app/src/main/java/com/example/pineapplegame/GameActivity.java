package com.example.pineapplegame;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class GameActivity extends AppCompatActivity {
    private static final int GRID_ROWS = 12;
    private static final int GRID_COLS = 8;
    private GridLayout gridLayout;
    private LinearLayout state;
    private FrameLayout root;//id:main rootview임
    private TextView[][] appleCells = new TextView[GRID_ROWS][GRID_COLS];
    private Random random = new Random();
    private View scoreOverlay;
    private TextView textFinalScore;

    private int cellSize = 110;

    private int startRow = -1, startCol = -1;

    // 타이머 및 점수 관련
    private TextView textTimer, textScore;
    private CountDownTimer countDownTimer, comboTimer;
    private int score = 0;
    private int comboScore = 0;
    private long remainingTime = 0;
    private final long totalTime = 60 * 1000; // 120초
    private boolean running = true;
    private boolean combo = false;
    private Button btnPause, btnReturn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game); // ✅ 수정된 레이아웃 사용
        root = findViewById(R.id.root);
        state = findViewById(R.id.state);
        gridLayout = findViewById(R.id.gridLayout);
        textTimer = findViewById(R.id.textTimer);
        textScore = findViewById(R.id.textScore);
        btnPause = findViewById(R.id.btnPause);
        btnReturn = findViewById(R.id.btnReturn);
        scoreOverlay = findViewById(R.id.scoreOverlay);
        textFinalScore = findViewById(R.id.textFinalScore);

        btnPause.setOnClickListener(v -> {
            if(running) {
                pauseTimer();
                btnPause.setText("재시작");
                btnReturn.setVisibility(View.VISIBLE);
            }
            else {
                resumeTimer();
                btnPause.setText("일시정지");
                btnReturn.setVisibility(View.INVISIBLE);
            }

        });

        setupReturnButton();

        createAppleGrid();
        setTouchListener();
        startTimer();
    }
    private void createAppleGrid() {
        gridLayout.removeAllViews();

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                TextView cell = new TextView(this);
                int value = random.nextInt(9) + 1;
                cell.setText(String.valueOf(value));
                cell.setGravity(Gravity.CENTER);
                cell.setTextSize(18);
                cell.setTextColor(Color.BLACK);
                cell.setBackgroundResource(R.drawable.pineapple_grid);
                cell.setPadding(0,20,0,0);

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = cellSize;
                params.height = cellSize;
                params.rowSpec = GridLayout.spec(row);
                params.columnSpec = GridLayout.spec(col);
                params.setMargins(4, 4, 4, 4);

                cell.setLayoutParams(params);
                gridLayout.addView(cell);
                appleCells[row][col] = cell;
            }
        }
    }

    private void highlightSelection(int r1, int c1, int r2, int c2) {
        clearHighlight();

        int top = Math.min(r1, r2);
        int bottom = Math.max(r1, r2);
        int left = Math.min(c1, c2);
        int right = Math.max(c1, c2);

        for (int row = top; row <= bottom; row++) {
            for (int col = left; col <= right; col++) {
                if (!appleCells[row][col].getText().toString().isEmpty()) {
                    appleCells[row][col].setBackgroundResource(R.drawable.pineapple_gridselect);
                }
            }
        }
    }

    private void clearHighlight() {
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                String text = appleCells[row][col].getText().toString();
                if (!text.isEmpty()) {
                    appleCells[row][col].setBackgroundResource(R.drawable.pineapple_grid);
                } else {
                    appleCells[row][col].setBackgroundResource(R.drawable.pineapple_griddestroy);
                }
            }
        }
    }

    private void setTouchListener() {
        gridLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                int[] gridLocation = new int[2];
                gridLayout.getLocationOnScreen(gridLocation);

                int gridX = (int) event.getRawX() - gridLocation[0];
                int gridY = (int) event.getRawY() - gridLocation[1];

                int gridWidth = gridLayout.getWidth();
                int gridHeight = gridLayout.getHeight();
                int cellWidth = gridWidth / GRID_COLS;
                int cellHeight = gridHeight / GRID_ROWS;

                int col = (int)((gridX) / cellWidth);
                int row = (int)((gridY*1.2f) / cellHeight);

                Log.d("TouchDebug", "gridLayout H: " + gridLayout.getHeight() + ", W: " + gridLayout.getWidth());
                Log.d("TouchDebug", "TouchX: " + gridX + ", TouchY: " + gridY);
                Log.d("TouchDebug", "Computed Row: " + row + ", Col: " + col);

                if (row >= GRID_ROWS || col >= GRID_COLS || row < 0 || col < 0)
                    return false;

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startRow = row;
                        startCol = col;
                        break;

                    case MotionEvent.ACTION_MOVE:
                        highlightSelection(startRow, startCol, row, col);
                        break;

                    case MotionEvent.ACTION_UP:
                        int endRow = row;
                        int endCol = col;
                        clearHighlight();
                        checkAndClearGroup(startRow, startCol, endRow, endCol);
                        break;
                }
                return true;
            }
        });

    }

    private void checkAndClearGroup(int r1, int c1, int r2, int c2) {
        int top = Math.min(r1, r2);
        int bottom = Math.max(r1, r2);
        int left = Math.min(c1, c2);
        int right = Math.max(c1, c2);

        int sum = 0;
        for (int row = top; row <= bottom; row++) {
            for (int col = left; col <= right; col++) {
                String text = appleCells[row][col].getText().toString();
                if (!text.isEmpty()) {
                    sum += Integer.parseInt(text);
                }
            }
        }

        if (sum == 10) {
            combo = true;
            for (int row = top; row <= bottom; row++) {
                for (int col = left; col <= right; col++) {
                    appleCells[row][col].setText("");
                    appleCells[row][col].setBackgroundResource(R.drawable.pineapple_griddestroy);
                }
            }
            startComboTimer();
            if(combo) {
                comboScore++;
                score += comboScore;
                textScore.setText("Score: " + score);
                if(comboScore > 1) {
                    Toast.makeText(this, "🔥콤보! x" + comboScore, Toast.LENGTH_SHORT).show();
                }
            }
            else {
                comboScore = 0;
                score++;
                textScore.setText("Score: " + score);
            }
        }
        else {
            combo = false;
            comboScore = 0;
        }
    }

    private void startComboTimer() {
        if (comboTimer != null) {
            comboTimer.cancel();
        }

        combo = true;
        comboTimer = new CountDownTimer(3000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // 콤보 타이머는 UI에 표시하지 않음
            }

            @Override
            public void onFinish() {
                combo = false;
                comboScore = 0;
            }
        };
        comboTimer.start();
    }
    private void startTimer() {
        running = true;
        remainingTime = totalTime;
        countDownTimer = new CountDownTimer(totalTime, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingTime = millisUntilFinished;
                textTimer.setText("Time: " + millisUntilFinished / 1000);
                int color = 0xffd1f06a; //시간에 따른 상태표시줄색상변경
                color = (int) ((color & 0xFF000000) |
                        (Math.min(255, ((color >> 16) & 0xFF) + 2*(60-millisUntilFinished/1000)) << 16) |
                        (Math.max(0, ((color >> 8) & 0xFF) - (60-millisUntilFinished/1000)) << 8) |
                        (Math.max(0, (color & 0xFF) - 2*(60-millisUntilFinished/1000))));
                state.setBackgroundColor(color);
            }

            @Override
            public void onFinish() {
                textTimer.setText("Time: 0");
                gridLayout.setEnabled(false);

                textFinalScore.setText("Score: " + score);
                scoreOverlay.setVisibility(View.VISIBLE);//게임종료에 따른 점수스크린
                scoreOverlay.setBackgroundColor(Color.parseColor("#AAFFFFFF"));
                textFinalScore.setTextColor(Color.BLACK);
                scoreOverlay.setTranslationY(-700);
                scoreOverlay.animate()
                        .translationY(0)
                        .setDuration(600)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
                btnReturn.setVisibility(View.VISIBLE);
                btnPause.setVisibility(View.GONE);
                gridLayout.setEnabled(false);
                running = false;
                // 점수 저장
                ScoreDatabaseHelper dbHelper = new ScoreDatabaseHelper(GameActivity.this);
                dbHelper.addScore(score);
                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                String nickname = prefs.getString("nickname", "Noname"); // 기본값은 "Noname"
                Log.d("ScoreUpload", "Uploading score. Nickname: " + nickname + ", Score: " + score);
                uploadScoreWithLimit(nickname, score);
            }
        };
        countDownTimer.start();
    }
    private void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            running = false;
        }
    }
    private void resumeTimer() {
        running = true;
        countDownTimer = new CountDownTimer(remainingTime, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingTime = millisUntilFinished;
                textTimer.setText("Time: " + millisUntilFinished / 1000);
            }

            @Override
            public void onFinish() {
                textTimer.setText("Time: 0");
                gridLayout.setEnabled(false);

                textFinalScore.setText("Score: " + score);
                scoreOverlay.setVisibility(View.VISIBLE);//게임종료에 따른 점수스크린
                scoreOverlay.setBackgroundColor(Color.parseColor("#AAFFFFFF"));
                textFinalScore.setTextColor(Color.BLACK);
                scoreOverlay.setTranslationY(-700);
                scoreOverlay.animate()
                        .translationY(0)
                        .setDuration(600)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
                btnReturn.setVisibility(View.VISIBLE);
                btnPause.setVisibility(View.GONE);
                gridLayout.setEnabled(false);
                running = false;
                // 점수 저장
                ScoreDatabaseHelper dbHelper = new ScoreDatabaseHelper(GameActivity.this);
                dbHelper.addScore(score);
                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                String nickname = prefs.getString("nickname", "Noname"); // 기본값은 "Noname"
                uploadScoreWithLimit(nickname, score);
            }
        };
        countDownTimer.start();
    }
    private void setupReturnButton() {
        Button btnReturn = findViewById(R.id.btnReturn);
        btnReturn.setVisibility(View.INVISIBLE);
        btnReturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
    private void uploadScoreWithLimit(String nickname, int newScore) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("rankings");

        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<DataSnapshot> entries = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    entries.add(child);
                }

                // 10개 미만이면 그냥 저장
                if (entries.size() < 10) {
                    saveScore(dbRef, nickname, newScore);
                } else {
                    // 최저 점수 찾기
                    DataSnapshot lowestSnapshot = null;
                    int minScore = Integer.MAX_VALUE;

                    for (DataSnapshot entry : entries) {
                        Integer score = entry.child("score").getValue(Integer.class);
                        if (score != null && score < minScore) {
                            minScore = score;
                            lowestSnapshot = entry;
                        }
                    }

                    // 새 점수가 더 크면 → 최저점 제거 후 저장
                    if (newScore > minScore && lowestSnapshot != null) {
                        lowestSnapshot.getRef().removeValue()
                                .addOnSuccessListener(aVoid -> saveScore(dbRef, nickname, newScore));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(GameActivity.this, "🔥 랭킹 업로드 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void saveScore(DatabaseReference dbRef, String nickname, int score) {
        String key = UUID.randomUUID().toString();

        Map<String, Object> data = new HashMap<>();
        data.put("nickname", nickname);
        data.put("score", score);
        data.put("timestamp", System.currentTimeMillis());

        dbRef.child(key).setValue(data)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(GameActivity.this, "✅ 점수 저장 완료!", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(GameActivity.this, "❌ 저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

}