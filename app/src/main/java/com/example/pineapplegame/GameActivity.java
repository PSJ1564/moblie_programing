package com.example.pineapplegame;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioAttributes;
import android.media.SoundPool;
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
    private Button btnHint; //힌트
    private int hintCount = 3; // 힌트
    private int destroyCount = 3;
    private int swapCount = 3;
    private enum Mode { NORMAL, DESTROY, SWAP }
    private Mode currentMode = Mode.NORMAL;
    private boolean isDestroyedMode = false;
    private boolean isSwapMode = false;
    private boolean isFirstSwapSelected = false;
    private boolean running = true;
    private boolean combo = false;
    private int firstSwapRow, firstSwapCol;
    private Button btnPause, btnReturn, btnDestroy, btnSwap;
    private SoundPool soundPool;
    private int soundExplosion, soundDragId;
    private long lastPlayedTime = 0;
    private int prevSelectedCount = -1;
    private boolean isExplosionInProgress = false;

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
        btnDestroy = findViewById(R.id.btnDestroy);
        btnSwap = findViewById(R.id.btnSwap);
        btnHint = findViewById(R.id.btnHint); // 힌트 연결
        updateHintButtonText();

        btnHint.setOnClickListener(v -> {
            if (hintCount > 0) {
                hintCount--;
                updateHintButtonText();
                showHint(); // 힌트 표시 함수 호출
                if (hintCount == 0) {
                    Toast.makeText(this, "💡 힌트 모두 사용!", Toast.LENGTH_SHORT).show();
                }
            }
        });
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
        setupItemButton();
        createAppleGrid();
        setTouchListener();
        startTimer();
        SharedPreferences prefs = getSharedPreferences("game_prefs", MODE_PRIVATE);
        boolean isItemMode = prefs.getBoolean("item_mode", false);
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(audioAttributes)
                .build();
        soundExplosion = soundPool.load(this, R.raw.low_pop, 1);
        soundDragId = soundPool.load(this, R.raw.drag, 1);

        if (!isItemMode) {
            // 아이템 UI만 숨김
            btnDestroy.setVisibility(View.GONE);
            btnSwap.setVisibility(View.GONE);
            btnHint.setVisibility(View.GONE);
        }
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
        if (isExplosionInProgress) return;

        clearHighlight();

        int top = Math.min(r1, r2);
        int bottom = Math.max(r1, r2);
        int left = Math.min(c1, c2);
        int right = Math.max(c1, c2);

        int selectedCount = 0;

        for (int row = top; row <= bottom; row++) {
            for (int col = left; col <= right; col++) {
                if (!appleCells[row][col].getText().toString().isEmpty()) {
                    appleCells[row][col].setBackgroundResource(R.drawable.pineapple_gridselect);
                    selectedCount++;
                }
            }
        }

        SharedPreferences prefs = getSharedPreferences("MusicPrefs", MODE_PRIVATE);
        if (prefs.getBoolean("isSoundEnabled", true) && selectedCount != prevSelectedCount) {
            float pitch = Math.min(2.5f, 1.5f + 0.1f * (Math.abs(r2 - r1) + Math.abs(c2 - c1) + 1));
            soundPool.play(soundDragId, 1f, 1f, 0, 0, pitch);
            prevSelectedCount = selectedCount;
        }
    }


    private void clearHighlight() {
        if (isExplosionInProgress) return;
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

                int col = (int)(gridX / (cellWidth-4));
                int row = (int)(gridY / (cellHeight-4));

                Log.d("TouchDebug", "gridLayout H: " + gridLayout.getHeight() + ", W: " + gridLayout.getWidth());
                Log.d("TouchDebug", "TouchX: " + gridX + ", TouchY: " + gridY);
                Log.d("TouchDebug", "Computed Row: " + row + ", Col: " + col);
                if (row >= GRID_ROWS || col >= GRID_COLS || row < 0 || col < 0)
                    return false;

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startRow = row;
                        startCol = col;
                        if (isDestroyedMode) {
                            destroySelectedBlock(row, col);
                            isDestroyedMode = false;
                        } else if (isSwapMode) {
                            handleSwapBlock(row, col);
                        }
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

                    // ⏱ 2초 추가
                    remainingTime += 2000;

                    // ✅ 즉시 UI 동기화 (중요!)
                    long seconds = remainingTime / 1000;
                    textTimer.setText("Time: " + seconds);

                    // 🔁 타이머 재시작
                    if (countDownTimer != null) {
                        countDownTimer.cancel();
                    }
                    resumeTimer();
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
                scoreOverlay.setTranslationY(-1000);
                scoreOverlay.animate()
                        .translationY(0)
                        .setDuration(600)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
                btnReturn.setVisibility(View.VISIBLE);
                btnPause.setVisibility(View.GONE);
                btnDestroy.setVisibility(View.GONE);
                btnSwap.setVisibility(View.GONE);
                btnHint.setVisibility(View.GONE);
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
                scoreOverlay.setTranslationY(-1000);
                scoreOverlay.animate()
                        .translationY(0)
                        .setDuration(600)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
                btnReturn.setVisibility(View.VISIBLE);
                btnPause.setVisibility(View.GONE);
                btnDestroy.setVisibility(View.GONE);
                btnSwap.setVisibility(View.GONE);
                btnHint.setVisibility(View.GONE);
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

    private void setupItemButton() {
        btnDestroy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(destroyCount > 0) {
                    isDestroyedMode = true;
                    Toast.makeText(GameActivity.this, "🧨블록 제거 아이템 사용: 제거할 블록을 선택하세요!", Toast.LENGTH_SHORT).show();
                    //Snackbar.make(root, "한순간만!", Snackbar.LENGTH_SHORT).setDuration(500).show(); //Tag:Suggest 제안사항 Toast message가 너무 긴 것 같으므로 시간을 조절할 수 있는 snackbar 제안
                } else {
                    Toast.makeText(GameActivity.this, "❌블록 제거 아이템 없음.", Toast.LENGTH_SHORT).show();
                    btnDestroy.setVisibility(View.INVISIBLE);
                }
            }
        });

        btnSwap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(swapCount > 0) {
                    isSwapMode = true;
                    isFirstSwapSelected = false;
                    Toast.makeText(GameActivity.this, "🔄블록 교환 아이템 사용: 첫 번째 블록을 선택하세요!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(GameActivity.this, "❌블록 교환 아이템 없음.", Toast.LENGTH_SHORT).show();
                    btnSwap.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    private void destroySelectedBlock(int row, int col) {
        if (isExplosionInProgress) return; // 중복 방지

        isExplosionInProgress = true;
        appleCells[row][col].setBackgroundResource(R.drawable.explosion_frame);  // 애니메이션 대신 단일 이미지
        appleCells[row][col].setText("");
        destroyCount--;

        SharedPreferences prefs = getSharedPreferences("MusicPrefs", MODE_PRIVATE);
        boolean isSoundEnabled = prefs.getBoolean("isSoundEnabled", true);
        if (isSoundEnabled) {
            soundPool.play(soundExplosion, 1f, 1f, 0, 0, 1.0f);
        }

        new Handler().postDelayed(() -> {
            appleCells[row][col].setBackgroundResource(R.drawable.pineapple_griddestroy);
            isExplosionInProgress = false;
        }, 500); // 애니메이션 종료 후 플래그 해제

        Toast.makeText(GameActivity.this, "💥블록 제거!" + destroyCount + "개 남음", Toast.LENGTH_SHORT).show();
    }


    private void handleSwapBlock(int row, int col) {
        if(!isFirstSwapSelected) {
            firstSwapRow = row;
            firstSwapCol = col;
            isFirstSwapSelected = true;

            appleCells[row][col].setBackgroundResource(R.drawable.pineapple_gridselect);

        } else {

            appleCells[row][col].setBackgroundResource(R.drawable.pineapple_gridselect);

            CharSequence temp = appleCells[row][col].getText();
            appleCells[row][col].setText(appleCells[firstSwapRow][firstSwapCol].getText());
            appleCells[firstSwapRow][firstSwapCol].setText(temp);

            swapCount--;
            Toast.makeText(GameActivity.this, "🔄블록 교환 완료!" + swapCount + "개 남음", Toast.LENGTH_SHORT).show();

            new Handler().postDelayed(() -> {
                appleCells[firstSwapRow][firstSwapCol].setBackgroundResource(R.drawable.pineapple_grid);
                appleCells[row][col].setBackgroundResource(R.drawable.pineapple_grid);
            }, 500);

            isFirstSwapSelected = false;
            isSwapMode = false;
        }
    }

    private void updateHintButtonText() {
        if (hintCount > 0) {
            btnHint.setText("힌트 (" + hintCount + "/3)");
        } else {
            btnHint.setText("힌트 사용 불가");
            btnHint.setEnabled(false);
            btnHint.setBackgroundColor(Color.GRAY); // 선택사항: 비활성화 느낌
        }
    }

    private void showHint() {
        clearHighlight(); // 기존 하이라이트 제거

        for (int r1 = 0; r1 < GRID_ROWS; r1++) {
            for (int c1 = 0; c1 < GRID_COLS; c1++) {
                for (int r2 = r1; r2 < GRID_ROWS; r2++) {
                    for (int c2 = c1; c2 < GRID_COLS; c2++) {

                        int sum = 0;
                        for (int row = r1; row <= r2; row++) {
                            for (int col = c1; col <= c2; col++) {
                                String text = appleCells[row][col].getText().toString();
                                if (!text.isEmpty()) {
                                    sum += Integer.parseInt(text);
                                }
                            }
                        }

                        if (sum == 10) {
                            // 금색 하이라이트로 표시
                            for (int row = r1; row <= r2; row++) {
                                for (int col = c1; col <= c2; col++) {
                                    appleCells[row][col].setBackgroundColor(Color.parseColor("#FF0000"));
                                }
                            }
                            Toast.makeText(this, "🔍 합이 10인 조합 발견!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                }
            }
        }

        Toast.makeText(this, "❌ 가능한 조합이 없습니다!", Toast.LENGTH_SHORT).show();
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
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }

}