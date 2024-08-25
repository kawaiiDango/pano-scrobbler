package com.arn.scrobble.onboarding

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.NavDeepLinkBuilder
import com.arn.scrobble.R
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.friends.UserAccountTemp
import com.arn.scrobble.main.App
import com.arn.scrobble.main.MainActivity
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.putSingle

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

        if (navController.graph.findNode(R.id.webViewFragment) != null)
            navController.navigate(R.id.webViewFragment, arguments)
        else // invoked from EditDialog, launch deep link
            NavDeepLinkBuilder(navController.context)
                .setGraph(R.navigation.nav_graph)
                .setComponentName(MainActivity::class.java)
                .setDestination(R.id.webViewFragment)
                .setArguments(arguments)
                .createPendingIntent()
                .send()

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
        val arguments = LoginFragmentArgs(
            loginTitle = App.application.getString(R.string.gnufm),
            textFieldLast = App.application.getString(R.string.password),
            textField1 = App.application.getString(R.string.api_url),
            textField2 = App.application.getString(R.string.username)
        )
            .toBundle()
        navController.navigate(R.id.loginFragment, arguments)
    }

    private fun listenbrainz() {
        val arguments = LoginFragmentArgs(
            loginTitle = App.application.getString(R.string.listenbrainz),
            textFieldLast = App.application.getString(R.string.pref_token_label),
            infoText = App.application.getString(
                R.string.listenbrainz_info,
                "https://listenbrainz.org/profile"
            )
        )
            .toBundle()
        navController.navigate(R.id.loginFragment, arguments)
    }

    private fun customListenbrainz() {
        val arguments = LoginFragmentArgs(
            loginTitle = App.application.getString(R.string.custom_listenbrainz),
            textFieldLast = App.application.getString(R.string.pref_token_label),
            textField1 = App.application.getString(R.string.api_url),
            infoText = App.application.getString(
                R.string.listenbrainz_info,
                "[API_URL]/profile"
            )
        )
            .toBundle()
        navController.navigate(R.id.loginFragment, arguments)
    }

    private fun maloja() {
        val arguments = LoginFragmentArgs(
            loginTitle = App.application.getString(R.string.maloja),
            textFieldLast = App.application.getString(R.string.pref_token_label),
            textField1 = App.application.getString(R.string.server_url)
        )
            .toBundle()
        navController.navigate(R.id.loginFragment, arguments)
    }

    private fun pleroma() {
        val arguments = LoginFragmentArgs(
            loginTitle = App.application.getString(R.string.pleroma),
            textFieldLast = App.application.getString(R.string.server_url),
        )
            .toBundle()
        navController.navigate(R.id.loginFragment, arguments)
    }

    private fun scrobbleToFile() {
        navController.navigate(R.id.loginScrobbleToFile)
    }

    fun acrCloud() {
        val arguments = LoginFragmentArgs(
            App.application.getString(R.string.add_acr_key),
            App.application.getString(R.string.acr_secret),
            infoText = App.application.getString(R.string.add_acr_key_info),
            textField1 = App.application.getString(R.string.acr_host),
            textField2 = App.application.getString(R.string.acr_key),
        )
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
            AccountType.MALOJA -> maloja()
            AccountType.PLEROMA -> pleroma()
            AccountType.FILE -> scrobbleToFile()
        }
    }
}