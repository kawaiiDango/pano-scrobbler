package com.arn.scrobble.pref

import com.arn.scrobble.utils.DesktopStuff
import com.arn.scrobble.utils.PlatformStuff
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

actual suspend fun AppListVM.load(
    packagesOverride: Set<String>?,
    onSetAppList: (AppList) -> Unit,
    onSetHasLoaded: () -> Unit,
) {
    val seenApps = PlatformStuff.mainPrefs.data.map { it.seenApps }.first()

    val (selectedList, unselectedList) = seenApps.asSequence()
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
        .partition { it.appId in selectedPackages.value }

    val musicPlayers = selectedList + unselectedList
    onSetAppList(AppList(musicPlayers, emptyList()))
    onSetHasLoaded()
}

actual val AppListVM.pluginsNeeded: List<Pair<String, String>>
    get() = if (DesktopStuff.os == DesktopStuff.Os.Windows)
        listOf(
            "AIMP (plugin does not report albums)" to "https://www.aimp.ru/forum/index.php?topic=63341",
            "foobar2000" to "https://github.com/ungive/foo_mediacontrol",
//            "foobar2000" to "https://github.com/kawaiiDango/foo_mediacontrol",
            "iTunes" to "https://github.com/thewizrd/iTunes-SMTC",
            "MusicBee" to "https://github.com/HenryPDT/mb_MediaControl",
            "VLC" to "https://github.com/spmn/vlc-win10smtc",
            "Winamp (Classic)" to "https://github.com/NanMetal/gen_smtc",
        )
    else
        emptyList()