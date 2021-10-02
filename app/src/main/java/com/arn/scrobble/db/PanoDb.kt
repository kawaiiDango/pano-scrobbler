package com.arn.scrobble.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase


/**
 * Created by arn on 11/09/2017.
 */

@Database(
    entities = [
        PendingScrobble::class,
        PendingLove::class,
        SimpleEdit::class,
        RegexEdit::class,
        BlockedMetadata::class,
        TrackedPlayer::class,
    ], version = 10
)
abstract class PanoDb : RoomDatabase() {
    abstract fun getScrobblesDao(): PendingScrobblesDao
    abstract fun getLovesDao(): PendingLovesDao
    abstract fun getSimpleEditsDao(): SimpleEditsDao
    abstract fun getRegexEditsDao(): RegexEditsDao
    abstract fun getBlockedMetadataDao(): BlockedMetadataDao
    abstract fun getTrackedPlayerDao(): TrackedPlayerDao

    companion object {
        const val fileName = "pendingScrobbles"
        private var INSTANCE: PanoDb? = null
        fun getDb(context: Context): PanoDb {
            if (INSTANCE == null || INSTANCE?.isOpen == false) {
                INSTANCE =
                    Room.databaseBuilder(context.applicationContext, PanoDb::class.java, fileName)
                        .addMigrations(
                            MIGRATION_3_4,
                            MIGRATION_4_5,
                            MIGRATION_5_6,
                            MIGRATION_6_7,
                            MIGRATION_7_8,
                            MIGRATION_8_9,
                            MIGRATION_9_10,
                        )
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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
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

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val tableName = "edits"
                database.execSQL("ALTER TABLE $tableName RENAME TO edits2")
                database.execSQL("CREATE TABLE $tableName (`legacyHash` TEXT, `origTrack` TEXT NOT NULL, `origAlbum` TEXT NOT NULL, `origArtist` TEXT NOT NULL, `track` TEXT NOT NULL, `album` TEXT NOT NULL, `albumArtist` TEXT NOT NULL, `artist` TEXT NOT NULL, PRIMARY KEY (origArtist, origAlbum, origTrack))")
                database.execSQL("CREATE INDEX legacyIdx ON $tableName (legacyHash)")
                database.execSQL("INSERT INTO $tableName SELECT hash, hash, hash, hash, track, album, albumArtist, artist FROM edits2")
                database.execSQL("DROP TABLE edits2")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                var tableName = "regexEdits"
                database.execSQL("CREATE TABLE IF NOT EXISTS $tableName (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `order` INTEGER NOT NULL, `preset` TEXT, `name` TEXT, `pattern` TEXT, `replacement` TEXT NOT NULL, `field` TEXT, `replaceAll` INTEGER NOT NULL, `caseSensitive` INTEGER NOT NULL, `continueMatching` INTEGER NOT NULL)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_regexEdits_preset` ON $tableName (`preset`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_regexEdits_order` ON $tableName (`order`)")

                tableName = "blockedMetadata"
                database.execSQL("CREATE TABLE IF NOT EXISTS $tableName (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `track` TEXT NOT NULL, `album` TEXT NOT NULL, `artist` TEXT NOT NULL, `albumArtist` TEXT NOT NULL)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_blockedMetadata_track_album_artist_albumArtist` ON $tableName (`track`, `album`, `artist`, `albumArtist`)")

                tableName = "simpleEdits"
                database.execSQL("CREATE TABLE IF NOT EXISTS `$tableName` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `legacyHash` TEXT, `origTrack` TEXT NOT NULL, `origAlbum` TEXT NOT NULL, `origArtist` TEXT NOT NULL, `track` TEXT NOT NULL, `album` TEXT NOT NULL, `albumArtist` TEXT NOT NULL, `artist` TEXT NOT NULL)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_simpleEdits_legacyHash` ON `$tableName` (`legacyHash`)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_simpleEdits_origArtist_origAlbum_origTrack` ON `$tableName` (`origArtist`, `origAlbum`, `origTrack`)")
                database.execSQL("INSERT INTO $tableName SELECT null, legacyHash, origTrack, origAlbum, origArtist, track, album, albumArtist, artist FROM edits")
                database.execSQL("DROP table edits")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val tableName = "trackedPlayers"
                database.execSQL("CREATE TABLE IF NOT EXISTS `$tableName` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timeMillis` INTEGER NOT NULL, `playerPackage` TEXT NOT NULL)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_trackedPlayers_timeMillis` ON `$tableName` (`timeMillis`)")
            }
        }

    }
}
