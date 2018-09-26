package com.arn.scrobble.pending.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase


/**
 * Created by arn on 11/09/2017.
 */

@Database(entities = [(PendingScrobble::class)], version = 3)
abstract class PendingScrobblesDb : RoomDatabase() {
    abstract fun getDao(): PendingScrobblesDao

    companion object {
        const val tableName = "pendingScrobbles"
        private var INSTANCE: PendingScrobblesDb? = null
        fun getDb(context: Context): PendingScrobblesDb {
            if (INSTANCE == null || INSTANCE?.isOpen == false) {
                INSTANCE = Room.databaseBuilder(context.applicationContext, PendingScrobblesDb::class.java, tableName)
                        .addMigrations(MIGRATION_2_3)
                        // allow queries on the main thread.
                        // Don't do this on a real app! See PersistenceBasicSample for an example.
//                        .allowMainThreadQueries()
                        .build()
            }
            return INSTANCE!!
        }

        fun destroyInstance() {
            if (INSTANCE?.isOpen == true)
                INSTANCE?.close()
            INSTANCE = null
        }

        private val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
/*
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
                */
                database.execSQL("ALTER TABLE $tableName ADD album TEXT NOT NULL DEFAULT \"\"")

            }
        }

    }
}
