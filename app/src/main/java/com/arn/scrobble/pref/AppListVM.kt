package com.arn.scrobble.pref

import android.app.Application
import android.content.Intent
import android.content.pm.ResolveInfo
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
                appList: List<ResolveInfo>,
                checkedByDefault: Boolean,
                @StringRes titleRes: Int,
                @DrawableRes iconRes: Int
            ) {
                val filteredList =
                    appList.filter { it.activityInfo.packageName !in packagesAdded }.sortApps()
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

            addSection(
                AppListAdapter.AppListSection.MUSIC_PLAYERS,
                musicPlayers,
                true,
                R.string.music_players,
                R.drawable.vd_play_circle
            )

            intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

            val launcherApps = packageManager.queryIntentActivities(intent, 0)
                .removeSpam().toMutableList()
            val launcherPackagesSet = launcherApps.packagesSet

            intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)

            launcherApps += packageManager.queryIntentActivities(intent, 0)
                .removeSpam()
                .filter { it.activityInfo.packageName !in launcherPackagesSet }

            val videoApps = Stuff.getBrowsers(packageManager).toMutableList()

            val ignoreMetaPackages = Stuff.IGNORE_ARTIST_META.toSet()

            videoApps += launcherApps.filter { it.activityInfo.packageName in ignoreMetaPackages }

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

    private val List<ResolveInfo>.packagesSet
        get() = map { it.activityInfo.packageName }.toSet()

    private fun List<ResolveInfo>.removeSpam() = filter {
        val ai = it.activityInfo.applicationInfo
        ai.icon != 0 && ai.enabled
    }

    private fun List<ResolveInfo>.sortApps(): List<ResolveInfo> {
        val (selectedList, unselectedList) = this
            .sortedWith(ResolveInfo.DisplayNameComparator(packageManager))
            .partition { it.activityInfo.packageName in selectedPackages }
        return selectedList + unselectedList
    }

}