package com.arn.scrobble.scrobbleable

import android.os.Bundle
import androidx.navigation.NavController
import com.arn.scrobble.App
import com.arn.scrobble.LoginFragmentArgs
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.putSingle
import com.arn.scrobble.friends.UserAccountTemp

class LoginFlows(private val navController: NavController) {

    private fun lastfm() {
        val arguments = Bundle().apply {
            putString(Stuff.ARG_URL, Stuff.LASTFM_AUTH_CB_URL)
            putBoolean(Stuff.ARG_SAVE_COOKIES, true)
            putSingle(
                UserAccountTemp(
                    AccountType.LASTFM,
                    "",
                )
            )
        }
        navController.navigate(R.id.webViewFragment, arguments)
    }

    private fun librefm() {
        val arguments = Bundle().apply {
            putString(Stuff.ARG_URL, Stuff.LIBREFM_AUTH_CB_URL)
            putSingle(
                UserAccountTemp(
                    AccountType.LIBREFM,
                    "",
                    Stuff.LIBREFM_API_ROOT
                )
            )
        }
        navController.navigate(R.id.webViewFragment, arguments)
    }

    private fun gnufm() {
        val arguments = LoginFragmentArgs.Builder(
            App.context.getString(R.string.gnufm),
            App.context.getString(R.string.api_url),
        ).apply {
            textCheckbox = App.context.getString(R.string.disable_tls_verify)
        }
            .build()
            .toBundle()
        navController.navigate(R.id.loginFragment, arguments)
    }

    private fun listenbrainz() {
        val arguments = LoginFragmentArgs.Builder(
            App.context.getString(R.string.listenbrainz),
            App.context.getString(R.string.pref_token_label),
        ).apply {
            infoText = App.context.getString(
                R.string.listenbrainz_info,
                "https://listenbrainz.org/profile"
            )
        }
            .build()
            .toBundle()
        navController.navigate(R.id.loginFragment, arguments)
    }

    private fun customListenbrainz() {
        val arguments = LoginFragmentArgs.Builder(
            App.context.getString(R.string.custom_listenbrainz),
            App.context.getString(R.string.pref_token_label),
        ).apply {
            textField1 = App.context.getString(R.string.api_url)
            textCheckbox = App.context.getString(R.string.disable_tls_verify)
            infoText = App.context.getString(
                R.string.listenbrainz_info,
                "[API_URL]/profile"
            )
        }
            .build()
            .toBundle()
        navController.navigate(R.id.loginFragment, arguments)
    }

    fun acrCloud() {
        val arguments = LoginFragmentArgs.Builder(
            App.context.getString(R.string.add_acr_key),
            App.context.getString(R.string.acr_secret)
        ).apply {
            infoText = App.context.getString(R.string.add_acr_key_info)
            textField1 = App.context.getString(R.string.acr_host)
            textField2 = App.context.getString(R.string.acr_key)
        }
            .build()
            .toBundle()
        navController.navigate(R.id.loginFragment, arguments)
    }


    fun go(accountType: AccountType) {
        when (accountType) {
            AccountType.LASTFM -> lastfm()
            AccountType.LIBREFM -> librefm()
            AccountType.GNUFM -> gnufm()
            AccountType.LISTENBRAINZ -> listenbrainz()
            AccountType.CUSTOM_LISTENBRAINZ -> customListenbrainz()
        }
    }
}