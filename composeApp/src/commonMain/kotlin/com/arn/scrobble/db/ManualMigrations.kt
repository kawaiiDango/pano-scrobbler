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
            // destructive migration for pendingLoves
            connection.execSQL("DROP TABLE IF EXISTS PendingLoves")

            // PendingScrobbles migration
            connection.execSQL("CREATE TABLE IF NOT EXISTS `PendingScrobbles_new` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `event` TEXT NOT NULL, `services` INTEGER NOT NULL, `lastFailedTimestamp` INTEGER, `lastFailedReason` TEXT, `artist` TEXT NOT NULL, `track` TEXT NOT NULL, `album` TEXT, `timestamp` INTEGER NOT NULL, `trackNumber` INTEGER, `albumArtist` TEXT, `duration` INTEGER, `appId` TEXT)")
            connection.execSQL(
                """
                    INSERT INTO PendingScrobbles_new (
                        _id, event, services, lastFailedTimestamp, artist, track, album, timestamp, albumArtist, duration
                    )
                    SELECT
                        _id,
                        'scrobble' AS event,
                        state AS services,
                        state_timestamp AS lastFailedTimestamp,
                        artist,
                        track,
                        album,
                        timestamp,
                        albumArtist,
                        duration
                    FROM PendingScrobbles
                """.trimIndent()
            )
            connection.execSQL("DROP TABLE IF EXISTS PendingScrobbles")
            connection.execSQL("ALTER TABLE PendingScrobbles_new RENAME TO PendingScrobbles")

            // RegexEdits migration
            connection.execSQL("CREATE TABLE IF NOT EXISTS `regexEdits_new` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `order` INTEGER NOT NULL, `name` TEXT NOT NULL, `appIds` TEXT NOT NULL, `caseSensitive` INTEGER NOT NULL, `blockPlayerAction` TEXT, `searchTrack` TEXT NOT NULL, `searchAlbum` TEXT NOT NULL, `searchArtist` TEXT NOT NULL, `searchAlbumArtist` TEXT NOT NULL, `replacementTrack` TEXT, `replacementAlbum` TEXT, `replacementArtist` TEXT, `replacementAlbumArtist` TEXT, `replaceAll` INTEGER)")
            // delete presets from old table
            connection.execSQL("DELETE FROM `regexEdits` WHERE `preset` IS NOT NULL")
            // Insert for 'track'
            connection.execSQL(
                """
INSERT INTO `regexEdits_new` (
    `order`, `name`, `appIds`, `caseSensitive`,
    `searchTrack`, `searchAlbum`, `searchArtist`, `searchAlbumArtist`,
    `replacementTrack`, `replacementAlbum`, `replacementArtist`, `replacementAlbumArtist`, `replaceAll`
)
SELECT
    (SELECT COUNT(*) FROM regexEdits_new) + 1,
    COALESCE(`name`, 'Untitled'),
    COALESCE(`packages`, ''),
    COALESCE(`caseSensitive`, 0),
    COALESCE(`pattern`, ''),
    '',
    '',
    '',
    COALESCE(`replacement`, ''),
    '',
    '',
    '',
    `replaceAll`
FROM `regexEdits`
WHERE ', ' || `fields` || ', ' LIKE '%, track ,%'
                """.trimIndent()
            )
            // Insert for 'album'
            connection.execSQL(
                """
INSERT INTO `regexEdits_new` (
    `order`, `name`, `appIds`, `caseSensitive`,
    `searchTrack`, `searchAlbum`, `searchArtist`, `searchAlbumArtist`,
    `replacementTrack`, `replacementAlbum`, `replacementArtist`, `replacementAlbumArtist`, `replaceAll`
)
SELECT
    (SELECT COUNT(*) FROM regexEdits_new) + 1,
    COALESCE(`name`, 'Untitled'),
    COALESCE(`packages`, ''),
    COALESCE(`caseSensitive`, 0),
    '',
    COALESCE(`pattern`, ''),
    '',
    '',
    '',
    COALESCE(`replacement`, ''),
    '',
    '',
    `replaceAll`
FROM `regexEdits`
WHERE ', ' || `fields` || ', ' LIKE '%, album, %'
                """.trimIndent()
            )
            // Insert for 'artist'
            connection.execSQL(
                """
INSERT INTO `regexEdits_new` (
    `order`, `name`, `appIds`, `caseSensitive`,
    `searchTrack`, `searchAlbum`, `searchArtist`, `searchAlbumArtist`,
    `replacementTrack`, `replacementAlbum`, `replacementArtist`, `replacementAlbumArtist`, `replaceAll`
)
SELECT
    (SELECT COUNT(*) FROM regexEdits_new) + 1,
    COALESCE(`name`, 'Untitled'),
    COALESCE(`packages`, ''),
    COALESCE(`caseSensitive`, 0),
    '',
    '',
    COALESCE(`pattern`, ''),
    '',
    '',
    '',
    COALESCE(`replacement`, ''),
    '',
    `replaceAll`
FROM `regexEdits`
WHERE ', ' || `fields` || ', ' LIKE '%, artist, %'
""".trimIndent()
            )
            // Insert for 'albumArtist'
            connection.execSQL(
                """
INSERT INTO `regexEdits_new` (
    `order`, `name`, `appIds`, `caseSensitive`,
    `searchTrack`, `searchAlbum`, `searchArtist`, `searchAlbumArtist`,
    `replacementTrack`, `replacementAlbum`, `replacementArtist`, `replacementAlbumArtist`, `replaceAll`
)
SELECT
    (SELECT COUNT(*) FROM regexEdits_new) + 1,
    COALESCE(`name`, 'Untitled'),
    COALESCE(`packages`, ''),
    COALESCE(`caseSensitive`, 0),
    '',
    '',
    '',
    COALESCE(`pattern`, ''),
    '',
    '',
    '',
    COALESCE(`replacement`, ''),
    `replaceAll`
FROM `regexEdits`
WHERE ', ' || `fields` || ', ' LIKE '%, albumartist, %'
""".trimIndent()
            )
            // Insert rows with extractionPatterns (no fields)
            connection.execSQL(
                """
INSERT INTO `regexEdits_new` (
    `order`, `name`, `appIds`, `caseSensitive`,
    `searchTrack`, `searchAlbum`, `searchArtist`, `searchAlbumArtist`
)
SELECT
    (SELECT COUNT(*) FROM regexEdits_new) + 1,
    COALESCE(`name`, 'Untitled'),
    COALESCE(`packages`, ''),
    COALESCE(`caseSensitive`, 0),
    `extractionTrack`,
    `extractionAlbum`,
    `extractionArtist`,
    `extractionAlbumArtist`
FROM `regexEdits`
WHERE `extractionTrack` IS NOT NULL
   AND `extractionAlbum` IS NOT NULL
   AND `extractionArtist` IS NOT NULL
   AND `extractionAlbumArtist` IS NOT NULL
""".trimIndent()
            )
            connection.execSQL("DROP TABLE `regexEdits`")
            connection.execSQL("ALTER TABLE `regexEdits_new` RENAME TO `regexEdits`")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_regexEdits_order` ON `regexEdits` (`order`)")

            // BlockedMetadata migration
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