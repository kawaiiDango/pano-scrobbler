package com.arn.scrobble.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.UserAccountTemp
import com.arn.scrobble.api.lastfm.GnuFm
import com.arn.scrobble.api.lastfm.LastFm
import com.arn.scrobble.api.listenbrainz.ListenBrainz
import com.arn.scrobble.api.maloja.Maloja
import com.arn.scrobble.api.pleroma.Pleroma
import com.arn.scrobble.api.pleroma.PleromaOauthClientCreds
import com.arn.scrobble.utils.Stuff
import io.ktor.http.parseUrl
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.failed_encode_url
import pano_scrobbler.composeapp.generated.resources.required_fields_empty

class LoginViewModel : ViewModel() {
    private val _result = MutableSharedFlow<Result<Unit>>()
    val result = _result.asSharedFlow()
    private val _pleromaCredsResult = MutableSharedFlow<Result<PleromaOauthClientCreds>>()
    val pleromaCredsResult = _pleromaCredsResult.asSharedFlow()

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
                        Result.failure(IllegalArgumentException(getString(Res.string.failed_encode_url)))
                    }
                } else {
                    Result.failure(IllegalArgumentException(getString(Res.string.required_fields_empty)))
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
                    Result.failure(IllegalArgumentException(getString(Res.string.failed_encode_url)))
                }
            } else {
                Result.failure(IllegalArgumentException(getString(Res.string.required_fields_empty)))
            }

            _result.emit(result.map { })
        }
    }

    fun pleromaCreateApp(apiRoot: String, redirectUri: String) {
        viewModelScope.launch {
            var apiRoot = apiRoot
            val result = if (apiRoot.isNotBlank()) {
                if (Stuff.isValidUrl(apiRoot)) {
                    if (!apiRoot.endsWith('/'))
                        apiRoot += '/'

                    Pleroma.createApp(apiRoot, redirectUri)

                } else {
                    Result.failure(IllegalArgumentException(getString(Res.string.failed_encode_url)))
                }
            } else {
                Result.failure(IllegalArgumentException(getString(Res.string.required_fields_empty)))
            }

            _pleromaCredsResult.emit(result)
        }
    }

    fun malojaLogin(apiRoot: String, token: String) {
        viewModelScope.launch {
            var apiRoot = apiRoot
            val result = if (apiRoot.isNotBlank() && token.isNotBlank()) {
                if (parseUrl(apiRoot) != null) {
                    if (!apiRoot.endsWith('/'))
                        apiRoot += '/'

                    val userAccount = UserAccountTemp(
                        AccountType.MALOJA,
                        token,
                        apiRoot,
                    )
                    Maloja.authAndGetSession(userAccount)
                } else {
                    Result.failure(IllegalArgumentException(getString(Res.string.failed_encode_url)))
                }
            } else {
                Result.failure(IllegalArgumentException(getString(Res.string.required_fields_empty)))
            }

            _result.emit(result)
        }
    }

    fun pleromaLogin(
        userAccountTemp: UserAccountTemp,
        creds: PleromaOauthClientCreds,
        token: String,
    ) {
        viewModelScope.launch {
            Pleroma.authAndGetSession(
                userAccountTemp.copy(authKey = token),
                creds
            )
                .map { }
                .let { _result.emit(it) }

        }
    }

    fun lastfmLogin(
        userAccountTemp: UserAccountTemp,
        token: String,
    ) {
        viewModelScope.launch {
            LastFm.authAndGetSession(
                userAccountTemp.copy(authKey = token)
            ).map { }
                .let { _result.emit(it) }
        }
    }
}