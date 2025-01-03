package com.arn.scrobble.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Entity(
    tableName = BlockedMetadataDao.tableName,
    indices = [Index(value = ["track", "album", "artist", "albumArtist"], unique = true)]
)
@Serializable
data class BlockedMetadata(
    @PrimaryKey(autoGenerate = true)
    @Transient
    val _id: Int = 0,

    // SQLite (unlike SQL Server) chose that multiple NULL values do not count towards uniqueness in an index.
    // Use "" for null instead

    val track: String = "",
    val album: String = "",
    val artist: String = "",
    val albumArtist: String = "",

    @ColumnInfo(defaultValue = "0")
    val skip: Boolean = false,

    @ColumnInfo(defaultValue = "0")
    val mute: Boolean = false,
)
