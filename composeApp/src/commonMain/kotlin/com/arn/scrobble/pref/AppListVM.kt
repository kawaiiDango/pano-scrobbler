package com.arn.scrobble.pref

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.utils.PlatformStuff
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppListVM(packagesOverride: Set<String>?) : ViewModel() {
    private val _appList = MutableStateFlow(AppList())
    val appList = _appList.asStateFlow()
    private val _searchTerm = MutableStateFlow("")
    val searchTerm = _searchTerm.asStateFlow()
    private val _appListFiltered = _searchTerm
        .combine(appList) { searchTerm, appList ->
            searchTerm to appList
        }.mapLatest { (searchTerm, appList) ->
            if (searchTerm.isBlank()) {
                appList
            } else {
                delay(500) // debounce
                AppList(
                    musicPlayers = filterFn(searchTerm, appList.musicPlayers),
                    otherApps = filterFn(searchTerm, appList.otherApps)
                )
            }
        }
    val appListFiltered = _appListFiltered.stateIn(
        viewModelScope,
        kotlinx.coroutines.flow.SharingStarted.Lazily,
        AppList()
    )
    private val _selectedPackages = MutableStateFlow(emptySet<String>())
    val selectedPackages = _selectedPackages.asStateFlow()
    private val _hasLoaded = MutableStateFlow(false)
    val hasLoaded = _hasLoaded.asStateFlow()

    init {
        load(packagesOverride)
    }

    private fun load(packagesOverride: Set<String>? = null) {
        viewModelScope.launch {
            load(
                packagesOverride = packagesOverride,
                onSetSelectedPackages = { setSelectedPackages(it) },
                onSetAppList = { _appList.value = it },
                onSetHasLoaded = { _hasLoaded.value = true },
                false
            )
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

    private fun filterFn(term: String, appsList: List<AppItem>): List<AppItem> {
        return appsList.filter {
            it.friendlyLabel.split(" ").any {
                it.startsWith(term, ignoreCase = true)
            }
        }
    }

    fun setFilter(searchTerm: String) {
        _searchTerm.value = searchTerm.trim()
    }

    fun forgetUncheckedApps() {
        viewModelScope.launch {
            val selectedPackages = selectedPackages.value

            PlatformStuff.mainPrefs.updateData { prefs ->
                prefs.copy(
                    seenApps = prefs.seenApps.filterKeys { it in selectedPackages },
                    extractFirstArtistPackages = prefs.extractFirstArtistPackages intersect selectedPackages,
                )
            }

            _appList.value = AppList(
                musicPlayers = _appList.value.musicPlayers.filter { it.appId in selectedPackages },
                otherApps = _appList.value.otherApps.filter { it.appId in selectedPackages }
            )
        }
    }
}

expect suspend fun AppListVM.load(
    packagesOverride: Set<String>?,
    onSetSelectedPackages: (Set<String>) -> Unit,
    onSetAppList: (AppList) -> Unit,
    onSetHasLoaded: () -> Unit,
    checkDefaultApps: Boolean,
)

expect val AppListVM.pluginsNeeded: List<Pair<String, String>>