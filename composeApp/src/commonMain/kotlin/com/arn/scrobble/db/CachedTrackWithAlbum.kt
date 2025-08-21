package com.arn.scrobble.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = CachedTracksWithAlbumsDao.tableName,
    indices = [
        Index(value = ["artist", "track", "album"], unique = true)
    ]
)
data class CachedTrackWithAlbum(
    @PrimaryKey(autoGenerate = true)
    val _id: Int = 0,

    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val artist: String = "",
    val artistMbid: String? = null,

    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val track: String = "",
    val trackMbid: String? = null,

    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val album: String = "",
    val albumMbid: String? = null,

    val insertedAt: Long = System.currentTimeMillis(),
)