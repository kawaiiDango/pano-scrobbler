package com.arn.scrobble.pref

import com.arn.scrobble.navigation.PanoRoute

expect object PlatformSpecificPrefs {
    fun prefNotifications(filteredItem: FilteredItem)

    fun prefScrobbler(
        filteredItem: FilteredItem,
        scrobblerEnabled: Boolean,
        nlsEnabled: Boolean,
        onNavigate: (PanoRoute) -> Unit
    )

    fun prefQuickSettings(filteredItem: FilteredItem, scrobblerEnabled: Boolean)

    fun prefPersistentNoti(filteredItem: FilteredItem, notiEnabled: Boolean)

    fun prefChartsWidget(filteredItem: FilteredItem)

    fun prefAutostart(filteredItem: FilteredItem)
    fun discordRpc(filteredItem: FilteredItem, onNavigate: (PanoRoute) -> Unit)
    fun tidalSteelSeries(filteredItem: FilteredItem, enabled: Boolean)

    fun deezerApi(filteredItem: FilteredItem, enabled: Boolean)
}