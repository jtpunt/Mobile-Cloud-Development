package com.example.jonathan.week6.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.jonathan.week6.database.LocationContract.*;

public class LocationDBHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "location.db";

    public static final String SQL_CREATE_ENTRIES = "CREATE TABLE " + LocationContract.LocationEntry.TABLE_NAME + "(" +
            LocationContract.LocationEntry._ID + " INTEGER PRIMARY KEY NOT NULL," +
            LocationContract.LocationEntry.COLUMN_LATITUDE + " FLOAT NOT NULL, " +
            LocationContract.LocationEntry.COLUMN_LONGITUDE + " FLOAT NOT NULL, "  +
            LocationContract.LocationEntry.COLUMN_USER_INPUT + " VARCHAR(255));";

    public static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + LocationContract.LocationEntry.TABLE_NAME;

    public LocationDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
