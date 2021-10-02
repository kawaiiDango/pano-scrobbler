package com.arn.scrobble.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "trackedPlayers",
    indices = [
        Index(value = ["timeMillis"]),
    ])
data class TrackedPlayer (
    @PrimaryKey(autoGenerate = true)
    val _id: Int = 0,
    val timeMillis: Long,
    val playerPackage: String,
)