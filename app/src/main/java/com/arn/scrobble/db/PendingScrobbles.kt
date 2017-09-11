package com.arn.scrobble.db

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey


/**
 * Created by arn on 11/09/2017.
 */

@Entity
class PendingScrobbles {
    @PrimaryKey
    private val id: Int = 0

    val track: String? = null
    val artist: String? = null
    val album: String? = null
    val duration: Long = 0
    val timestamp: Long = 0
    val autoCorrected: Int = 0
    val state: Int = 0
    val state_timestamp: Long = 0
}
