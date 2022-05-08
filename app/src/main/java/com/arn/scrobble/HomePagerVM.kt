package com.arn.scrobble

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import de.umass.lastfm.User

class HomePagerVM(application: Application) : AndroidViewModel(application) {
    val userInfo = MutableLiveData<User>()

    fun fetchUserInfo(username: String) {
        LFMRequester(getApplication(), viewModelScope, userInfo).getUserInfo(username)
    }
}