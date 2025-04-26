package com.arn.scrobble.pref

import com.arn.scrobble.utils.Stuff
import kotlinx.serialization.Serializable

@Serializable
data class AppItem(
    val appId: String,
    private val _label: String,
) {
    val label: String
        get() = Stuff.FRIENDLY_APP_NAMES[appId] ?: _label
}

data class AppList(
    val musicPlayers: List<AppItem> = emptyList(),
    val otherApps: List<AppItem> = emptyList()
)