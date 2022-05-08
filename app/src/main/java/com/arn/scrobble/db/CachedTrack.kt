package com.arn.scrobble.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import de.umass.lastfm.Track
import java.util.*

@Entity(
    tableName = CachedTracksDao.tableName,
    indices = [
        Index(value = ["artistName", "trackName"], unique = true),
        Index(value = ["isLoved"])
    ]
)
data class CachedTrack(
    @PrimaryKey(autoGenerate = true)
    var _id: Int = 0,

    var trackName: String = "",
    var trackMbid: String = "",
    var trackUrl: String = "",

    var artistName: String = "",
    var artistMbid: String = "",
    var artistUrl: String = "",

    // optional
    var durationSecs: Int = -1,
    var userPlayCount: Int = -1,
    var isLoved: Boolean = false,
    var lastPlayed: Long = -1,
) {
    companion object {
        fun CachedTrack.toTrack() = Track(
            _id,
            trackName,
            trackUrl,
            trackMbid,
            0,
            userPlayCount,
            isLoved,
            0,
            durationSecs,
            false,
            null,
            null,
            artistName,
            artistUrl,
            artistMbid,
            Date(lastPlayed),
            false,
            false
        )

        fun Track.toCachedTrack() = CachedTrack(
            _id = id?.toInt() ?: 0,
            trackName = name,
            trackUrl = url ?: "",
            trackMbid = mbid ?: "",
            artistName = artist,
            artistUrl = artistUrl ?: "",
            artistMbid = artistMbid ?: "",
            userPlayCount = playcount,
            isLoved = isLoved,
            durationSecs = duration,
            lastPlayed = playedWhen?.time ?: -1
        )
    }
}