package com.arn.scrobble.pref

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.media.MediaBrowserServiceCompat
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.utils.AndroidStuff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


actual suspend fun AppListVM.load(
    onSetSelectedPackages: (Set<String>) -> Unit,
    onSetAppList: (AppList) -> Unit,
    onSetHasLoaded: () -> Unit,
    checkDefaultApps: Boolean,
) {
    val packageManager = AndroidStuff.application.packageManager

    val packagesToNotConsider = setOf(
        BuildConfig.APPLICATION_ID,
        "com.android.bluetooth",
        "com.google.android.bluetooth"
    )

    fun Collection<ApplicationInfo>.removeSpam() = filter {
        it.icon != 0 && it.enabled && it.packageName !in packagesToNotConsider
    }

    fun Collection<ApplicationInfo>.sortAndTransform(): List<AppItem> {
        val (selectedList, unselectedList) = this
            .sortedWith(ApplicationInfo.DisplayNameComparator(packageManager))
            .map { AppItem(it.packageName, packageManager.getApplicationLabel(it).toString()) }
            .partition { it.appId in selectedPackages.value }
        return selectedList + unselectedList
    }


    withContext(Dispatchers.IO) {
        val musicPlayers = mutableMapOf<String, ApplicationInfo>()
        val otherApps = mutableMapOf<String, ApplicationInfo>()

        // this matches music players including shazam
        var intent = Intent(MediaBrowserServiceCompat.SERVICE_INTERFACE)

        musicPlayers += packageManager.queryIntentServices(
            intent,
            PackageManager.GET_RESOLVED_FILTER
        ).map { it.serviceInfo.applicationInfo }
            .removeSpam()
            .map { it.packageName to it }

        // this matches the chromecast receiver on pixel tablets and tv
        intent = Intent("com.google.cast.action.BIND").addCategory(Intent.CATEGORY_DEFAULT)

        musicPlayers += packageManager.queryIntentServices(
            intent,
            PackageManager.GET_RESOLVED_FILTER
        ).map { it.serviceInfo.packageName to it.serviceInfo.applicationInfo }

        // this matches pixel now playing including ambient music mod
        intent =
            Intent("com.google.intelligence.sense.NOW_PLAYING_HISTORY").addCategory(Intent.CATEGORY_DEFAULT)

        musicPlayers += packageManager.queryIntentActivities(
            intent,
            PackageManager.GET_RESOLVED_FILTER
        ).map { it.activityInfo.packageName to it.activityInfo.applicationInfo }

        // apps on phone
        intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        otherApps +=
            packageManager.queryIntentActivities(intent, PackageManager.GET_RESOLVED_FILTER)
                .map { it.activityInfo.applicationInfo }
                .removeSpam()
                .map { it.packageName to it }

        // apps on tv
        intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)

        otherApps += packageManager.queryIntentActivities(
            intent,
            PackageManager.GET_RESOLVED_FILTER
        )
            .map { it.activityInfo.applicationInfo }
            .removeSpam()
            .map { it.packageName to it }

        // remove music players from other apps
        musicPlayers.forEach { (key, _) -> otherApps.remove(key) }

        if (checkDefaultApps)
            onSetSelectedPackages(musicPlayers.keys)

        onSetAppList(
            AppList(
                musicPlayers = musicPlayers.values.sortAndTransform(),
                otherApps = otherApps.values.sortAndTransform()
            )
        )

        // add other apps to list

        onSetHasLoaded()
    }
}