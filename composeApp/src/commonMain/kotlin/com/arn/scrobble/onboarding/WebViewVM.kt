package com.arn.scrobble.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.api.UserAccountTemp
import com.arn.scrobble.api.lastfm.LastFm
import com.arn.scrobble.api.lastfm.LastfmUnscrobbler
import com.arn.scrobble.api.pleroma.Pleroma
import com.arn.scrobble.api.pleroma.PleromaOauthClientCreds
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.http.Url
import io.ktor.util.date.GMTDate
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

    fun handleCallbackUrl(
        url: Url,
        userAccountTemp: UserAccountTemp,
        creds: PleromaOauthClientCreds?,
    ): Boolean {
        val path = url.segments.lastOrNull() ?: return false
        if (url.protocol.name != Stuff.DEEPLINK_PROTOCOL_NAME) return false

        when (path) {
            "lastfm" -> {
                val token = url.parameters["token"] ?: return false
                val httpUrlString = "https://www.last.fm/"
                val httpUrl = Url(httpUrlString)
                viewModelScope.launch {
                    PlatformStuff.getWebviewCookies(httpUrlString)
                        .forEach { (name, value) ->
                            if (name.isNotBlank() && value.isNotBlank()) {
                                val cookie = Cookie(
                                    name,
                                    value,
                                    CookieEncoding.RAW,
                                    domain = httpUrl.host,
                                    path = "/",
                                    expires = GMTDate(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30 * 11),
                                    secure = true,
                                )
                                LastfmUnscrobbler.cookieStorage.addCookie(httpUrl, cookie)

                            } else {
                                null
                            }
                        }
                }
                doLastFmAuth(
                    userAccountTemp.copy(authKey = token)
                )
            }

            "librefm" -> {
                val token = url.parameters["token"] ?: return false
                doLastFmAuth(
                    userAccountTemp.copy(authKey = token)
                )
            }

            "pleroma" -> {
                val token = url.parameters["code"] ?: return false
                doPleromaAuth(
                    userAccountTemp.copy(authKey = token),
                    creds!!
                )
            }

            else -> return false
        }
        return true
    }

    override fun onCleared() {
        // clear cookies
        PlatformStuff.clearWebviewCookies()
        super.onCleared()
    }
}