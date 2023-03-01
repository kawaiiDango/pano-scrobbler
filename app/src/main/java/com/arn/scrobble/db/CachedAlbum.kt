package com.arn.scrobble.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import de.umass.lastfm.Album
import de.umass.lastfm.ImageSize
import de.umass.lastfm.Track

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
            _id,
            albumName,
            albumUrl,
            albumMbid,
            0,
            userPlayCount,
            0,
            artistName,
            artistUrl,
            artistMbid,
            false
        ).apply {
            imageUrlsMap = mapOf(ImageSize.LARGE to (largeImageUrl ?: return@apply))
        }

        fun Album.toCachedAlbum() = CachedAlbum(
            _id = id?.toInt() ?: 0,
            albumName = name,
            albumUrl = url ?: "",
            albumMbid = mbid ?: "",
            artistName = artist,
            artistUrl = artistUrl ?: "",
            artistMbid = artistMbid ?: "",
            userPlayCount = playcount,
            largeImageUrl = getImageURL(ImageSize.LARGE)?.ifEmpty { null }
            // useless. always empty for limit=1000
        )

        fun Track.toCachedAlbum() = CachedAlbum(
            _id = id?.toInt() ?: 0,
            albumName = album!!,
            albumUrl = "",
            albumMbid = albumMbid ?: "",
            artistName = artist,
            artistUrl = artistUrl ?: "",
            artistMbid = artistMbid ?: "",
            userPlayCount = playcount,
            largeImageUrl = getImageURL(ImageSize.LARGE)?.ifEmpty { null }
        )
    }
}