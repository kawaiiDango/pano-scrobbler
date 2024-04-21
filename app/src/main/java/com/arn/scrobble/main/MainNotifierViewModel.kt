package com.arn.scrobble.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.BuildConfig
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
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainNotifierViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = App.prefs
    var prevDestinationId: Int? = null
    private var lastDrawerDataRefreshTime = 0L

    private val _drawerData by lazy {
        MutableStateFlow(Scrobblables.current?.userAccount?.type?.let { prefs.drawerData[it] })
    }

    val drawerData = _drawerData.asStateFlow()

    private val _canIndex = MutableStateFlow(false)
    val canIndex = _canIndex.asStateFlow()

    private val _fabData = MutableStateFlow<FabData?>(null)
    val fabData = _fabData.asStateFlow()

    private val _editData = MutableSharedFlow<Track>()
    val editData = _editData.asSharedFlow()

    private val _updateAvailablity = MutableSharedFlow<GithubReleases>()
    val updateAvailability = _updateAvailablity.asSharedFlow()

    lateinit var currentUser: UserCached
        private set

    private var prevDrawerUser: UserCached? = null

    var pendingSubmitAttempted = false

    val isItChristmas by lazy {
        val cal = Calendar.getInstance()
//        BuildConfig.DEBUG ||
        (cal.get(Calendar.MONTH) == Calendar.DECEMBER && cal.get(Calendar.DAY_OF_MONTH) >= 24) ||
                (cal.get(Calendar.MONTH) == Calendar.JANUARY && cal.get(Calendar.DAY_OF_MONTH) <= 7)
    }

    fun updateCanIndex() {
        _canIndex.value = BuildConfig.DEBUG && Scrobblables.current is LastFm &&
                System.currentTimeMillis() -
                (prefs.lastMaxIndexTime ?: 0) > TimeUnit.HOURS.toMillis(12)
    }

    fun initializeCurrentUser(user: UserCached) {
        if (!::currentUser.isInitialized)
            currentUser = user
    }

    fun setCurrentUser(user: UserCached) {
        currentUser = user
    }

    fun loadCurrentUserDrawerData() {
        if (
            prevDrawerUser != currentUser ||
            System.currentTimeMillis() - lastDrawerDataRefreshTime > Stuff.RECENTS_REFRESH_INTERVAL
        ) {

            viewModelScope.launch {
                if (currentUser.isSelf)
                    loadDrawerDataCached()
                Scrobblables.current
                    ?.loadDrawerData(currentUser.name)
                    ?.let {
                        _drawerData.emit(it)
                    }
                lastDrawerDataRefreshTime = System.currentTimeMillis()
            }
        }
    }

    override fun onCleared() {
//        PanoDb.destroyInstance()
    }

    fun setFabData(fabData: FabData?) {
        viewModelScope.launch {
            _fabData.emit(fabData)
        }
    }

    fun loadDrawerDataCached() {
        _drawerData.value = Scrobblables.current?.userAccount?.type?.let { prefs.drawerData[it] }
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