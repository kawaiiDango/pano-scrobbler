package com.arn.scrobble

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.arn.scrobble.friends.UserSerializable
import com.arn.scrobble.pref.MainPrefs
import com.hadilq.liveevent.LiveEvent
import de.umass.lastfm.Track

class MainNotifierViewModel(application: Application) : AndroidViewModel(application) {

    val prefs = MainPrefs(application)
    val drawerData by lazy { MutableLiveData(DrawerData.loadFromPref(application)) }
    val editData by lazy { LiveEvent<Track>() }
    var backButtonEnabled = true
    private val userStack by lazy {
        ArrayDeque<UserSerializable>().also { deq ->
            prefs.currentUser
                ?.copy(sessionKey = null)
                ?.let { deq.addFirst(it) }
        }
    }
    val trackBundleLd = MutableLiveData<Bundle>()

    fun pushUser(user: UserSerializable) {
        userStack.addFirst(user)
    }

    fun popUser() = userStack.removeFirst()

    fun peekUser() = userStack.first()

    val userIsSelf
        get() = peekUser().name == prefs.lastfmUsername

    val userStackDepth
        get() = userStack.size
}