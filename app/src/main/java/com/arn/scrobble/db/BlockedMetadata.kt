package com.arn.scrobble.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "blockedMetadata",
    indices = [Index(value = ["track", "album", "artist", "albumArtist"], unique = true)]
    )
data class BlockedMetadata (
    @PrimaryKey(autoGenerate = true)
    var _id: Int = 0,

    // SQLite (unlike SQL Server) chose that multiple NULL values do not count towards uniqueness in an index.
    // Use "" for null instead

    var track: String = "",
    var album: String = "",
    var artist: String = "",
    var albumArtist: String = "",
)
