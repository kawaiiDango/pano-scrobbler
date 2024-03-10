package com.arn.scrobble.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.lastfm.LastFm
import com.arn.scrobble.api.pleroma.Pleroma
import com.arn.scrobble.friends.UserAccountTemp
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class WebViewVM : ViewModel() {
    val callbackProcessed = MutableSharedFlow<Boolean>()

    fun doAuth(userAccountTemp: UserAccountTemp) {
        viewModelScope.launch {

            when (userAccountTemp.type) {
                AccountType.LASTFM,
                AccountType.LIBREFM -> {
                    LastFm.authAndGetSession(userAccountTemp).isSuccess
                        .let { callbackProcessed.emit(it) }
                }

                AccountType.PLEROMA -> {
                    Pleroma.authAndGetSession(userAccountTemp).isSuccess
                        .let { callbackProcessed.emit(it) }
                }

                else -> {
                    throw IllegalArgumentException("Account type not supported: ${userAccountTemp.type}")
                }

            }
        }
    }
}
