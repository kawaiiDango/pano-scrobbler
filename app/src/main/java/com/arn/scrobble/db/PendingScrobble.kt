package com.arn.scrobble.db

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey


/**
 * Created by arn on 11/09/2017.
 */

@Entity(tableName = "PendingScrobbles")
class PendingScrobble {
    @PrimaryKey(autoGenerate = true)
    var _id: Int = 0

    var track: String = ""
    var album: String = ""
    var artist: String = ""
    var duration: Long = 0
    var timestamp: Long = System.currentTimeMillis()
    var autoCorrected: Int = 0
    var state: Int = 0
    var state_timestamp: Long = System.currentTimeMillis()

    override fun toString(): String {
        return "PendingScrobble [track=$track, album=$album, artist=$artist, duration=$duration, timestamp=$timestamp," +
                " autoCorrected=$autoCorrected, state=$state, state_timestamp=$state_timestamp]"
    }
}
