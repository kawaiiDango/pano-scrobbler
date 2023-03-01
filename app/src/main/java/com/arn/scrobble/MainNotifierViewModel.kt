package com.arn.scrobble

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.friends.UserSerializable
import com.arn.scrobble.pending.PendingScrService
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.scrobbleable.Scrobblables
import com.arn.scrobble.ui.FabData
import com.hadilq.liveevent.LiveEvent
import de.umass.lastfm.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext

class MainNotifierViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = MainPrefs(application)
    var prevDestinationId: Int? = null
    var prevBackQueueSize = 0
    private var lastDrawerDataRefreshTime = 0L

    val drawerData by lazy {
        MutableLiveData<DrawerData>(
            if (userIsSelf)
                prefs.drawerDataCached
            else
                null
        )
    }

    val fabData by lazy { MutableLiveData<FabData>() }

    val editData by lazy { LiveEvent<Track>() }

    fun loadCurrentUserDrawerData() {
        if (System.currentTimeMillis() - lastDrawerDataRefreshTime > Stuff.RECENTS_REFRESH_INTERVAL)
            viewModelScope.launch(LFMRequester.ExceptionNotifier(timberLog = false)) {
                drawerData.value = withContext(Dispatchers.IO) {
                    Scrobblables.current?.loadDrawerData(currentUser.name)
                }
                lastDrawerDataRefreshTime = System.currentTimeMillis()
            }
    }

    private val userStack by lazy {
        ArrayDeque<UserSerializable>().also { deq ->
            Scrobblables.current?.userAccount
                ?.user
                ?.let { deq.addFirst(it) }
        }
    }

    override fun onCleared() {
        if (!PendingScrService.mightBeRunning)
            PanoDb.destroyInstance()
    }

    var lastNavHeaderRefreshTime = 0L

    fun pushUser(user: UserSerializable) {
        userStack.addFirst(user)
    }

    fun popUser() = userStack.removeFirst()

    val currentUser get() = userStack.first()

    val userIsSelf
        get() = userStack.firstOrNull() == Scrobblables.current?.userAccount?.user

    val userStackDepth
        get() = userStack.size

    val destroyEventPending = Semaphore(1)
}