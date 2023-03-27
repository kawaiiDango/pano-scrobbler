package com.arn.scrobble

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hadilq.liveevent.LiveEvent
import de.umass.lastfm.User

class UserLoaderVM : ViewModel() {
    val userInfo = LiveEvent<User>()
    fun fetchUserInfo(username: String) {
        LFMRequester(viewModelScope, userInfo).getUserInfo(username)
    }

}