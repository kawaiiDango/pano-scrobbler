package com.arn.scrobble.db

import androidx.room.Entity
import androidx.room.PrimaryKey


/**
 * Created by arn on 11/09/2017.
 */

@Entity(tableName = PendingLovesDao.tableName)
data class PendingLove(
    @PrimaryKey(autoGenerate = true)
    val _id: Int = 0,

    val track: String = "",
    val artist: String = "",
    val shouldLove: Boolean = true,
    override val state: Int = 0,
    override val state_timestamp: Long = System.currentTimeMillis(),
) : PendingScrobbleState
