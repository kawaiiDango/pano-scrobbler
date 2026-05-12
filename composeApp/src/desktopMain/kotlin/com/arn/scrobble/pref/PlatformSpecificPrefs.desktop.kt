package com.arn.scrobble.pref

import com.arn.scrobble.PanoNativeComponents
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.ui.PanoSnackbarVisuals
import com.arn.scrobble.utils.DesktopStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.work.CommonWorkProgress
import com.arn.scrobble.work.UpdaterWork
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.add_to_app_launcher
import pano_scrobbler.composeapp.generated.resources.deezer
import pano_scrobbler.composeapp.generated.resources.disable
import pano_scrobbler.composeapp.generated.resources.discord_rich_presence
import pano_scrobbler.composeapp.generated.resources.done
import pano_scrobbler.composeapp.generated.resources.enable
import pano_scrobbler.composeapp.generated.resources.pref_check_updates
import pano_scrobbler.composeapp.generated.resources.pref_fetch_missing_album
import pano_scrobbler.composeapp.generated.resources.pref_master
import pano_scrobbler.composeapp.generated.resources.pref_notify_updates
import pano_scrobbler.composeapp.generated.resources.pref_offline_info
import pano_scrobbler.composeapp.generated.resources.run_on_start
import pano_scrobbler.composeapp.generated.resources.tidal
import pano_scrobbler.composeapp.generated.resources.tidal_steelseries
import pano_scrobbler.composeapp.generated.resources.when_using

actual object PlatformSpecificPrefs {
    actual fun prefQuickSettings(filteredItem: FilteredItem, scrobblerEnabled: Boolean) {
        // no-op
    }

    actual fun prefChartsWidget(filteredItem: FilteredItem) {
        // no-op
    }

    actual fun prefNotifications(filteredItem: FilteredItem) {
        // no-op
    }

    actual fun prefPersistentNotification(filteredItem: FilteredItem, notiPersistent: Boolean) {
    }

    actual fun prefAutostart(filteredItem: FilteredItem) {
        // only implemented for Linux
        if (DesktopStuff.os == DesktopStuff.Os.Linux) {
            filteredItem("startup", Res.string.run_on_start, null) { title ->
                val doneString = stringResource(Res.string.done)

                DropdownPref(
                    text = title,
                    selectedValue = null,
                    values = listOf(true, false),
                    toLabel = {
                        if (it)
                            stringResource(Res.string.enable)
                        else
                            stringResource(Res.string.disable)
                    },
                    copyToSave = {
                        PanoNativeComponents.autoStartLinux(it)
                        val snackbarData = PanoSnackbarVisuals(doneString)
                        Stuff.globalSnackbarFlow.tryEmit(snackbarData)

                        this
                    }
                )
            }
        }
    }

    actual fun prefAddToAppLauncher(filteredItem: FilteredItem) {
        // only implemented for Linux AppImage
        if (DesktopStuff.os == DesktopStuff.Os.Linux && System.getenv("APPIMAGE") != null) {
            filteredItem(
                "app_launcher",
                Res.string.add_to_app_launcher,
                null
            ) { title ->
                val doneString = stringResource(Res.string.done)
                TextPref(
                    text = title,
                    onClick = {
                        DesktopStuff.addAppImageToAppLauncher()
                        val snackbarData = PanoSnackbarVisuals(doneString)
                        Stuff.globalSnackbarFlow.tryEmit(snackbarData)
                    }
                )
            }
        }
    }

    actual fun discordRpc(filteredItem: FilteredItem, onNavigate: (PanoRoute) -> Unit) {
        filteredItem(MainPrefs::discordRpc.name, Res.string.discord_rich_presence, null) { title ->
            TextPref(
                text = title,
                onClick = { onNavigate(PanoRoute.DiscordRpcSettings) }
            )
        }
    }


    actual fun tidalSteelSeries(filteredItem: FilteredItem, enabled: Boolean) {
        if (DesktopStuff.os == DesktopStuff.Os.Windows) {
            filteredItem(
                MainPrefs::tidalSteelSeriesApi.name,
                Res.string.pref_fetch_missing_album,
                Res.string.tidal_steelseries
            ) { title ->
                SwitchPref(
                    text = title,
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

    actual fun deezerApi(filteredItem: FilteredItem, enabled: Boolean) {
        if (DesktopStuff.os == DesktopStuff.Os.Windows) {
            filteredItem(
                MainPrefs::deezerApi.name,
                Res.string.pref_fetch_missing_album,
                Res.string.deezer
            ) { title ->
                SwitchPref(
                    text = title,
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
        filteredItem: FilteredItem,
        scrobblerEnabled: Boolean,
        nlsEnabled: Boolean,
        onNavigate: (PanoRoute) -> Unit,
    ) {
        filteredItem(MainPrefs::scrobblerEnabled.name, Res.string.pref_master, null) { title ->
            SwitchPref(
                text = title,
                summary = stringResource(Res.string.pref_offline_info),
                value = scrobblerEnabled && nlsEnabled,
                enabled = nlsEnabled,
                copyToSave = { copy(scrobblerEnabled = it) }
            )
        }
    }

    actual fun updateCheck(
        filteredItem: FilteredItem,
        enabled: Boolean,
        updateProgress: CommonWorkProgress?
    ) {
        if (!DesktopStuff.noUpdateCheck) {
            filteredItem(
                MainPrefs::autoUpdates.name,
                Res.string.pref_notify_updates,
                null
            ) { title ->
                SwitchPref(
                    text = title,
                    value = enabled,
                    copyToSave = {
                        if (!it)
                            UpdaterWork.cancel()
                        else
                            UpdaterWork.schedule(true)

                        copy(autoUpdates = it)
                    }
                )
            }

            filteredItem("check_for_updates", Res.string.pref_check_updates, null) { title ->
                TextPref(
                    text = updateProgress?.message ?: title,
                    enabled = updateProgress == null,
                    onClick = {
                        if (updateProgress == null)
                            UpdaterWork.schedule(true)
                    }
                )
            }
        }
    }
}