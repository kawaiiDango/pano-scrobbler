package com.arn.scrobble.db

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = SeenAlbum.TABLE,
    indices = [Index(value = ["artist", "album"], unique = true)]
)
data class SeenAlbum(
    @PrimaryKey(autoGenerate = true)
    val _id: Long = 0,

    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val artist: String,

    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val album: String,

    val artUrl: String? = null,
    val artUpdatedAt: Long? = null,
) {
    companion object {
        const val TABLE = "seenAlbums"
    }
}