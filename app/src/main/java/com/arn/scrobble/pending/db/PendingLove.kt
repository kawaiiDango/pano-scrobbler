package com.arn.scrobble.pending.db

import androidx.room.Entity
import androidx.room.PrimaryKey


/**
 * Created by arn on 11/09/2017.
 */

@Entity(tableName = "PendingLoves")
class PendingLove {
    @PrimaryKey(autoGenerate = true)
    var _id: Int = 0

    var track: String = ""
    var artist: String = ""
    var shouldLove: Boolean = true
    var state: Int = 0
    var state_timestamp: Long = System.currentTimeMillis()

    override fun toString(): String {
        return "PendingLove [track=$track, artist=$artist, shouldLove=$shouldLove, state=$state, state_timestamp=$state_timestamp]"
    }
}
