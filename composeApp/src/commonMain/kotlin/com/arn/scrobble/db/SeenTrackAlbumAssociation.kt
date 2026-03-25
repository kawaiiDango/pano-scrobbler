package com.arn.scrobble.db


import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index


@Entity(
    tableName = SeenTrackAlbumAssociation.TABLE,
    primaryKeys = ["trackId", "albumId"],
    foreignKeys = [
        ForeignKey(
            entity = SeenTrack::class,
            parentColumns = ["_id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SeenAlbum::class,
            parentColumns = ["_id"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    // Index on album_id so reverse lookups (all tracks in an album) are fast.
    // track_id is already covered by the composite PK index.
    indices = [Index("albumId")],
)
data class SeenTrackAlbumAssociation(
    val trackId: Long,
    val albumId: Long,

    val priority: Int,

    /** Epoch ms. Last time this association was observed from its source. */
    val seenAt: Long,
) {
    companion object {
        const val TABLE = "seenTrackAlbumAssociations"
    }


    /**
     * Priority constants — higher number wins.
     * Use intervals of 10 so you can insert intermediate values later without
     * renumbering everything.
     */
    enum class Priority(val n: Int) {
        ALBUM_INFO(50),  // Inferred from album.getInfo tracklist
        TRACK_INFO(40), // From track.getInfo response
        CHARTS(30),  // From top tracks in charts for ListenBrainz
        RECENT_TRACKS(20),  // From user.getRecentTracks
        MEDIA_PLAYER(10)  // Direct from OS media metadata — most trusted
    }
}