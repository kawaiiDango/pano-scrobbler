package com.arn.scrobble.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import de.umass.lastfm.Artist
import de.umass.lastfm.ImageSize
import de.umass.lastfm.Track

@Entity(
    tableName = CachedArtistsDao.tableName,
    indices = [
        Index(value = ["artistName"], unique = true),
    ]
)
data class CachedArtist(
    @PrimaryKey(autoGenerate = true)
    var _id: Int = 0,

    var artistName: String = "",
    var artistMbid: String = "",
    var artistUrl: String = "",
    var userPlayCount: Int = -1,
) {
    companion object {
        fun CachedArtist.toArtist() = Artist(
            _id,
            artistName,
            artistUrl,
            artistMbid,
            0,
            userPlayCount,
            0,
            false,
        )

        fun Artist.toCachedArtist() = CachedArtist(
            _id = id?.toInt() ?: 0,
            artistName = name,
            artistUrl = url ?: "",
            artistMbid = mbid ?: "",
            userPlayCount = playcount,
        )

        fun Track.toCachedArtist() = CachedArtist(
            _id = id?.toInt() ?: 0,
            artistName = artist,
            artistUrl = artistUrl ?: "",
            artistMbid = artistMbid ?: "",
            userPlayCount = playcount,
        )
    }
}