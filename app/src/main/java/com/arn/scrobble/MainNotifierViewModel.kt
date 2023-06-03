package com.arn.scrobble

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.friends.UserSerializable
import com.arn.scrobble.scrobbleable.Lastfm
import com.arn.scrobble.scrobbleable.Scrobblables
import com.arn.scrobble.ui.FabData
import com.hadilq.liveevent.LiveEvent
import de.umass.lastfm.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class MainNotifierViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = App.prefs
    var prevDestinationId: Int? = null
    private var lastDrawerDataRefreshTime = 0L

    val drawerData by lazy {
        MutableLiveData<DrawerData>(
            if (userIsSelf)
                prefs.drawerDataCached
            else
                null
        )
    }

    val canIndex by lazy { MutableLiveData(false) }

    val fabData by lazy { MutableLiveData<FabData>() }

    val editData by lazy { LiveEvent<Track>() }

    lateinit var currentUser: UserSerializable

    private var prevDrawerUser: UserSerializable? = null

    fun updateCanIndex() {
        canIndex.value = BuildConfig.DEBUG && Scrobblables.current is Lastfm &&
                System.currentTimeMillis() -
                (prefs.lastMaxIndexTime ?: 0) > TimeUnit.HOURS.toMillis(12)
    }

    fun initializeCurrentUser(user: UserSerializable) {
        if (!::currentUser.isInitialized)
            currentUser = user
    }

    fun loadCurrentUserDrawerData() {
        if (
            prevDrawerUser != currentUser ||
            System.currentTimeMillis() - lastDrawerDataRefreshTime > Stuff.RECENTS_REFRESH_INTERVAL
        )
            viewModelScope.launch(LFMRequester.ExceptionNotifier()) {
                drawerData.value = withContext(Dispatchers.IO) {
                    Scrobblables.current?.loadDrawerData(currentUser.name)
                }
                lastDrawerDataRefreshTime = System.currentTimeMillis()
            }
    }

    override fun onCleared() {
        PanoDb.destroyInstance()
    }

    val userIsSelf
        get() = ::currentUser.isInitialized && currentUser == Scrobblables.currentScrobblableUser

    val destroyEventPending = Semaphore(1)
}