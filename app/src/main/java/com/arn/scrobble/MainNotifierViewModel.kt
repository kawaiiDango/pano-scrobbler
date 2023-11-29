package com.arn.scrobble

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.api.github.GithubReleases
import com.arn.scrobble.api.github.Updater
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.friends.UserCached
import com.arn.scrobble.api.lastfm.LastFm
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.ui.FabData
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainNotifierViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = App.prefs
    var prevDestinationId: Int? = null
    private var lastDrawerDataRefreshTime = 0L

    private val _drawerData by lazy {
        MutableStateFlow<DrawerData?>(prefs.drawerDataCached)
    }

    val drawerData = _drawerData.asStateFlow()

    private val _canIndex = MutableStateFlow(false)
    val canIndex = _canIndex.asStateFlow()

    private val _fabData = MutableStateFlow<FabData?>(null)
    val fabData = _fabData.asStateFlow()

    private val _editData = MutableSharedFlow<Track>()
    val editData = _editData.asSharedFlow()

    private val _updateAvailable = MutableSharedFlow<GithubReleases>()
    val updateAvailable = _updateAvailable.asSharedFlow()

    lateinit var currentUser: UserCached

    private var prevDrawerUser: UserCached? = null

    val isItChristmas by lazy {
        val cal = Calendar.getInstance()
        BuildConfig.DEBUG ||
                (cal.get(Calendar.MONTH) == Calendar.DECEMBER && cal.get(Calendar.DAY_OF_MONTH) >= 25) ||
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

    fun loadCurrentUserDrawerData() {
        if (
            prevDrawerUser != currentUser ||
            System.currentTimeMillis() - lastDrawerDataRefreshTime > Stuff.RECENTS_REFRESH_INTERVAL
        )
            viewModelScope.launch {
                Scrobblables.current
                    ?.loadDrawerData(currentUser.name)
                    ?.let {
                        _drawerData.emit(it)
                    }
                lastDrawerDataRefreshTime = System.currentTimeMillis()
            }
    }

    override fun onCleared() {
        PanoDb.destroyInstance()
    }

    val destroyEventPending = Semaphore(1)

    fun setFabData(fabData: FabData?) {
        viewModelScope.launch {
            _fabData.emit(fabData)
        }
    }

    fun clearDrawerData() {
        viewModelScope.launch {
            _drawerData.emit(null)
        }
    }

    fun notifyEdit(track: Track) {
        viewModelScope.launch {
            _editData.emit(track)
        }
    }

    // from activity
    fun checkForUpdatesIfNeeded() {
        // check if play store exists
        val hasPlayStore = getApplication<App>()
            .packageManager
            .getLaunchIntentForPackage("com.android.vending") != null

        if (hasPlayStore) {
            prefs.checkForUpdates = null
            return
        } else if (prefs.checkForUpdates == null) {
            prefs.checkForUpdates = true
        }

        viewModelScope.launch {
            _updateAvailable.emitAll(Updater().checkGithubForUpdates())
        }
    }
}