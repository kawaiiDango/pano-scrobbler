package com.arn.scrobble.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.api.UserAccountTemp
import com.arn.scrobble.api.lastfm.LastFm
import com.arn.scrobble.api.pleroma.Pleroma
import com.arn.scrobble.api.pleroma.PleromaOauthClientCreds
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.redactedMessage
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.http.Url
import io.ktor.util.date.GMTDate
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch


class WebViewVM(
    private val userAccountTemp: UserAccountTemp?,
    private val pleromaCreds: PleromaOauthClientCreds?,
) : ViewModel() {
    val loginState = MutableStateFlow<WebViewLoginState>(WebViewLoginState.None)
    val callbackUrlAndCookies =
        MutableSharedFlow<Pair<String, Map<String, String>>>(extraBufferCapacity = 1)

    init {
        platformInit()

        if (userAccountTemp != null) {
            viewModelScope.launch {
                callbackUrlAndCookies.collect { (urlString, cookies) ->
                    val url = Url(urlString)
                    val path = url.segments.lastOrNull() ?: return@collect
                    if (url.protocol.name != Stuff.DEEPLINK_SCHEME) return@collect

                    when (path) {
                        "lastfm" -> {
                            val token = url.parameters["token"] ?: return@collect
                            val httpUrl = Url(Stuff.LASTFM_URL)
                            val cookieObjs = cookies.mapNotNull { (name, value) ->
                                if (name.isNotBlank() && value.isNotBlank()) {
                                    Cookie(
                                        name,
                                        value,
                                        CookieEncoding.RAW,
                                        domain = httpUrl.host,
                                        path = "/",
                                        expires = GMTDate(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30 * 11),
                                        secure = true,
                                    )
                                } else null
                            }

                            launch {
                                LastFm.authAndGetSession(
                                    userAccountTemp.copy(authKey = token),
                                    cookieObjs
                                ).let {
                                    loginFinished(it)
                                }
                            }
                        }

                        "librefm" -> {
                            val token = url.parameters["token"] ?: return@collect

                            launch {
                                LastFm.authAndGetSession(
                                    userAccountTemp.copy(authKey = token),
                                ).let { loginFinished(it) }
                            }
                        }

                        "pleroma" -> {
                            val token = url.parameters["code"] ?: return@collect
                            launch {
                                Pleroma.authAndGetSession(
                                    userAccountTemp.copy(authKey = token),
                                    pleromaCreds!!
                                ).let { loginFinished(it) }

                            }
                        }

                        else -> return@collect
                    }
                    platformClear()
                    loginState.value = WebViewLoginState.Processing
                }
            }
        }
    }

    private fun loginFinished(result: Result<*>) {
        loginState.value =
            if (result.isSuccess)
                WebViewLoginState.Success
            else
                WebViewLoginState.Failed(
                    result.exceptionOrNull()?.redactedMessage ?: "Unknown error"
                )
    }

    fun webViewHelp() {
        loginState.value = WebViewLoginState.Unavailable
    }

    override fun onCleared() {
        // clear cookies
        platformClear()
        super.onCleared()
    }
}

expect fun WebViewVM.platformInit()

expect fun WebViewVM.platformClear()