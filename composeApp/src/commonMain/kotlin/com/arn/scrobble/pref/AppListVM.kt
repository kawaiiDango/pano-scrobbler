package com.arn.scrobble.pref

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.utils.PlatformStuff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppListVM : ViewModel() {
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

    fun load(packagesOverride: Set<String>? = null) {
        if (hasLoaded.value)
            return

        viewModelScope.launch {
            val appListWasRun = PlatformStuff.mainPrefs.data.map { it.appListWasRun }.first()

            load(
                packagesOverride = packagesOverride,
                onSetSelectedPackages = { setSelectedPackages(it) },
                onSetAppList = { _appList.value = it },
                onSetHasLoaded = { _hasLoaded.value = true },
                !appListWasRun
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

    fun filterFn(term: String, appsList: List<AppItem>): List<AppItem> {
        return appsList.filter {
            it.friendlyLabel.split(" ").any {
                it.startsWith(term, ignoreCase = true)
            }
        }
    }

    fun setFilter(searchTerm: String) {
        _searchTerm.value = searchTerm.trim()
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