package com.example.geomemo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class MemoDBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "geomemo.db";
    private static final int DATABASE_VERSION = 2;

    private static final String TABLE_MEMOS = "memos";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_TEXT = "text";
    private static final String COLUMN_LATITUDE = "latitude";
    private static final String COLUMN_LONGITUDE = "longitude";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_POINTER_ICON = "pointer_icon";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_MEMOS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_TITLE + " TEXT NOT NULL, " +
                    COLUMN_TEXT + " TEXT, " +
                    COLUMN_LATITUDE + " REAL NOT NULL, " +
                    COLUMN_LONGITUDE + " REAL NOT NULL, " +
                    COLUMN_CREATED_AT + " TEXT, " +
                    COLUMN_POINTER_ICON + " TEXT DEFAULT 'pointer_1'" +
                    ");";

    private static MemoDBHelper instance;

    public static synchronized MemoDBHelper getInstance(Context context) {
        if (instance == null) {
            instance = new MemoDBHelper(context.getApplicationContext());
        }
        return instance;
    }

    public MemoDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_MEMOS + " ADD COLUMN " + COLUMN_POINTER_ICON + " TEXT DEFAULT 'pointer_1'");
        }
    }

    public long insertMemo(Memo memo) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, memo.getTitle());
        values.put(COLUMN_TEXT, memo.getText());
        values.put(COLUMN_LATITUDE, memo.getLatitude());
        values.put(COLUMN_LONGITUDE, memo.getLongitude());
        values.put(COLUMN_CREATED_AT, memo.getCreatedAt());
        values.put(COLUMN_POINTER_ICON, memo.getPointerIcon());
        return db.insert(TABLE_MEMOS, null, values);
    }

    public List<Memo> getAllMemos() {
        List<Memo> memos = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_MEMOS,
                null,
                null,
                null,
                null,
                null,
                COLUMN_CREATED_AT + " DESC"
        );
        try {
            if (cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndexOrThrow(COLUMN_ID);
                int titleIndex = cursor.getColumnIndexOrThrow(COLUMN_TITLE);
                int textIndex = cursor.getColumnIndexOrThrow(COLUMN_TEXT);
                int latIndex = cursor.getColumnIndexOrThrow(COLUMN_LATITUDE);
                int lngIndex = cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE);
                int createdAtIndex = cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT);
                int pointerIconIndex = cursor.getColumnIndex(COLUMN_POINTER_ICON);

                do {
                    Memo memo = new Memo();
                    memo.setId(cursor.getLong(idIndex));
                    memo.setTitle(cursor.getString(titleIndex));
                    memo.setText(cursor.getString(textIndex));
                    memo.setLatitude(cursor.getDouble(latIndex));
                    memo.setLongitude(cursor.getDouble(lngIndex));
                    memo.setCreatedAt(cursor.getString(createdAtIndex));

                    String pointerIcon = "pointer_1";
                    if (pointerIconIndex != -1) {
                        pointerIcon = cursor.getString(pointerIconIndex);
                        if (pointerIcon == null) {
                            pointerIcon = "pointer_1";
                        }
                    }
                    memo.setPointerIcon(pointerIcon);

                    memos.add(memo);
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        return memos;
    }

    public int deleteMemo(long id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_MEMOS, COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)});
    }

    public int updateMemo(Memo memo) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, memo.getTitle());
        values.put(COLUMN_TEXT, memo.getText());
        values.put(COLUMN_LATITUDE, memo.getLatitude());
        values.put(COLUMN_LONGITUDE, memo.getLongitude());
        values.put(COLUMN_CREATED_AT, memo.getCreatedAt());
        values.put(COLUMN_POINTER_ICON, memo.getPointerIcon());
        return db.update(TABLE_MEMOS, values, COLUMN_ID + " = ?",
                new String[]{String.valueOf(memo.getId())});
    }

    public Memo getMemoById(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_MEMOS,
                null,
                COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)},
                null,
                null,
                null
        );
        Memo memo = null;
        try {
            if (cursor.moveToFirst()) {
                memo = new Memo();
                memo.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                memo.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)));
                memo.setText(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEXT)));
                memo.setLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUDE)));
                memo.setLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE)));
                memo.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)));

                int pointerIconIndex = cursor.getColumnIndex(COLUMN_POINTER_ICON);
                String pointerIcon = "pointer_1";
                if (pointerIconIndex != -1) {
                    pointerIcon = cursor.getString(pointerIconIndex);
                    if (pointerIcon == null) {
                        pointerIcon = "pointer_1";
                    }
                }
                memo.setPointerIcon(pointerIcon);
            }
        } finally {
            cursor.close();
        }
        return memo;
    }
}
