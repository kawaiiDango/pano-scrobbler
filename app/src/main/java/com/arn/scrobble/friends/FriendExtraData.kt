package com.arn.scrobble.friends

import com.arn.scrobble.api.lastfm.Track

data class FriendExtraData(
    val track: Track,
    val playCount: Int?,
    val lastUpdated: Long,
)