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
    val _id: Int = 0,

    val albumName: String = "",
    val albumMbid: String = "",
    val albumUrl: String = "",
    val artistName: String = "",
    val artistMbid: String = "",
    val artistUrl: String = "",
    val largeImageUrl: String? = null,
    val userPlayCount: Int = -1,

    @ColumnInfo(defaultValue = "-1")
    val userPlayCountDirty: Int = -1,
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
            playcount = userPlayCount.toLong(),
            image = largeImageUrl?.let { listOf(LastFmImage(ImageSize.extralarge.name, it)) },
        )

        fun Album.toCachedAlbum() = CachedAlbum(
            albumName = name,
            albumUrl = url ?: "",
            albumMbid = mbid ?: "",
            artistName = artist!!.name,
            artistUrl = artist.url ?: "",
            artistMbid = artist.mbid ?: "",
            userPlayCount = playcount?.toInt() ?: -1,
            largeImageUrl = image?.find { it.size == ImageSize.extralarge.name }?.url
        )

        fun Track.toCachedAlbum() = CachedAlbum(
            albumName = album?.name ?: "",
            albumUrl = album?.url ?: "",
            albumMbid = album?.mbid ?: "",
            artistName = artist.name,
            artistUrl = artist.url ?: "",
            artistMbid = artist.mbid ?: "",
            userPlayCount = playcount?.toInt() ?: -1,
            largeImageUrl = album?.image?.find { it.size == ImageSize.extralarge.name }?.url
        )
    }
}