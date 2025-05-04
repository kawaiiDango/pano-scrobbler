package com.arn.scrobble.pref

import com.arn.scrobble.utils.PlatformStuff
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest

actual suspend fun AppListVM.load(
    onSetSelectedPackages: (Set<String>) -> Unit,
    onSetAppList: (AppList) -> Unit,
    onSetHasLoaded: () -> Unit,
    checkDefaultApps: Boolean,
) {
    val packagesToNotConsider = setOf(
        "stuff",
    )

    val seenApps = PlatformStuff.mainPrefs.data.mapLatest { it.seenApps }.first()

    val musicPlayers = seenApps
        .filterNot { it.appId in packagesToNotConsider }
        .sortedBy { it.friendlyLabel }

    if (checkDefaultApps)
        onSetSelectedPackages(musicPlayers.map { it.appId }.toSet())

    onSetAppList(AppList(musicPlayers, emptyList()))
    onSetHasLoaded()
}