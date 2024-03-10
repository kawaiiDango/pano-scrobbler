package com.arn.scrobble.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.ImageSize
import com.arn.scrobble.api.lastfm.LastFmImage
import com.arn.scrobble.api.lastfm.Track

@Entity(
    tableName = CachedAlbumsDao.tableName,
    indices = [
        Index(value = ["artistName", "albumName"], unique = true),
    ]
)
data class CachedAlbum(
    @PrimaryKey(autoGenerate = true)
    var _id: Int = 0,

    var albumName: String = "",
    var albumMbid: String = "",
    var albumUrl: String = "",
    var artistName: String = "",
    var artistMbid: String = "",
    var artistUrl: String = "",
    var largeImageUrl: String? = null,
    var userPlayCount: Int = -1,

    @ColumnInfo(defaultValue = "-1")
    var userPlayCountDirty: Int = -1,
) {
    companion object {
        fun CachedAlbum.toAlbum() = Album(
            name = albumName,
            url = albumUrl,
            mbid = albumMbid,
            artist = Artist(
                name = artistName,
                url = artistUrl,
                mbid = artistMbid
            ),
            playcount = userPlayCount,
            image = largeImageUrl?.let { listOf(LastFmImage(ImageSize.extralarge.name, it)) },
        )

        fun Album.toCachedAlbum() = CachedAlbum(
            albumName = name,
            albumUrl = url ?: "",
            albumMbid = mbid ?: "",
            artistName = artist!!.name,
            artistUrl = artist.url ?: "",
            artistMbid = artist.mbid ?: "",
            userPlayCount = playcount ?: -1,
            largeImageUrl = image?.find { it.size == ImageSize.extralarge.name }?.url
        )

        fun Track.toCachedAlbum() = CachedAlbum(
            albumName = album?.name ?: "",
            albumUrl = album?.url ?: "",
            albumMbid = album?.mbid ?: "",
            artistName = artist.name,
            artistUrl = artist.url ?: "",
            artistMbid = artist.mbid ?: "",
            userPlayCount = playcount ?: -1,
            largeImageUrl = image?.find { it.size == ImageSize.extralarge.name }?.url
        )
    }
}