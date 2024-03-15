package com.arn.scrobble.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.LastFm
import com.arn.scrobble.api.lastfm.User
import com.arn.scrobble.utils.Stuff.doOnSuccessLoggingFaliure
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class UserLoaderVM : ViewModel() {
    private val _userInfo = MutableSharedFlow<User>()
    val userInfo = _userInfo.asSharedFlow()

    fun fetchUserInfo(username: String) {
        viewModelScope.launch {
            (Scrobblables.current as? LastFm)
                ?.userGetInfo(username)
                ?.doOnSuccessLoggingFaliure {
                    _userInfo.emit(it)
                }
        }
    }

}