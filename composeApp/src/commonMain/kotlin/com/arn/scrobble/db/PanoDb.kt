package com.arn.scrobble.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.arn.scrobble.utils.PlatformStuff
import kotlinx.coroutines.Dispatchers


/**
 * Created by arn on 11/09/2017.
 */

@Database(
    entities = [
        PendingScrobble::class,
        SimpleEdit::class,
        RegexEdit::class,
        BlockedMetadata::class,
        ScrobbleSource::class,
        CachedTrack::class,
        CachedAlbum::class,
        CachedArtist::class,
        CustomSpotifyMapping::class,
    ],
    version = 15,
    autoMigrations = [
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 10, to = 11, spec = Spec_10_11::class),
        AutoMigration(from = 11, to = 12),
        AutoMigration(from = 12, to = 13),
        AutoMigration(from = 13, to = 14),
//        AutoMigration(from = 14, to = 15),
    ],
)
abstract class PanoDb : RoomDatabase() {
    abstract fun getPendingScrobblesDao(): PendingScrobblesDao
    abstract fun getSimpleEditsDao(): SimpleEditsDao
    abstract fun getRegexEditsDao(): RegexEditsDao
    abstract fun getBlockedMetadataDao(): BlockedMetadataDao
    abstract fun getScrobbleSourcesDao(): ScrobbleSourcesDao
    abstract fun getCachedTracksDao(): CachedTracksDao
    abstract fun getCachedAlbumsDao(): CachedAlbumsDao
    abstract fun getCachedArtistsDao(): CachedArtistsDao
    abstract fun getCustomSpotifyMappingsDao(): CustomSpotifyMappingsDao

    companion object {
        const val FILE_NAME = "pendingScrobbles"

        @Volatile
        private var INSTANCE: PanoDb? = null

        val db
            get(): PanoDb = INSTANCE ?: synchronized(this) {
                PlatformStuff.getDatabaseBuilder()
                    .addMigrations(*ManualMigrations.all)
                    .setQueryCoroutineContext(Dispatchers.IO)
                    .fallbackToDestructiveMigrationOnDowngrade(true)
                    .build()
                    .also { INSTANCE = it }
            }

        fun destroyInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }

    }
}
