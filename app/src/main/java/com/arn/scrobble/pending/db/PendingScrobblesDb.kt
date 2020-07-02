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

@Database(entities = [PendingScrobble::class, PendingLove::class, Edit::class], version = 7)
abstract class PendingScrobblesDb : RoomDatabase() {
    abstract fun getScrobblesDao(): PendingScrobblesDao
    abstract fun getLovesDao(): PendingLovesDao
    abstract fun getEditsDao(): EditsDao

    companion object {
        const val fileName = "pendingScrobbles"
        private var INSTANCE: PendingScrobblesDb? = null
        fun getDb(context: Context): PendingScrobblesDb {
            if (INSTANCE == null || INSTANCE?.isOpen == false) {
                INSTANCE = Room.databaseBuilder(context.applicationContext, PendingScrobblesDb::class.java, fileName)
                        .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                        // allow queries on the main thread.
                        // Don't do this on a real app! See PersistenceBasicSample for an example.
                        // Just dont do this on a UI thread
                        .allowMainThreadQueries()
                        .build()
            }
            return INSTANCE!!
        }

        fun destroyInstance() {
            if (INSTANCE?.isOpen == true)
                INSTANCE?.close()
            INSTANCE = null
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
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
                database.execSQL("ALTER TABLE $fileName ADD albumArtist TEXT NOT NULL DEFAULT \"\"")

            }
        }
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val tableName = "pendingLoves"
                database.execSQL("CREATE TABLE IF NOT EXISTS $tableName (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `track` TEXT NOT NULL, `artist` TEXT NOT NULL, `shouldLove` INTEGER NOT NULL DEFAULT 1)")
            }
        }
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val tableName = "edits"
                database.execSQL("CREATE TABLE IF NOT EXISTS $tableName (`hash` TEXT PRIMARY KEY NOT NULL, `track` TEXT NOT NULL, `album` TEXT NOT NULL, `albumArtist` TEXT NOT NULL, `artist` TEXT NOT NULL)")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val tableName = "pendingLoves"
                database.execSQL("ALTER TABLE $tableName ADD state INTEGER NOT NULL DEFAULT 7") //111
                database.execSQL("ALTER TABLE $tableName ADD state_timestamp INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
                database.execSQL("UPDATE $fileName SET state = 31 WHERE state <= 0") //11111
            }
        }

    }
}
