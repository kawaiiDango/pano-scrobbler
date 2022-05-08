package com.arn.scrobble.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Entity(
    tableName = ScrobbleSourcesDao.tableName,
    indices = [
        Index(value = ["timeMillis"]),
    ]
)

@Serializable
data class ScrobbleSource(
    @PrimaryKey(autoGenerate = true)
    @Transient
    val _id: Int = 0,
    val timeMillis: Long,
    val pkg: String,
)