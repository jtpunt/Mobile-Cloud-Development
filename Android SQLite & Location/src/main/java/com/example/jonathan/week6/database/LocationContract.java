package com.example.jonathan.week6.database;

import android.provider.BaseColumns;

public final class LocationContract {
    private LocationContract(){}; // To prevent someone from accidentally instantiating the contract class, make the constructor private

    // Inner class that defines the table contents
    public static class LocationEntry implements BaseColumns {
        public static final String TABLE_NAME = "location";
        public static final String COLUMN_LATITUDE = "latitude";
        public static final String COLUMN_LONGITUDE = "longitude";
        public static final String COLUMN_USER_INPUT = "user_input";
    }
}
