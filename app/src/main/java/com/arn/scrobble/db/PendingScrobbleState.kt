package com.arn.scrobble.db

interface PendingScrobbleState {
    val state: Int
    val state_timestamp: Long
}