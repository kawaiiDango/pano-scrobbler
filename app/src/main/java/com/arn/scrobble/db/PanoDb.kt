package com.arn.scrobble.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.arn.scrobble.App


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
        ScrobbleSource::class,
        CachedTrack::class,
        CachedAlbum::class,
        CachedArtist::class,
    ],
    version = 12,
    autoMigrations = [
        AutoMigration(
            from = 9,
            to = 10,
        ),
        AutoMigration(
            from = 10,
            to = 11,
            spec = Spec_10_11::class
        ),
        AutoMigration(
            from = 11,
            to = 12,
        ),
    ],
)
abstract class PanoDb : RoomDatabase() {
    abstract fun getPendingScrobblesDao(): PendingScrobblesDao
    abstract fun getPendingLovesDao(): PendingLovesDao
    abstract fun getSimpleEditsDao(): SimpleEditsDao
    abstract fun getRegexEditsDao(): RegexEditsDao
    abstract fun getBlockedMetadataDao(): BlockedMetadataDao
    abstract fun getScrobbleSourcesDao(): ScrobbleSourcesDao
    abstract fun getCachedTracksDao(): CachedTracksDao
    abstract fun getCachedAlbumsDao(): CachedAlbumsDao
    abstract fun getCachedArtistsDao(): CachedArtistsDao

    companion object {
        const val fileName = "pendingScrobbles"

        @Volatile
        private var INSTANCE: PanoDb? = null

        val db get(): PanoDb = INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(App.context, PanoDb::class.java, fileName)
                .addMigrations(*ManualMigrations.all)
                .enableMultiInstanceInvalidation()
                .build()
                .also { INSTANCE = it }
        }

        fun destroyInstance() {
            if (INSTANCE?.isOpen == true)
                INSTANCE?.close()
            INSTANCE = null
        }

    }
}
