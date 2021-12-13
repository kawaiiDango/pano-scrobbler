package com.arn.scrobble.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "scrobbleSources",
    indices = [
        Index(value = ["timeMillis"]),
    ])
data class ScrobbleSource (
    @PrimaryKey(autoGenerate = true)
    val _id: Int = 0,
    val timeMillis: Long,
    val pkg: String,
)