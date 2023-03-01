package com.arn.scrobble

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hadilq.liveevent.LiveEvent
import de.umass.lastfm.User

class UserLoaderVM(application: Application) : AndroidViewModel(application) {
    val userInfo = LiveEvent<User>()
    fun fetchUserInfo(username: String) {
        LFMRequester(viewModelScope, userInfo).getUserInfo(username)
    }

}