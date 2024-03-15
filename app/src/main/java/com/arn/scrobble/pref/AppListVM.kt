package com.arn.scrobble.pref

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media.MediaBrowserServiceCompat
import com.arn.scrobble.main.App
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.R
import com.arn.scrobble.ui.ExpandableHeader
import com.arn.scrobble.ui.SectionWithHeader
import com.arn.scrobble.ui.SectionedVirtualList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppListVM(application: Application) : AndroidViewModel(application) {
    private val _appList = MutableStateFlow(SectionedVirtualList())
    val appList = _appList.asStateFlow()
    private val packageManager = application.packageManager
    val selectedPackages = mutableSetOf<String>()
    private val _hasLoaded = MutableStateFlow(false)
    val hasLoaded = _hasLoaded.asStateFlow()
    private val prefs = App.prefs
    private val packagesToNotConsider = setOf(
        BuildConfig.APPLICATION_ID,
        "com.google.android.bluetooth"
    )

    init {
        viewModelScope.launch {
            load(!prefs.appListWasRun)
        }
    }

    suspend fun load(checkDefaultApps: Boolean) {
        withContext(Dispatchers.IO) {
            val sectionedList = SectionedVirtualList()
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

            sectionedList.addSection(
                SectionWithHeader(
                    AppListSection.MUSIC_PLAYERS,
                    musicPlayers.values.sortApps(),
                    header = ExpandableHeader(
                        R.drawable.vd_play_circle,
                        R.string.music_players,
                        isExpanded = true
                    )
                )
            )

            if (checkDefaultApps)
                selectedPackages += musicPlayers.keys

            // add other apps to list
            sectionedList.addSection(
                SectionWithHeader(
                    AppListSection.OTHERS,
                    otherApps.values.sortApps(),
                    header = ExpandableHeader(
                        R.drawable.vd_apps,
                        R.string.other_apps,
                        isExpanded = true
                    )
                )
            )
            _appList.emit(sectionedList)
            _hasLoaded.emit(true)
        }
    }

    private fun Collection<ApplicationInfo>.removeSpam() = filter {
        it.icon != 0 && it.enabled && it.packageName !in packagesToNotConsider
    }

    private fun Collection<ApplicationInfo>.sortApps(): List<ApplicationInfo> {
        val (selectedList, unselectedList) = this
            .sortedWith(ApplicationInfo.DisplayNameComparator(packageManager))
            .partition { it.packageName in selectedPackages }
        return selectedList + unselectedList
    }

    enum class AppListSection {
        MUSIC_PLAYERS,
        OTHERS
    }
}