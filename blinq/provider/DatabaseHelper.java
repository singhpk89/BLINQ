package com.blinq.provider;

import com.blinq.provider.HeadboxFeed.Feeds;
import com.blinq.provider.HeadboxFeed.Messages;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String MODIFIED_DATABASE_NAME = "blinq2.db";
    private static final int DATABASE_VERSION = 1;
    public static String dbPath = "";

    @Override
    public void onCreate(SQLiteDatabase db) {
        /* Your database is already Created, so no need to add anything here */
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        /* Database Not upgraded here */
    }

    private DatabaseHelper(Context context, String path) {

        super(context, path, null, DATABASE_VERSION);
    }

    public boolean checkDatabase() {

        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openDatabase(dbPath, null,
                    SQLiteDatabase.OPEN_READWRITE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (db == null) {
            return false;
        } else {
            db.close();
            return true;
        }
    }

    public static synchronized DatabaseHelper getDBHelper(Context context) {

        dbPath = context.getDatabasePath(DatabaseHelper.MODIFIED_DATABASE_NAME)
                .getAbsolutePath();

        return new DatabaseHelper(context, dbPath);
    }

    /**
     * Do some manipulations to hide the original records before sending the
     * database.
     */
    public void encryptDB() {

        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openDatabase(dbPath, null,
                    SQLiteDatabase.OPEN_READWRITE);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (db != null) {
            // Here we need to change the real records.
            db.execSQL("UPDATE " + HeadboxDatabaseHelper.TABLE_MESSAGES
                    + " SET " + Messages.BODY + " = " + Messages._ID);
            db.execSQL("UPDATE " + HeadboxDatabaseHelper.TABLE_FEEDS + " SET "
                    + Feeds.SNIPPET_TEXT + " = " + Feeds._ID);
            db.close();
        }
    }
}