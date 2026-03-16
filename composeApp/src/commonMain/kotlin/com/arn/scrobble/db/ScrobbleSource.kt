package com.arn.scrobble.db

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(
    tableName = ScrobbleSourcesDao.tableName,
    indices = [
        Index(value = ["timeMillis"]),
    ]
)

@Serializable
data class ScrobbleSource(
    @PrimaryKey(autoGenerate = true)
    val _id: Int = 0,
    val timeMillis: Long,
    val pkg: String,
)