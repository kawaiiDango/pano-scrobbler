package com.arn.scrobble.recents

import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.Track

data class ScrobblesInput(
    val user: UserCached,
    val timeJumpMillis: Long? = null,
    val loadLoved: Boolean = false,
    val track: Track? = null,
)