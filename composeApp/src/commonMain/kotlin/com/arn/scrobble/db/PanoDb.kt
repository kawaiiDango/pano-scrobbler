package com.arn.scrobble.db

import androidx.room3.AutoMigration
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
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
        CachedTrackWithAlbum::class,
        ArtistWithDelimiters::class,
    ],
    version = 18,
    autoMigrations = [
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 10, to = 11, spec = Spec_10_11::class),
        AutoMigration(from = 11, to = 12),
        AutoMigration(from = 12, to = 13),
        AutoMigration(from = 13, to = 14),
        AutoMigration(from = 15, to = 16),
        AutoMigration(from = 16, to = 17, spec = Spec_16_17::class),
        AutoMigration(from = 17, to = 18),
    ],
)
@ConstructedBy(PanoDbConstructor::class)
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
    abstract fun getCachedTracksWithAlbumsDao(): CachedTracksWithAlbumsDao
    abstract fun getArtistsWithDelimitersDao(): ArtistsWithDelimitersDao

    companion object {
        val db = PlatformStuff.getDatabaseBuilder()
            .addMigrations(*ManualMigrations.all)
            .setQueryCoroutineContext(Dispatchers.IO)
            .fallbackToDestructiveMigrationOnDowngrade(true)
            .build()
    }
}
