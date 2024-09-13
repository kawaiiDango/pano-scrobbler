package com.arn.scrobble.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.github.GithubReleases
import com.arn.scrobble.api.github.Updater
import com.arn.scrobble.api.lastfm.LastFm
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.friends.UserCached
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainNotifierViewModel : ViewModel() {

    var prevDestinationId: Int? = null
    private var lastDrawerDataRefreshTime = 0L
    private val mainPrefs = PlatformStuff.mainPrefs

    val canIndex = mainPrefs.data.map { it.lastMaxIndexTime }.map {
        BuildConfig.DEBUG && Scrobblables.current.value is LastFm &&
                System.currentTimeMillis() - (it ?: 0) > TimeUnit.HOURS.toMillis(12)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    private val _fabData = MutableStateFlow<FabData?>(null)
    val fabData = _fabData.asStateFlow()

    private val _editData = MutableSharedFlow<Track>()
    val editData = _editData.asSharedFlow()

    private val _updateAvailablity = MutableSharedFlow<GithubReleases>()
    val updateAvailability = _updateAvailablity.asSharedFlow()

    private val _currentUser = MutableStateFlow<UserCached?>(null)
    val currentUser get() = _currentUser.value!!

    val drawerData = mainPrefs.data.map { it.drawerData }
        .combine(_currentUser) { drawerData, user ->

            if (user?.isSelf == true) {
                Scrobblables.current.value?.userAccount?.type?.let { type ->
                    drawerData[type]
                }
            } else if (user != null) {
                lastDrawerDataRefreshTime = System.currentTimeMillis()

                Scrobblables.current.value
                    ?.loadDrawerData(user.name)

            } else
                null
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    var pendingSubmitAttempted = false

    val isItChristmas by lazy {
        val cal = Calendar.getInstance()
//        BuildConfig.DEBUG ||
        (cal.get(Calendar.MONTH) == Calendar.DECEMBER && cal.get(Calendar.DAY_OF_MONTH) >= 24) ||
                (cal.get(Calendar.MONTH) == Calendar.JANUARY && cal.get(Calendar.DAY_OF_MONTH) <= 7)
    }

    fun initializeCurrentUser(user: UserCached) {
        if (_currentUser.value == null)
            _currentUser.value = user
    }

    fun setCurrentUser(user: UserCached) {
        _currentUser.value = user
    }

    override fun onCleared() {
//        PanoDb.destroyInstance()
    }

    fun setFabData(fabData: FabData?) {
        viewModelScope.launch {
            _fabData.emit(fabData)
        }
    }

    fun notifyEdit(track: Track) {
        viewModelScope.launch {
            _editData.emit(track)
        }
    }

    // from activity
    private fun checkForUpdates() {
        if (!Stuff.isLoggedIn())
            return

        viewModelScope.launch(Dispatchers.IO) {
            delay(3000)
            val releases = Updater().checkGithubForUpdates() ?: return@launch
            _updateAvailablity.emit(releases)
        }
    }
}