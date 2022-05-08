package com.arn.scrobble.db

import androidx.room.Entity
import androidx.room.PrimaryKey


/**
 * Created by arn on 11/09/2017.
 */

@Entity(tableName = PendingScrobblesDao.tableName)
data class PendingScrobble(
    @PrimaryKey(autoGenerate = true)
    var _id: Int = 0,

    var track: String = "",
    var album: String = "",
    var artist: String = "",
    var albumArtist: String = "",
    var duration: Long = 0,
    var timestamp: Long = System.currentTimeMillis(),
    var autoCorrected: Int = 0,
    var state: Int = 0,
    var state_timestamp: Long = System.currentTimeMillis(),
)
