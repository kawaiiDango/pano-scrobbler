package com.arn.scrobble.onboarding

import android.webkit.URLUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.lastfm.GnuFm
import com.arn.scrobble.api.listenbrainz.ListenBrainz
import com.arn.scrobble.api.maloja.Maloja
import com.arn.scrobble.api.pleroma.Pleroma
import com.arn.scrobble.api.pleroma.PleromaOauthClientCreds
import com.arn.scrobble.friends.UserAccountTemp
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val _result = MutableSharedFlow<Result<Unit>>()
    val result = _result.asSharedFlow()
    private val _pleromaCredsResult = MutableSharedFlow<Result<PleromaOauthClientCreds>>()
    val pleromaCredsResult = _pleromaCredsResult.asSharedFlow()
    private val context = PlatformStuff.application

    fun gnufmLogin(
        apiRoot: String,
        username: String,
        password: String,
    ) {
        viewModelScope.launch {
            val result =
                if (apiRoot.isNotBlank() && username.isNotBlank() && password.isNotBlank()) {
                    // test login
                    if (apiRoot.startsWith("test_creds_")) {
                        Stuff.addTestCreds(
                            apiRoot.substringAfter("test_creds_"),
                            username,
                            password
                        )
                        Result.success(Unit)
                    } else if (Stuff.isValidUrl(apiRoot)) {
                        var apiRoot = apiRoot
                        if (!apiRoot.endsWith('/'))
                            apiRoot += '/'
                        if (!apiRoot.endsWith("2.0/"))
                            apiRoot += "2.0/"

                        GnuFm.authAndGetSession(apiRoot, username, password)
                            .map { }
                    } else {
                        Result.failure(IllegalArgumentException(context.getString(R.string.failed_encode_url)))
                    }
                } else {
                    Result.failure(IllegalArgumentException(context.getString(R.string.required_fields_empty)))
                }
            _result.emit(result)
        }
    }

    fun listenBrainzLogin(
        apiRoot: String = Stuff.LISTENBRAINZ_API_ROOT,
        token: String,
    ) {
        viewModelScope.launch {
            val result = if (apiRoot.isNotBlank() && token.isNotBlank()) {
                if (Stuff.isValidUrl(apiRoot)) {
                    var apiRoot = apiRoot
                    if (!apiRoot.endsWith('/'))
                        apiRoot = "$apiRoot/"

                    val userAccount = UserAccountTemp(
                        if (apiRoot == Stuff.LISTENBRAINZ_API_ROOT) {
                            AccountType.LISTENBRAINZ
                        } else {
                            AccountType.CUSTOM_LISTENBRAINZ
                        },
                        token,
                        apiRoot,
                    )
                    ListenBrainz.authAndGetSession(userAccount)
                } else {
                    Result.failure(IllegalArgumentException(context.getString(R.string.failed_encode_url)))
                }
            } else {
                Result.failure(IllegalArgumentException(context.getString(R.string.required_fields_empty)))
            }

            _result.emit(result.map { })
        }
    }

    fun pleromaCreateApp(apiRoot: String) {
        viewModelScope.launch {
            var apiRoot = apiRoot
            val result = if (apiRoot.isNotBlank()) {
                if (Stuff.isValidUrl(apiRoot)) {
                    if (!apiRoot.endsWith('/'))
                        apiRoot += '/'

                    Pleroma.createApp(apiRoot)


                } else {
                    Result.failure(IllegalArgumentException(context.getString(R.string.failed_encode_url)))
                }
            } else {
                Result.failure(IllegalArgumentException(context.getString(R.string.required_fields_empty)))
            }

            _pleromaCredsResult.emit(result)
        }
    }

    fun malojaLogin(apiRoot: String, token: String) {
        viewModelScope.launch {
            var apiRoot = apiRoot
            val result = if (apiRoot.isNotBlank() && token.isNotBlank()) {
                if (URLUtil.isValidUrl(apiRoot)) {
                    if (!apiRoot.endsWith('/'))
                        apiRoot += '/'

                    val userAccount = UserAccountTemp(
                        AccountType.MALOJA,
                        token,
                        apiRoot,
                    )
                    Maloja.authAndGetSession(userAccount)
                } else {
                    Result.failure(IllegalArgumentException(context.getString(R.string.failed_encode_url)))
                }
            } else {
                Result.failure(IllegalArgumentException(context.getString(R.string.required_fields_empty)))
            }

            _result.emit(result)
        }
    }
}