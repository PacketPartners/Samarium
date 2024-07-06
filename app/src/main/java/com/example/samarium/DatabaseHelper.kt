package com.example.samarium

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "cellinfo.db"
        const val DATABASE_VERSION = 2  // Increase version due to schema change
        const val TABLE_NAME = "cellinfo"
        const val COLUMN_ID = "id"
        const val COLUMN_EVENT_TIME = "event_time"
        const val COLUMN_PLMN_ID = "plmn_id"
        const val COLUMN_TAC = "tac"
        const val COLUMN_CELL_ID = "cell_id"
        const val COLUMN_RSRP = "rsrp"
        const val COLUMN_RSRQ = "rsrq"
        const val COLUMN_TECHNOLOGY = "technology"
        const val COLUMN_LATITUDE = "latitude"  // New column for latitude
        const val COLUMN_LONGITUDE = "longitude" // New column for longitude
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = "CREATE TABLE $TABLE_NAME (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_EVENT_TIME TEXT, " +
                "$COLUMN_PLMN_ID TEXT, " +
                "$COLUMN_TAC INTEGER, " +
                "$COLUMN_CELL_ID INTEGER, " +
                "$COLUMN_RSRP TEXT, " +
                "$COLUMN_RSRQ TEXT, " +
                "$COLUMN_TECHNOLOGY TEXT, " +
                "$COLUMN_LATITUDE REAL, " +        // Define latitude column
                "$COLUMN_LONGITUDE REAL)"         // Define longitude column
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }
}
