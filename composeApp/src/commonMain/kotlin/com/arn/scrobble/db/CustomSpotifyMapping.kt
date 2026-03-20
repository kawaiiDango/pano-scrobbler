package com.arn.scrobble.db

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Entity(
    tableName = CustomSpotifyMappingsDao.tableName,
    indices = [Index(value = ["artist", "album"], unique = true)]
)

@Serializable
data class CustomSpotifyMapping(
    @PrimaryKey(autoGenerate = true)
    @Transient
    val _id: Long = 0,

    // SQLite (unlike SQL Server) chose that multiple NULL values do not count towards uniqueness in an index.
    // Use "" for null instead

    val artist: String = "",
    val album: String = "",
    val spotifyId: String? = null,
    val fileUri: String? = null,
)
