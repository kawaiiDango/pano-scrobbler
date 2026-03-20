package com.arn.scrobble.db

import androidx.room3.AutoMigration
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.arn.scrobble.utils.PlatformStuff


@Database(
    entities = [
        PendingScrobble::class,
        SimpleEdit::class,
        RegexEdit::class,
        BlockedMetadata::class,
        ScrobbleSource::class,
        CustomSpotifyMapping::class,
        ArtistWithDelimiters::class,
        SeenTrack::class,
        SeenAlbum::class,
        SeenTrackAlbumAssociation::class
    ],
    version = 19,
    autoMigrations = [
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 10, to = 11, spec = Spec_10_11::class),
        AutoMigration(from = 11, to = 12),
        AutoMigration(from = 12, to = 13),
        AutoMigration(from = 13, to = 14),
        AutoMigration(from = 15, to = 16),
        AutoMigration(from = 16, to = 17, spec = Spec_16_17::class),
        AutoMigration(from = 17, to = 18),
        AutoMigration(from = 18, to = 19, spec = Spec_18_19::class),
    ],
)
@ConstructedBy(PanoDbConstructor::class)
abstract class PanoDb : RoomDatabase() {
    abstract fun getPendingScrobblesDao(): PendingScrobblesDao
    abstract fun getSimpleEditsDao(): SimpleEditsDao
    abstract fun getRegexEditsDao(): RegexEditsDao
    abstract fun getBlockedMetadataDao(): BlockedMetadataDao
    abstract fun getScrobbleSourcesDao(): ScrobbleSourcesDao
    abstract fun getCustomSpotifyMappingsDao(): CustomSpotifyMappingsDao
    abstract fun getArtistsWithDelimitersDao(): ArtistsWithDelimitersDao
    abstract fun getSeenEntitiesDao(): SeenEntitiesDao

    companion object {
        val db = PlatformStuff.getDatabaseBuilder()
            .addMigrations(*ManualMigrations.all)
            .fallbackToDestructiveMigrationOnDowngrade(true)
            .build()
    }
}
