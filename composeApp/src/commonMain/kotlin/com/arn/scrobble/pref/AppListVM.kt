package com.arn.scrobble.pref

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.utils.PlatformStuff
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class AppListVM : ViewModel() {
    private val _appList = MutableStateFlow(AppList())
    val appList = _appList.asStateFlow()
    private val _selectedPackages = MutableStateFlow(emptySet<String>())
    val selectedPackages = _selectedPackages.asStateFlow()
    private val _hasLoaded = MutableStateFlow(false)
    val hasLoaded = _hasLoaded.asStateFlow()

    init {
        viewModelScope.launch {
            val appListWasRun = PlatformStuff.mainPrefs.data.map { it.appListWasRun }.first()
            load(
                onSetSelectedPackages = { setSelectedPackages(it) },
                onSetAppList = { _appList.value = it },
                onSetHasLoaded = { _hasLoaded.value = true },
                !appListWasRun
            )

            PlatformStuff.mainPrefs.updateData {
                it.copy(appListWasRun = true)
            }
        }
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

}

expect suspend fun AppListVM.load(
    onSetSelectedPackages: (Set<String>) -> Unit,
    onSetAppList: (AppList) -> Unit,
    onSetHasLoaded: () -> Unit,
    checkDefaultApps: Boolean,
)

expect val AppListVM.pluginsNeeded: List<Pair<String, String>>