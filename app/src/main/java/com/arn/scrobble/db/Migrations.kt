package com.arn.scrobble.db

import androidx.room.RenameColumn
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@RenameColumn(
    tableName = RegexEditsDao.tableName,
    fromColumnName = "field",
    toColumnName = "fields"
)
class Spec_10_11: AutoMigrationSpec

object ManualMigrations {

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            val tableName = "edits"
            database.execSQL("CREATE TABLE IF NOT EXISTS $tableName (`hash` TEXT PRIMARY KEY NOT NULL, `track` TEXT NOT NULL, `album` TEXT NOT NULL, `albumArtist` TEXT NOT NULL, `artist` TEXT NOT NULL)")
        }
    }

    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            val tableName = PendingLovesDao.tableName
            database.execSQL("ALTER TABLE $tableName ADD state INTEGER NOT NULL DEFAULT 7") //111
            database.execSQL("ALTER TABLE $tableName ADD state_timestamp INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
            database.execSQL("UPDATE ${PanoDb.fileName} SET state = 31 WHERE state <= 0") //11111
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
            var tableName = RegexEditsDao.tableName
            database.execSQL("CREATE TABLE IF NOT EXISTS $tableName (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `order` INTEGER NOT NULL, `preset` TEXT, `name` TEXT, `pattern` TEXT, `replacement` TEXT NOT NULL, `field` TEXT, `replaceAll` INTEGER NOT NULL, `caseSensitive` INTEGER NOT NULL, `continueMatching` INTEGER NOT NULL)")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_regexEdits_preset` ON $tableName (`preset`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_regexEdits_order` ON $tableName (`order`)")

            tableName = BlockedMetadataDao.tableName
            database.execSQL("CREATE TABLE IF NOT EXISTS $tableName (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `track` TEXT NOT NULL, `album` TEXT NOT NULL, `artist` TEXT NOT NULL, `albumArtist` TEXT NOT NULL)")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_blockedMetadata_track_album_artist_albumArtist` ON $tableName (`track`, `album`, `artist`, `albumArtist`)")

            tableName = SimpleEditsDao.tableName
            database.execSQL("CREATE TABLE IF NOT EXISTS `$tableName` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `legacyHash` TEXT, `origTrack` TEXT NOT NULL, `origAlbum` TEXT NOT NULL, `origArtist` TEXT NOT NULL, `track` TEXT NOT NULL, `album` TEXT NOT NULL, `albumArtist` TEXT NOT NULL, `artist` TEXT NOT NULL)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_simpleEdits_legacyHash` ON `$tableName` (`legacyHash`)")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_simpleEdits_origArtist_origAlbum_origTrack` ON `$tableName` (`origArtist`, `origAlbum`, `origTrack`)")
            database.execSQL("INSERT INTO $tableName SELECT null, legacyHash, origTrack, origAlbum, origArtist, track, album, albumArtist, artist FROM edits")
            database.execSQL("DROP table edits")
        }
    }

    val all = arrayOf(
        MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9
    )
}