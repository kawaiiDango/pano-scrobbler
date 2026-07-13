package com.arn.scrobble.pref

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.utils.PlatformStuff
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class AppListVM(
    preSelectedPackages: Set<String>,
    packageListOverride: Set<String>?
) : ViewModel() {
    private val _appList = MutableStateFlow(AppList())
    val appList = _appList.asStateFlow()
    private val _searchTerm = MutableStateFlow("")
    val searchTerm = _searchTerm.asStateFlow()
    private val _appListFiltered = _searchTerm
        .combine(appList) { searchTerm, appList ->
            searchTerm to appList
        }.map { (searchTerm, appList) ->
            if (searchTerm.isBlank()) {
                appList
            } else {
                delay(500.milliseconds) // debounce
                AppList(
                    musicPlayers = filterFn(searchTerm, appList.musicPlayers),
                    otherApps = filterFn(searchTerm, appList.otherApps)
                )
            }
        }
    val appListFiltered = _appListFiltered.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        AppList()
    )

    private val _hostnames = MutableStateFlow(emptyList<String>())

    val hostnamesFiltered = _searchTerm
        .combine(_hostnames) { searchTerm, hostnames ->
            searchTerm to hostnames
        }.map { (searchTerm, hostnames) ->
            if (searchTerm.isBlank()) {
                hostnames
            } else {
                delay(500.milliseconds) // debounce
                hostnames.filter {
                    it.split('.').any { part ->
                        part.startsWith(searchTerm, ignoreCase = true)
                    }
                }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            emptyList()
        )

    private val _blockedHostnames = MutableStateFlow(emptySet<String>())
    val blockedHostnames = _blockedHostnames.asStateFlow()

    private val _selectedPackages = MutableStateFlow(preSelectedPackages)
    val selectedPackages = _selectedPackages.asStateFlow()
    private val _hasLoaded = MutableStateFlow(false)
    val hasLoaded = _hasLoaded.asStateFlow()

    init {
        viewModelScope.launch {
            load(
                packagesOverride = packageListOverride,
                onSetAppList = { _appList.value = it },
                onSetHostnames = { _hostnames.value = it },
                onSetBlockedHostnames = { _blockedHostnames.value = it },
                onSetHasLoaded = { _hasLoaded.value = true },
            )
        }
    }

    fun setSingleSelectionAppId(pkg: String) {
        _selectedPackages.value = setOf(pkg)
    }

    fun setMultiSelectionAppId(packageName: String, add: Boolean) {
        _selectedPackages.value = if (add) {
            _selectedPackages.value + packageName
        } else {
            _selectedPackages.value - packageName
        }
    }

    fun setSingleSelectionHostname(hostname: String) {
        _blockedHostnames.value = setOf(hostname)
    }

    fun setMultiSelectionHostname(hostname: String, add: Boolean) {
        _blockedHostnames.value = if (add) {
            _blockedHostnames.value - hostname
        } else {
            _blockedHostnames.value + hostname
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
            val newSeenApps = PlatformStuff.mainPrefs.data.map { it.seenApps }
                .first()
                .filterKeys { it in selectedPackages }

            PlatformStuff.mainPrefs.updateData { prefs ->
                prefs.copy(
                    seenApps = newSeenApps,
                    allowedPackages = prefs.allowedPackages intersect newSeenApps.keys,
                    extractFirstArtistPackages = prefs.extractFirstArtistPackages intersect newSeenApps.keys,
                    blockedPackages = emptySet()
                )
            }

            _appList.value = AppList(
                musicPlayers = _appList.value.musicPlayers.filter { it.appId in newSeenApps },
                otherApps = _appList.value.otherApps.filter { it.appId in newSeenApps }
            )
        }
    }

    fun forgetCheckedHostnames() {
        viewModelScope.launch {
            val newSeenHostnames = PlatformStuff.mainPrefs.data.map { it.seenHostnames }
                .first()
                .filter { it in _blockedHostnames.value }
                .toSet()

            _hostnames.value = newSeenHostnames.toList()
            _blockedHostnames.value = newSeenHostnames

            PlatformStuff.mainPrefs.updateData { prefs ->
                prefs.copy(
                    seenHostnames = newSeenHostnames,
                    blockedHostnames = newSeenHostnames
                )
            }
        }
    }
}

expect suspend fun AppListVM.load(
    packagesOverride: Set<String>?,
    onSetAppList: (AppList) -> Unit,
    onSetHostnames: (List<String>) -> Unit,
    onSetBlockedHostnames: (Set<String>) -> Unit,
    onSetHasLoaded: () -> Unit,
)

expect val AppListVM.pluginsNeeded: List<Pair<String, String>>