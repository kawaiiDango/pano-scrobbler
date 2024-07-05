package com.arn.scrobble.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.api.lastfm.LastFm
import com.arn.scrobble.api.pleroma.Pleroma
import com.arn.scrobble.api.pleroma.PleromaOauthClientCreds
import com.arn.scrobble.friends.UserAccountTemp
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class WebViewVM : ViewModel() {
    val callbackProcessed = MutableSharedFlow<Boolean>()

    fun doLastFmAuth(userAccountTemp: UserAccountTemp) {
        viewModelScope.launch {
            LastFm.authAndGetSession(userAccountTemp).isSuccess
                .let { callbackProcessed.emit(it) }
        }
    }

    fun doPleromaAuth(userAccountTemp: UserAccountTemp, oauthClientCreds: PleromaOauthClientCreds) {
        viewModelScope.launch {
            Pleroma.authAndGetSession(userAccountTemp, oauthClientCreds).isSuccess
                .let { callbackProcessed.emit(it) }

        }
    }
}

