package com.arn.scrobble.pref

import com.arn.scrobble.utils.DesktopStuff
import com.arn.scrobble.utils.PlatformStuff
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

actual suspend fun AppListVM.load(
    packagesOverride: Set<String>?,
    onSetSelectedPackages: (Set<String>) -> Unit,
    onSetAppList: (AppList) -> Unit,
    onSetHasLoaded: () -> Unit,
    checkDefaultApps: Boolean,
) {
    val seenApps = PlatformStuff.mainPrefs.data.map { it.seenApps }.first()

    if (checkDefaultApps)
        onSetSelectedPackages(seenApps.keys)

    val musicPlayers = seenApps.toList()
        .filter { (appId, friendlyLabel) ->
            packagesOverride?.contains(appId) ?: true
        }
        .sortedBy { (appId, friendlyLabel) ->
            friendlyLabel.lowercase()
        }
        .map { (appId, friendlyLabel) ->
            AppItem(
                appId = appId,
                label = friendlyLabel,
            )
        }

    onSetAppList(AppList(musicPlayers, emptyList()))
    onSetHasLoaded()
}

actual val AppListVM.pluginsNeeded: List<Pair<String, String>>
    get() = if (DesktopStuff.os == DesktopStuff.Os.Windows)
        listOf(
            "MusicBee" to "https://github.com/HenryPDT/mb_MediaControl",
            "foobar2000" to "https://github.com/ungive/foo_mediacontrol",
            "VLC" to "https://github.com/spmn/vlc-win10smtc",
        )
    else
        emptyList()