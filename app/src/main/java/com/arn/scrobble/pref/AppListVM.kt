package com.arn.scrobble.pref

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.App
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.ui.ExpandableHeader
import com.arn.scrobble.ui.SectionWithHeader
import com.arn.scrobble.ui.SectionedVirtualList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppListVM(application: Application) : AndroidViewModel(application) {
    val data = MutableLiveData<SectionedVirtualList>()
    private val packageManager = application.packageManager
    val selectedPackages = mutableSetOf<String>()
    val isLoading = MutableLiveData(true)

    fun load(checkDefaultApps: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val application = getApplication<Application>()
            val sectionedList = SectionedVirtualList()
            val packagesAdded = mutableSetOf(application.packageName)

            var intent = Intent(MediaStore.INTENT_ACTION_MUSIC_PLAYER)
            //the newer intent category doesn't match many players including poweramp
            val musicPlayers = packageManager.queryIntentActivities(intent, 0)
                .map { it.activityInfo.applicationInfo }
                .toMutableList()

            intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

            val launcherApps = packageManager.queryIntentActivities(intent, 0)
                .map { it.activityInfo.applicationInfo }
                .removeSpam()
                .toMutableList()

            val launcherPackagesSet = launcherApps.packagesSet

            intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)

            launcherApps += packageManager.queryIntentActivities(intent, 0)
                .map { it.activityInfo.applicationInfo }
                .removeSpam()
                .filter { it.packageName !in launcherPackagesSet }

            // pixel now playing
            var nowPlayingPackageInfo: PackageInfo? = null
            if (Build.MANUFACTURER.lowercase() == Stuff.MANUFACTURER_GOOGLE) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    nowPlayingPackageInfo = kotlin.runCatching {
                        packageManager.getPackageInfo(Stuff.PACKAGE_PIXEL_NP_R, 0)
                    }.getOrNull()
                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    nowPlayingPackageInfo = kotlin.runCatching {
                        packageManager.getPackageInfo(Stuff.PACKAGE_PIXEL_NP, 0)
                    }.getOrNull()
            }

            if (nowPlayingPackageInfo == null)
                nowPlayingPackageInfo = kotlin.runCatching {
                    packageManager.getPackageInfo(Stuff.PACKAGE_PIXEL_NP_AMM, 0)
                }.getOrNull()

            val ignoreMetaPackages = Stuff.IGNORE_ARTIST_META

            musicPlayers += launcherApps.filter { it.packageName == Stuff.PACKAGE_SHAZAM }
            nowPlayingPackageInfo?.applicationInfo?.let { musicPlayers += it }

            // add music players to list
            musicPlayers.removeAll { it.packageName in packagesAdded }
            packagesAdded += musicPlayers.packagesSet

            musicPlayers += launcherApps.filter { it.packageName in ignoreMetaPackages && it.packageName !in packagesAdded }
            packagesAdded += ignoreMetaPackages

            val browserApps = Stuff.getBrowsers(packageManager)
                .map { it.activityInfo.applicationInfo }
                .filter { it.packageName !in packagesAdded }

            musicPlayers += browserApps
            packagesAdded += browserApps.packagesSet

            sectionedList.addSection(
                SectionWithHeader(
                    AppListSection.MUSIC_PLAYERS,
                    musicPlayers.sortApps(),
                    header = ExpandableHeader(
                        R.drawable.vd_play_circle,
                        R.string.music_players,
                        isExpanded = true
                    )
                )
            )

            if (checkDefaultApps)
                selectedPackages += musicPlayers.packagesSet

            withContext(Dispatchers.Main) {
                data.value = sectionedList
            }

            val systemApps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                packageManager.getInstalledApplications(PackageManager.MATCH_SYSTEM_ONLY)
                    .filter { it.packageName in App.prefs.seenPackages }
            } else {
                emptyList()
            }

            val otherApps = (launcherApps + systemApps).filter { it.packageName !in packagesAdded }

            // add other apps to list
            sectionedList.addSection(
                SectionWithHeader(
                    AppListSection.OTHERS,
                    otherApps.sortApps(),
                    header = ExpandableHeader(
                        R.drawable.vd_apps,
                        R.string.other_apps,
                        isExpanded = true
                    )
                )
            )

            withContext(Dispatchers.Main) {
                isLoading.value = false
                data.value = sectionedList
            }

        }
    }

    private val Collection<ApplicationInfo>.packagesSet
        get() = map { it.packageName }.toSet()

    private fun Collection<ApplicationInfo>.removeSpam() = filter {
        it.icon != 0 && it.enabled
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