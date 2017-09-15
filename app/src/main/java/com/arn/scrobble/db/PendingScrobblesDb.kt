package com.arn.scrobble.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context


/**
 * Created by arn on 11/09/2017.
 */

@Database(entities = arrayOf(PendingScrobble::class), version = 2)
abstract class PendingScrobblesDb : RoomDatabase() {
    abstract fun getDao(): PendingScrobblesDao

    companion object {
        val tableName = "pendingScrobbles"
        private var INSTANCE: PendingScrobblesDb? = null
        fun getDb(context: Context): PendingScrobblesDb {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(context.applicationContext, PendingScrobblesDb::class.java, tableName)
//                        .addMigrations(MIGRATION_1_2)
                        // allow queries on the main thread.
                        // Don't do this on a real app! See PersistenceBasicSample for an example.
//                        .allowMainThreadQueries()
                        .build()
            }
            return INSTANCE!!
        }

        fun destroyInstance() {
            INSTANCE = null
        }
/*
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {

                val TABLE_NAME_TEMP = "tmp"

                // 1. Create new table
                database.execSQL("CREATE TABLE IF NOT EXISTS `$TABLE_NAME_TEMP` " +
                        "(`game_name` TEXT NOT NULL, " +
                        "PRIMARY KEY(`game_name`))")

                // 2. Copy the data
                database.execSQL("INSERT INTO $TABLE_NAME_TEMP (game_name) "
                        + "SELECT game_name "
                        + "FROM $TABLE_NAME")

                // 3. Remove the old table
                database.execSQL("DROP TABLE $TABLE_NAME")

                // 4. Change the table name to the correct one
                database.execSQL("ALTER TABLE $TABLE_NAME_TEMP RENAME TO $TABLE_NAME")

            }
        }
        */
    }
}
