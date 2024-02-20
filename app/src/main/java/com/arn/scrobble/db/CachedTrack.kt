package com.arn.scrobble.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.Track

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

    @ColumnInfo(defaultValue = "-1")
    var userPlayCountDirty: Int = -1,
    var isLoved: Boolean = false,
    var lastPlayed: Long = -1,
) {
    val plays get() = if (userPlayCountDirty != -1) userPlayCountDirty else userPlayCount

    companion object {
        fun CachedTrack.toTrack() = Track(
            name = trackName,
            url = trackUrl,
            mbid = trackMbid,
            artist = Artist(
                name = artistName,
                url = artistUrl,
                mbid = artistMbid
            ),
            album = null,
            duration = durationSecs,
            playcount = userPlayCount,
            userloved = isLoved,
            date = lastPlayed.toInt()
        )

        fun Track.toCachedTrack() = CachedTrack(
            trackName = name,
            trackUrl = url ?: "",
            trackMbid = mbid ?: "",
            artistName = artist.name,
            artistUrl = artist.url ?: "",
            artistMbid = artist.mbid ?: "",
            durationSecs = duration ?: -1,
            userPlayCount = playcount ?: -1,
            isLoved = userloved ?: false,
            lastPlayed = date?.toLong() ?: -1
        )
    }
}