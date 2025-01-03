package com.arn.scrobble.pref

import kotlinx.serialization.Serializable

@Serializable
data class AppItem(
    val appId: String,
    val label: String,
)

data class AppList(
    val musicPlayers: List<AppItem> = emptyList(),
    val otherApps: List<AppItem> = emptyList()
)