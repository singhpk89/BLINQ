package com.blinq.utils;

import android.content.ContentValues;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

/**
 * Database utilities.
 */
public class HeadboxDBUtils {

    private static final String TAG = HeadboxDBUtils.class.getSimpleName();

    public HeadboxDBUtils() {
    }

    /**
     * Generic bulk insert method.
     *
     * @param tableName - the table to insert on.
     * @param values    - array of values mapped to the table columns.
     */
    public static int bulkInsert(SQLiteDatabase db, String tableName,
                                 ContentValues[] values) {

        if (values != null && values.length > 0) {

            long startTime = System.currentTimeMillis();
            long endTime = 0;

            int insertedRows = 0;

            try {

                db.beginTransaction();

                for (ContentValues contentValues : values) {

                    long insertedId = 0;
                    try {
                        insertedId = db.insertOrThrow(tableName, null,
                                contentValues);
                        insertedRows++;
                    } catch (SQLException ignore) {
                        Log.e(TAG,
                                "Error while inserting record"
                                        + ignore.toString()
                        );
                    }
                }

                db.setTransactionSuccessful();

            } catch (Throwable ex) {
                Log.e(TAG, ex.getMessage(), ex);
            } finally {
                try {
                    db.endTransaction();
                } catch (SQLException ignore) {
                }
            }

            endTime = System.currentTimeMillis();
            long timeTaken = (endTime - startTime);
            Log.v(TAG, "Time taken to insert " + insertedRows + " In "
                    + tableName + " was " + timeTaken + " milliseconds");

            return insertedRows;
        }
        return 0;
    }

}
