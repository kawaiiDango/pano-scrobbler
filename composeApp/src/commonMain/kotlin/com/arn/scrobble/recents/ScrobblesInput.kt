package com.arn.scrobble.recents

data class ScrobblesInput(
    val timeJumpMillis: Long? = null,
    val loadLoved: Boolean = false,
    val showScrobbleSources: Boolean
)