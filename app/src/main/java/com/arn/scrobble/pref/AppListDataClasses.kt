package com.arn.scrobble.pref

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class AppItem(
    val appId: String,
    val label: String,
) : Parcelable

data class AppList(
    val musicPlayers: List<AppItem> = emptyList(),
    val otherApps: List<AppItem> = emptyList()
)