package com.arn.scrobble.pref

import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

actual suspend fun AppListVM.load(
    onSetSelectedPackages: (Set<String>) -> Unit,
    onSetAppList: (AppList) -> Unit,
    onSetHasLoaded: () -> Unit,
    checkDefaultApps: Boolean,
) {
    val seenApps = PlatformStuff.mainPrefs.data.map { it.seenApps }.first()

    if (checkDefaultApps)
        onSetSelectedPackages(seenApps.keys)

    val musicPlayers = seenApps.toList()
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

actual fun AppListVM.pluginUrl(appItem: AppItem): String? {
    return when {
        appItem.appId.startsWith("MusicBee") ||
                appItem.label == "MusicBee" -> {
            Stuff.MUSICBEE_PLUGIN_URL
        }

        appItem.appId == "foobar2000.exe" ||
                appItem.label == "foobar2000" -> {
            Stuff.FOOBAR_PLUGIN_URL
        }

        else -> {
            null
        }
    }
}