package com.arn.scrobble.friends

import com.arn.scrobble.api.lastfm.Track
import kotlinx.serialization.Serializable

@Serializable
data class FriendExtraData(
    val track: Track,
    val playCount: Int?,
    val lastUpdated: Long,
)