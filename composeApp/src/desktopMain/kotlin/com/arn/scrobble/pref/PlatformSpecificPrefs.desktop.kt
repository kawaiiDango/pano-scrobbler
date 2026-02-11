package com.arn.scrobble.pref

import androidx.compose.foundation.lazy.LazyListScope
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.utils.DesktopStuff
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.deezer
import pano_scrobbler.composeapp.generated.resources.discord_rich_presence
import pano_scrobbler.composeapp.generated.resources.fetch_missing_metadata
import pano_scrobbler.composeapp.generated.resources.not_running_desktop
import pano_scrobbler.composeapp.generated.resources.pref_master
import pano_scrobbler.composeapp.generated.resources.pref_offline_info
import pano_scrobbler.composeapp.generated.resources.run_on_start
import pano_scrobbler.composeapp.generated.resources.tidal
import pano_scrobbler.composeapp.generated.resources.tidal_steelseries
import pano_scrobbler.composeapp.generated.resources.when_using

actual object PlatformSpecificPrefs {
    actual fun prefQuickSettings(listScope: LazyListScope, scrobblerEnabled: Boolean) {
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
                text = stringResource(Res.string.discord_rich_presence),
                onClick = { onNavigate(PanoRoute.DiscordRpcSettings) }
            )
        }
    }


    actual fun tidalSteelSeries(listScope: LazyListScope, enabled: Boolean) {
        if (DesktopStuff.os == DesktopStuff.Os.Windows) {
            listScope.item(MainPrefs::tidalSteelSeriesApi.name) {
                SwitchPref(
                    text = stringResource(Res.string.tidal_steelseries),
                    summary = stringResource(
                        Res.string.when_using,
                        stringResource(Res.string.tidal)
                    ),
                    value = enabled,
                    copyToSave = { copy(tidalSteelSeriesApi = it) }
                )
            }
        }
    }

    actual fun deezerApi(listScope: LazyListScope, enabled: Boolean) {
        if (DesktopStuff.os == DesktopStuff.Os.Windows) {
            listScope.item(MainPrefs::deezerApi.name) {
                SwitchPref(
                    text = stringResource(
                        Res.string.fetch_missing_metadata,
                        stringResource(Res.string.deezer)
                    ),
                    summary = stringResource(
                        Res.string.when_using,
                        stringResource(Res.string.deezer)
                    ),
                    value = enabled,
                    copyToSave = { copy(deezerApi = it) }
                )
            }
        }
    }

    actual fun prefScrobbler(
        listScope: LazyListScope,
        scrobblerEnabled: Boolean,
        nlsEnabled: Boolean,
        onNavigate: (PanoRoute) -> Unit,
    ) {
        listScope.item(MainPrefs::scrobblerEnabled.name) {
            SwitchPref(
                text = stringResource(Res.string.pref_master),
                summary = if (!nlsEnabled)
                    stringResource(Res.string.not_running_desktop)
                else
                    stringResource(Res.string.pref_offline_info),
                value = scrobblerEnabled && nlsEnabled,
                enabled = nlsEnabled,
                copyToSave = { copy(scrobblerEnabled = it) }
            )
        }
    }
}