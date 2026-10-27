package com.arn.scrobble.pref

import com.arn.scrobble.work.CommonWorkProgress

expect object PlatformSpecificPrefs {
    fun prefNotifications(filteredItem: FilteredItem)

    fun prefPersistentNotification(filteredItem: FilteredItem, notiPersistent: Boolean)

    fun onPrefScrobblerToggled(scrobblerEnabled: Boolean)

    fun prefQuickSettings(filteredItem: FilteredItem, scrobblerEnabled: Boolean)

    fun prefChartsWidget(filteredItem: FilteredItem)

    fun prefAutostart(filteredItem: FilteredItem)
    fun prefAddToAppLauncher(filteredItem: FilteredItem)
    fun tidalSteelSeries(filteredItem: FilteredItem, enabled: Boolean)

    fun deezerApi(filteredItem: FilteredItem, enabled: Boolean)

    fun updateCheck(
        filteredItem: FilteredItem,
        enabled: Boolean,
        updateProgress: CommonWorkProgress?
    )
}