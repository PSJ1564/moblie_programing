package com.example.pineapplegame;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
public class TiledBackgroundView extends View {

    private Bitmap patternBitmap;      // 한번만 생성될 패턴 전체 이미지
    private float offsetX = 0;         // 이동 위치
    private static final int TILE_SIZE = 130;
    private static final int PATTERN_WIDTH = 650; // 필요에 따라 조절
    private static final int PATTERN_HEIGHT = 2200;

    private Paint paint = new Paint();

    public TiledBackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
        generatePatternBitmap();
    }

    private void generatePatternBitmap() {
        patternBitmap = Bitmap.createBitmap(PATTERN_WIDTH, PATTERN_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(patternBitmap);

        Bitmap tile = BitmapFactory.decodeResource(getResources(), R.drawable.pineapple_bg);
        Bitmap scaled = Bitmap.createScaledBitmap(tile, TILE_SIZE, TILE_SIZE, true);

        int cols = PATTERN_WIDTH / TILE_SIZE;
        int rows = PATTERN_HEIGHT / TILE_SIZE;

        int index = 0;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (index == 0) {
                    float x = col * TILE_SIZE;
                    float y = row * TILE_SIZE;
                    canvas.drawBitmap(scaled, x, y, paint);
                    index = 3;
                }
                index--;
            }
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 패턴 전체를 왼쪽으로 이동하며 반복 그리기
        int drawCount = getWidth() / patternBitmap.getWidth() + 2;

        for (int i = 0; i < drawCount; i++) {
            float x = i * patternBitmap.getWidth() + offsetX;
            canvas.drawBitmap(patternBitmap, x, 0, paint);
        }

        offsetX -= 1f;

        // 한 주기만큼 이동되면 초기화
        if (offsetX <= -patternBitmap.getWidth()) {
            offsetX += patternBitmap.getWidth();
        }

        invalidate(); // 애니메이션 반복
    }

}
