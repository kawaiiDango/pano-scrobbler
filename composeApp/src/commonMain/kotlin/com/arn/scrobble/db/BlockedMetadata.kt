package com.arn.scrobble.db

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(
    tableName = BlockedMetadataDao.tableName,
    indices = [Index(value = ["track", "album", "artist", "albumArtist"], unique = true)]
)
@Serializable
data class BlockedMetadata(
    @PrimaryKey(autoGenerate = true)
    val _id: Int = 0,

    // SQLite (unlike SQL Server) chose that multiple NULL values do not count towards uniqueness in an index.
    // Use "" for null instead

    val track: String = "",
    val album: String = "",
    val artist: String = "",
    val albumArtist: String = "",

    val blockPlayerAction: BlockPlayerAction = BlockPlayerAction.ignore,
)
