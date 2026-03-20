package com.arn.scrobble.db

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = SeenTrack.TABLE,
    indices = [Index(value = ["artist", "track"], unique = true)]
)
data class SeenTrack(
    @PrimaryKey(autoGenerate = true)
    val _id: Long = 0,

    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val artist: String,

    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val track: String,

    /** null = unknown, false = not loved, true = loved */
    val isLoved: Boolean? = null,

    /** Epoch ms. When isLoved was last written from any source. */
    val lovedStateUpdatedAt: Long? = null,

    /**
     * Epoch ms. When track.getInfo was last attempted for this artist+track,
     * regardless of whether an album was returned. Used for TTL-based
     * re-fetch throttling.
     */
    val trackInfoFetchedAt: Long? = null,
) {
    companion object {
        const val TABLE = "seenTracks"
    }
}