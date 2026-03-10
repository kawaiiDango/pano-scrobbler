package com.arn.scrobble.pref

import androidx.compose.foundation.lazy.LazyListScope
import com.arn.scrobble.navigation.PanoRoute

expect object PlatformSpecificPrefs {
    fun prefNotifications(listScope: LazyListScope)

    fun prefScrobbler(
        listScope: LazyListScope,
        scrobblerEnabled: Boolean,
        nlsEnabled: Boolean,
        onNavigate: (PanoRoute) -> Unit
    )

    fun prefQuickSettings(listScope: LazyListScope, scrobblerEnabled: Boolean)

    fun prefPersistentNoti(listScope: LazyListScope, notiEnabled: Boolean)

    fun prefChartsWidget(listScope: LazyListScope)

    fun prefAutostart(listScope: LazyListScope)
    fun discordRpc(listScope: LazyListScope, onNavigate: (PanoRoute) -> Unit)
    fun tidalSteelSeries(listScope: LazyListScope, enabled: Boolean)

    fun deezerApi(listScope: LazyListScope, enabled: Boolean)
}