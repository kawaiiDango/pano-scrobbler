package com.arn.scrobble.db

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(
    tableName = BlockedMetadataDao.tableName,
    indices = [Index(value = ["track", "album", "artist", "albumArtist"], unique = true)]
)
@Parcelize
data class BlockedMetadata(
    @PrimaryKey(autoGenerate = true)
    var _id: Int = 0,

    // SQLite (unlike SQL Server) chose that multiple NULL values do not count towards uniqueness in an index.
    // Use "" for null instead

    var track: String = "",
    var album: String = "",
    var artist: String = "",
    var albumArtist: String = "",

    @ColumnInfo(defaultValue = "0")
    var skip: Boolean = false,

    @ColumnInfo(defaultValue = "0")
    var mute: Boolean = false,
) : Parcelable
