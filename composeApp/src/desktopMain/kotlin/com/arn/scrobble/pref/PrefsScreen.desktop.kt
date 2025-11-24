package com.arn.scrobble.pref

import androidx.compose.foundation.lazy.LazyListScope
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.utils.DesktopStuff
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.discord_rich_presence
import pano_scrobbler.composeapp.generated.resources.run_on_start
import pano_scrobbler.composeapp.generated.resources.tidal_steelseries
import pano_scrobbler.composeapp.generated.resources.use_something

actual fun prefQuickSettings(listScope: LazyListScope, scrobblerEnabled: Boolean) {
    // no-op
}

actual fun prefCrashReporter(listScope: LazyListScope, crashReporterEnabled: Boolean) {
    // no-op
}

actual fun prefChartsWidget(listScope: LazyListScope) {
    // no-op
}

actual fun prefNotifications(listScope: LazyListScope) {
    // no-op
}

actual fun prefPersistentNoti(listScope: LazyListScope, notiEnabled: Boolean) {
    // no-op
}

actual fun addToStartup(
    listScope: LazyListScope,
    isAdded: Boolean,
    onAddedChanged: (Boolean) -> Unit,
) {
    // only implemented for Linux
    if (DesktopStuff.os == DesktopStuff.Os.Linux) {
        listScope.item("startup") {
            SwitchPref(
                text = stringResource(Res.string.run_on_start),
                value = isAdded,
                copyToSave = {
                    DesktopStuff.addOrRemoveFromStartup(it)
                    onAddedChanged(it)
                    this
                }
            )
        }
    }
}

actual suspend fun isAddedToStartup() = DesktopStuff.isAddedToStartup()

actual fun discordRpc(listScope: LazyListScope, onNavigate: (PanoRoute) -> Unit) {
    listScope.item(MainPrefs::discordRpc.name) {
        TextPref(
            text = stringResource(Res.string.discord_rich_presence) + " (Experimental)",
            onClick = { onNavigate(PanoRoute.DiscordRpcSettings) }
        )
    }
}


actual fun tidalSteelSeries(listScope: LazyListScope, enabled: Boolean) {
    if (DesktopStuff.os == DesktopStuff.Os.Windows) {
        listScope.item(MainPrefs::tidalSteelSeries.name) {
            SwitchPref(
                text = stringResource(
                    Res.string.use_something,
                    stringResource(Res.string.tidal_steelseries)
                ),
                value = enabled,
                copyToSave = { copy(tidalSteelSeries = it) }
            )
        }
    }
}