package com.arn.scrobble.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.Track

@Entity(
    tableName = CachedArtistsDao.tableName,
    indices = [
        Index(value = ["artistName"], unique = true),
    ]
)
data class CachedArtist(
    @PrimaryKey(autoGenerate = true)
    val _id: Int = 0,

    val artistName: String = "",
    val artistMbid: String = "",
    val artistUrl: String = "",
    val userPlayCount: Int = -1,

    @ColumnInfo(defaultValue = "-1")
    val userPlayCountDirty: Int = -1,
) {
    companion object {
        fun CachedArtist.toArtist() = Artist(
            name = artistName,
            url = artistUrl,
            mbid = artistMbid,
            playcount = userPlayCount.toLong(),
            userplaycount = userPlayCount,
        )

        fun Artist.toCachedArtist() = CachedArtist(
            artistName = name,
            artistUrl = url ?: "",
            artistMbid = mbid ?: "",
            userPlayCount = playcount?.toInt() ?: -1,
        )

        fun Track.toCachedArtist() = CachedArtist(
            artistName = artist.name,
            artistUrl = artist.url ?: "",
            artistMbid = artist.mbid ?: "",
            userPlayCount = playcount?.toInt() ?: -1,
        )
    }
}