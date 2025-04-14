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
    val _id: Int = 0,

    val trackName: String = "",
    val trackMbid: String = "",
    val trackUrl: String = "",

    val artistName: String = "",
    val artistMbid: String = "",
    val artistUrl: String = "",

    // optional
    val durationSecs: Int = -1,
    val userPlayCount: Int = -1,

    @ColumnInfo(defaultValue = "-1")
    val userPlayCountDirty: Int = -1,
    val isLoved: Boolean = false,
    val lastPlayed: Long = -1,
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
            duration = durationSecs.takeIf { it > 0 }?.toLong()?.times(1000),
            playcount = userPlayCount.toLong(),
            userloved = isLoved,
            date = lastPlayed
        )

        fun Track.toCachedTrack() = CachedTrack(
            trackName = name,
            trackUrl = url ?: "",
            trackMbid = mbid ?: "",
            artistName = artist.name,
            artistUrl = artist.url ?: "",
            artistMbid = artist.mbid ?: "",
            durationSecs = duration?.div(1000)?.toInt() ?: -1,
            userPlayCount = playcount?.toInt() ?: -1,
            isLoved = userloved ?: false,
            lastPlayed = date ?: -1
        )
    }
}