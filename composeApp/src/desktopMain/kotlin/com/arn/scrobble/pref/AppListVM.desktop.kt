package com.arn.scrobble.pref

import com.arn.scrobble.utils.PlatformStuff
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

actual suspend fun AppListVM.load(
    onSetSelectedPackages: (Set<String>) -> Unit,
    onSetAppList: (AppList) -> Unit,
    onSetHasLoaded: () -> Unit,
    checkDefaultApps: Boolean,
) {
    val seenApps = PlatformStuff.mainPrefs.data.map { it.seenApps }.first()

    val musicPlayers = seenApps
        .sortedBy { it.friendlyLabel.lowercase() }

    if (checkDefaultApps)
        onSetSelectedPackages(musicPlayers.map { it.appId }.toSet())

    onSetAppList(AppList(musicPlayers, emptyList()))
    onSetHasLoaded()
}