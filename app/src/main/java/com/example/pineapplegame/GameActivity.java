package com.example.pineapplegame;

import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class GameActivity extends AppCompatActivity {

    private static final int GRID_SIZE = 10;
    private GridLayout gridLayout;
    private TextView[][] appleCells = new TextView[GRID_SIZE][GRID_SIZE];
    private Random random = new Random();

    private int cellSize = 100;

    private int startRow = -1, startCol = -1;

    // 타이머 및 점수 관련
    private TextView textTimer, textScore;
    private CountDownTimer countDownTimer;
    private int score = 0;
    private final long totalTime = 60 * 1000; // 60초

    private final int highlightColor = Color.parseColor("#A5D6A7");  // 연한 초록
    private final int defaultColor = Color.parseColor("#FFE066");   // 기본 사과 색
    private final int clearedColor = Color.parseColor("#DDDDDD");   // 제거된 색

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game); // ✅ 수정된 레이아웃 사용

        gridLayout = findViewById(R.id.gridLayout);
        textTimer = findViewById(R.id.textTimer);
        textScore = findViewById(R.id.textScore);

        createAppleGrid();
        setTouchListener();
        startTimer();
    }

    private void createAppleGrid() {
        gridLayout.removeAllViews();

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                TextView cell = new TextView(this);
                int value = random.nextInt(9) + 1;
                cell.setText(String.valueOf(value));
                cell.setGravity(Gravity.CENTER);
                cell.setTextSize(18);
                cell.setBackgroundColor(defaultColor);
                cell.setTextColor(Color.BLACK);

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
                    appleCells[row][col].setBackgroundColor(highlightColor);
                }
            }
        }
    }

    private void clearHighlight() {
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                String text = appleCells[row][col].getText().toString();
                if (!text.isEmpty()) {
                    appleCells[row][col].setBackgroundColor(defaultColor);
                } else {
                    appleCells[row][col].setBackgroundColor(clearedColor);
                }
            }
        }
    }

    private void setTouchListener() {
        gridLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                int x = (int) event.getX();
                int y = (int) event.getY();

                int col = x / (cellSize + 4);
                int row = y / (cellSize + 4);

                if (row >= GRID_SIZE || col >= GRID_SIZE || row < 0 || col < 0)
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
            for (int row = top; row <= bottom; row++) {
                for (int col = left; col <= right; col++) {
                    appleCells[row][col].setText("");
                    appleCells[row][col].setBackgroundColor(clearedColor);
                }
            }
            score++;
            textScore.setText("Score: " + score);
        }
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(totalTime, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                textTimer.setText("Time: " + millisUntilFinished / 1000);
            }

            @Override
            public void onFinish() {
                textTimer.setText("Time: 0");
                Toast.makeText(GameActivity.this, "⏰ Time's up! Final Score: " + score, Toast.LENGTH_LONG).show();
                gridLayout.setEnabled(false);
            }
        };
        countDownTimer.start();
    }
}
