package com.example.pineapplegame;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ScoreDatabaseHelper extends SQLiteOpenHelper {

    // 데이터베이스 이름과 버전
    private static final String DATABASE_NAME = "high_scores.db";
    private static final int DATABASE_VERSION = 1;

    // 테이블 이름과 컬럼 정의
    private static final String TABLE_NAME = "high_scores";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_SCORE = "score";

    // 테이블 생성 SQL문
    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_SCORE + " INTEGER NOT NULL"
            + ");";

    // 생성자
    public ScoreDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // 데이터베이스가 처음 생성될 때 호출되는 메소드
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);  // 테이블 생성 SQL 실행
    }

    // 데이터베이스 버전이 변경될 때 호출되는 메소드
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);  // 기존 테이블 삭제
        onCreate(db);  // 새로운 테이블 생성
    }

    // 점수 추가 메소드
    public void addScore(int score) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SCORE, score);

        // 점수 삽입
        db.insert(TABLE_NAME, null, values);

        // 상위 10개만 유지하기 위해 나머지 삭제
        db.execSQL("DELETE FROM " + TABLE_NAME +
                " WHERE " + COLUMN_ID + " NOT IN (SELECT " + COLUMN_ID +
                " FROM " + TABLE_NAME + " ORDER BY " + COLUMN_SCORE + " DESC LIMIT 10)");

        db.close();
    }

    // 상위 10개의 점수를 조회하는 메소드
    public Cursor getTopScores() {
        SQLiteDatabase db = this.getReadableDatabase();  // 읽기 가능한 데이터베이스 열기
        return db.query(TABLE_NAME, new String[]{COLUMN_SCORE}, null, null, null, null,
                COLUMN_SCORE + " DESC", "10");  // 점수를 내림차순으로 정렬하고, 상위 10개 조회
    }
}
