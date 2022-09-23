package com.arn.scrobble.pref

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
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

            fun addSection(
                enum: AppListAdapter.AppListSection,
                appList: List<ApplicationInfo>,
                checkedByDefault: Boolean,
                @StringRes titleRes: Int,
                @DrawableRes iconRes: Int
            ) {
                val filteredList =
                    appList.filter { it.packageName !in packagesAdded }.sortApps()
                sectionedList.addSection(
                    SectionWithHeader(
                        enum,
                        filteredList,
                        header = ExpandableHeader(application, iconRes, titleRes, isExpanded = true)
                    )
                )
                packagesAdded += filteredList.packagesSet

                if (checkDefaultApps && checkedByDefault)
                    selectedPackages += filteredList.packagesSet
            }

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

            val videoApps = Stuff.getBrowsers(packageManager)
                .map { it.activityInfo.applicationInfo }
                .toMutableList()

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

            val ignoreMetaPackages = Stuff.IGNORE_ARTIST_META.toSet()

            musicPlayers += launcherApps.filter { it.packageName == Stuff.PACKAGE_SHAZAM }
            nowPlayingPackageInfo?.applicationInfo?.let { musicPlayers += it }

            videoApps += launcherApps.filter { it.packageName in ignoreMetaPackages }

            addSection(
                AppListAdapter.AppListSection.MUSIC_PLAYERS,
                musicPlayers,
                true,
                R.string.music_players,
                R.drawable.vd_play_circle
            )

            addSection(
                AppListAdapter.AppListSection.VIDEO_PLAYERS,
                videoApps,
                true,
                R.string.video_players,
                R.drawable.vd_video
            )

            withContext(Dispatchers.Main) {
                data.value = sectionedList
            }

            addSection(
                AppListAdapter.AppListSection.OTHERS,
                launcherApps,
                false,
                R.string.other_apps,
                R.drawable.vd_apps
            )

            withContext(Dispatchers.Main) {
                isLoading.value = false
                data.value = sectionedList
            }

        }
    }

    private val List<ApplicationInfo>.packagesSet
        get() = map { it.packageName }.toSet()

    private fun List<ApplicationInfo>.removeSpam() = filter {
        it.icon != 0 && it.enabled
    }

    private fun List<ApplicationInfo>.sortApps(): List<ApplicationInfo> {
        val (selectedList, unselectedList) = this
            .sortedWith(ApplicationInfo.DisplayNameComparator(packageManager))
            .partition { it.packageName in selectedPackages }
        return selectedList + unselectedList
    }

}