package com.example.socketchat;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class UserDBHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "chatapp.db";
    public static final int DB_VERSION = 5;   // ★ 버전 5로 올림

    public static final String TABLE_USERS = "users";
    public static final String TABLE_CHAT_PARTNERS = "chat_partners";
    public static final String TABLE_MESSAGES = "messages";

    public UserDBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // (1) 예전 users 테이블 (지금은 거의 안 쓰지만 호환용)
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS " + TABLE_USERS + " (" +
                        "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "username TEXT," +
                        "password TEXT," +
                        "phone TEXT UNIQUE," +
                        "nickname TEXT" +
                        ");"
        );

        // (2) 대화 상대 목록용 테이블
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS " + TABLE_CHAT_PARTNERS + " (" +
                        "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "owner_phone TEXT NOT NULL," +   // ★ 로그인한 내 번호
                        "phone TEXT NOT NULL," +         // ★ 상대 번호
                        "nickname TEXT," +
                        "UNIQUE(owner_phone, phone)" +   // 같은 사람 중복 등록 방지
                        ");"
        );

        // (3) 대화 내용 저장용 메시지 테이블
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS " + TABLE_MESSAGES + " (" +
                        "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "my_phone TEXT NOT NULL," +
                        "partner_phone TEXT NOT NULL," +
                        "sender_phone TEXT NOT NULL," +
                        "content TEXT NOT NULL," +
                        "created_at INTEGER NOT NULL" +
                        ");"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 개발 단계니까 그냥 싹 갈아엎기
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHAT_PARTNERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }
}
