package com.arn.scrobble.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey


/**
 * Created by arn on 11/09/2017.
 */

@Entity(tableName = PendingScrobblesDao.tableName)
data class PendingScrobble(
    @PrimaryKey(autoGenerate = true)
    val _id: Int = 0,

    val track: String = "",
    val album: String = "",
    val artist: String = "",
    val albumArtist: String = "",
    val duration: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val autoCorrected: Int = 0,
    override val state: Int = 0,
    override val state_timestamp: Long = System.currentTimeMillis(),
) : PendingScrobbleState