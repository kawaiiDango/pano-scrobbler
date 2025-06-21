package com.arn.scrobble.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

object ManualMigrations {
    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(connection: SQLiteConnection) {
            val tableName = "edits"
            connection.execSQL("ALTER TABLE $tableName RENAME TO edits2")
            connection.execSQL("CREATE TABLE $tableName (`legacyHash` TEXT, `origTrack` TEXT NOT NULL, `origAlbum` TEXT NOT NULL, `origArtist` TEXT NOT NULL, `track` TEXT NOT NULL, `album` TEXT NOT NULL, `albumArtist` TEXT NOT NULL, `artist` TEXT NOT NULL, PRIMARY KEY (origArtist, origAlbum, origTrack))")
            connection.execSQL("CREATE INDEX legacyIdx ON $tableName (legacyHash)")
            connection.execSQL("INSERT INTO $tableName SELECT hash, hash, hash, hash, track, album, albumArtist, artist FROM edits2")
            connection.execSQL("DROP TABLE edits2")
        }
    }

    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(connection: SQLiteConnection) {
            var tableName = RegexEditsDao.tableName
            connection.execSQL("CREATE TABLE IF NOT EXISTS $tableName (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `order` INTEGER NOT NULL, `preset` TEXT, `name` TEXT, `pattern` TEXT, `replacement` TEXT NOT NULL, `field` TEXT, `replaceAll` INTEGER NOT NULL, `caseSensitive` INTEGER NOT NULL, `continueMatching` INTEGER NOT NULL)")
            connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_regexEdits_preset` ON $tableName (`preset`)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_regexEdits_order` ON $tableName (`order`)")

            tableName = BlockedMetadataDao.tableName
            connection.execSQL("CREATE TABLE IF NOT EXISTS $tableName (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `track` TEXT NOT NULL, `album` TEXT NOT NULL, `artist` TEXT NOT NULL, `albumArtist` TEXT NOT NULL)")
            connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_blockedMetadata_track_album_artist_albumArtist` ON $tableName (`track`, `album`, `artist`, `albumArtist`)")

            tableName = SimpleEditsDao.tableName
            connection.execSQL("CREATE TABLE IF NOT EXISTS `$tableName` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `legacyHash` TEXT, `origTrack` TEXT NOT NULL, `origAlbum` TEXT NOT NULL, `origArtist` TEXT NOT NULL, `track` TEXT NOT NULL, `album` TEXT NOT NULL, `albumArtist` TEXT NOT NULL, `artist` TEXT NOT NULL)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_simpleEdits_legacyHash` ON `$tableName` (`legacyHash`)")
            connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_simpleEdits_origArtist_origAlbum_origTrack` ON `$tableName` (`origArtist`, `origAlbum`, `origTrack`)")
            connection.execSQL("INSERT INTO $tableName SELECT null, legacyHash, origTrack, origAlbum, origArtist, track, album, albumArtist, artist FROM edits")
            connection.execSQL("DROP table edits")
        }
    }

    private val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(connection: SQLiteConnection) {
            // destructive migration for pendingScrobbles
            connection.execSQL("DROP TABLE IF EXISTS PendingLoves")
            connection.execSQL("DROP TABLE IF EXISTS ${PendingScrobblesDao.tableName}")
            connection.execSQL("CREATE TABLE IF NOT EXISTS `${PendingScrobblesDao.tableName}` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `event` TEXT NOT NULL, `services` INTEGER NOT NULL, `lastFailedTimestamp` INTEGER, `lastFailedReason` TEXT, `artist` TEXT NOT NULL, `track` TEXT NOT NULL, `album` TEXT, `timestamp` INTEGER NOT NULL, `trackNumber` INTEGER, `albumArtist` TEXT, `duration` INTEGER, `appId` TEXT)")

            // destructive migration for regexEdits
            connection.execSQL("DROP TABLE `${RegexEditsDao.tableName}`")
            connection.execSQL("CREATE TABLE IF NOT EXISTS `${RegexEditsDao.tableName}` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `order` INTEGER NOT NULL, `name` TEXT NOT NULL, `appIds` TEXT NOT NULL, `caseSensitive` INTEGER NOT NULL, `blockPlayerAction` TEXT, `searchTrack` TEXT NOT NULL, `searchAlbum` TEXT NOT NULL, `searchArtist` TEXT NOT NULL, `searchAlbumArtist` TEXT NOT NULL, `replacementTrack` TEXT, `replacementAlbum` TEXT, `replacementArtist` TEXT, `replacementAlbumArtist` TEXT, `replaceAll` INTEGER)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_regexEdits_order` ON `${RegexEditsDao.tableName}` (`order`)")

            connection.execSQL(
                """
        CREATE TABLE blockedMetadata_new (
            _id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            track TEXT NOT NULL,
            album TEXT NOT NULL,
            artist TEXT NOT NULL,
            albumArtist TEXT NOT NULL,
            blockPlayerAction TEXT NOT NULL DEFAULT ''
        )
    """.trimIndent()
            )
            connection.execSQL(
                """
        INSERT INTO blockedMetadata_new (_id, track, album, artist, albumArtist, blockPlayerAction)
        SELECT _id, track, album, artist, albumArtist, 'ignore' FROM blockedMetadata
    """.trimIndent()
            )
            connection.execSQL("DROP TABLE blockedMetadata")
            connection.execSQL("ALTER TABLE blockedMetadata_new RENAME TO blockedMetadata")

            connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_blockedMetadata_track_album_artist_albumArtist` ON `blockedMetadata` (`track`, `album`, `artist`, `albumArtist`)")
        }
    }

    val all = arrayOf(
        MIGRATION_7_8, MIGRATION_8_9, MIGRATION_14_15
    )
}