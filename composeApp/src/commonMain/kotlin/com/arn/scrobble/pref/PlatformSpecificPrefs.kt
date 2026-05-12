package com.arn.scrobble.pref

import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.work.CommonWorkProgress

expect object PlatformSpecificPrefs {
    fun prefNotifications(filteredItem: FilteredItem)

    fun prefPersistentNotification(filteredItem: FilteredItem, notiPersistent: Boolean)

    fun prefScrobbler(
        filteredItem: FilteredItem,
        scrobblerEnabled: Boolean,
        nlsEnabled: Boolean,
        onNavigate: (PanoRoute) -> Unit
    )

    fun prefQuickSettings(filteredItem: FilteredItem, scrobblerEnabled: Boolean)

    fun prefChartsWidget(filteredItem: FilteredItem)

    fun prefAutostart(filteredItem: FilteredItem)
    fun prefAddToAppLauncher(filteredItem: FilteredItem)
    fun discordRpc(filteredItem: FilteredItem, onNavigate: (PanoRoute) -> Unit)
    fun tidalSteelSeries(filteredItem: FilteredItem, enabled: Boolean)

    fun deezerApi(filteredItem: FilteredItem, enabled: Boolean)

    fun updateCheck(
        filteredItem: FilteredItem,
        enabled: Boolean,
        updateProgress: CommonWorkProgress?
    )
}