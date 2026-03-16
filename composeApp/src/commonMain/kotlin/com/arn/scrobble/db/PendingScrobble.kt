package com.arn.scrobble.db

import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.TypeConverters
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.ScrobbleEvent
import com.arn.scrobble.api.lastfm.ScrobbleData


/**
 * Created by arn on 11/09/2017.
 */

@Entity(tableName = PendingScrobblesDao.tableName)
data class PendingScrobble(
    @PrimaryKey(autoGenerate = true)
    val _id: Int = 0,

    @Embedded
    val scrobbleData: ScrobbleData,

    val event: ScrobbleEvent,

    @field:TypeConverters(AccountBitmaskConverter::class)
    val services: Set<AccountType> = emptySet(),

    val lastFailedTimestamp: Long? = null,
    val lastFailedReason: String? = null,
)