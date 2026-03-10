package com.arn.scrobble.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = ArtistsWithDelimitersDao.tableName,
    indices = [
        Index(value = ["artist"], unique = true)
    ]
)
data class ArtistWithDelimiters(
    @PrimaryKey(autoGenerate = true)
    val _id: Int = 0,

    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val artist: String = "",
)