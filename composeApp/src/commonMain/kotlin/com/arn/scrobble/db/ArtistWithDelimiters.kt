package com.arn.scrobble.db

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = ArtistsWithDelimitersDao.tableName,
    indices = [
        Index(value = ["artist"], unique = true)
    ]
)
data class ArtistWithDelimiters(
    @PrimaryKey(autoGenerate = true)
    val _id: Long = 0,

    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val artist: String = "",
)