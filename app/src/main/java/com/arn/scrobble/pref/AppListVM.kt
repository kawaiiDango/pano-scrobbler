package com.arn.scrobble.pref

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media.MediaBrowserServiceCompat
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.PlatformStuff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppListVM : ViewModel() {
    private val _appList = MutableStateFlow(AppList())
    val appList = _appList.asStateFlow()
    private val packageManager = PlatformStuff.application.packageManager
    private val _selectedPackages = MutableStateFlow(emptySet<String>())
    val selectedPackages = _selectedPackages.asStateFlow()
    private val _hasLoaded = MutableStateFlow(false)
    val hasLoaded = _hasLoaded.asStateFlow()
    private val packagesToNotConsider = setOf(
        BuildConfig.APPLICATION_ID,
        "com.android.bluetooth",
        "com.google.android.bluetooth"
    )

    init {
        viewModelScope.launch {
            val appListWasRun = PlatformStuff.mainPrefs.data.map { it.appListWasRun }.first()
            load(!appListWasRun)

            PlatformStuff.mainPrefs.updateData {
                it.copy(appListWasRun = true)
            }
        }
    }

    suspend fun load(checkDefaultApps: Boolean) {
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
                _selectedPackages.value = musicPlayers.keys

            _appList.value = AppList(
                musicPlayers = musicPlayers.values.sortAndTransform(),
                otherApps = otherApps.values.sortAndTransform()
            )


            // add other apps to list

            _hasLoaded.emit(true)
        }
    }

    private fun Collection<ApplicationInfo>.removeSpam() = filter {
        it.icon != 0 && it.enabled && it.packageName !in packagesToNotConsider
    }

    private fun Collection<ApplicationInfo>.sortAndTransform(): List<AppItem> {
        val (selectedList, unselectedList) = this
            .sortedWith(ApplicationInfo.DisplayNameComparator(packageManager))
            .map { AppItem(it.packageName, packageManager.getApplicationLabel(it).toString()) }
            .partition { it.appId in selectedPackages.value }
        return selectedList + unselectedList
    }

    fun setSelectedPackages(packages: Set<String>) {
        _selectedPackages.value = packages
    }

    fun setMultiSelection(packageName: String, add: Boolean) {
        _selectedPackages.value = if (add) {
            _selectedPackages.value + packageName
        } else {
            _selectedPackages.value - packageName
        }
    }

    fun saveToPrefs() {
        GlobalScope.launch {
            val blockedPackages = appList.value.musicPlayers
                .union(appList.value.otherApps)
                .map { it.appId }
                .toSet() - selectedPackages.value
            //BL = old WL - new WL
//            prefs.blockedPackages =
//                prefs.blockedPackages + prefs.allowedPackages - viewModel.selectedPackages
            // behaviour change: all unselected apps on the list are blocklisted

            PlatformStuff.mainPrefs.updateData {
                it.copy(
                    allowedPackages = selectedPackages.value,
                    blockedPackages = blockedPackages
                )
            }
        }
    }

}