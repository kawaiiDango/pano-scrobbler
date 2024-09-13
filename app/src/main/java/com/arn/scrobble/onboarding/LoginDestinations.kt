package com.arn.scrobble.onboarding

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.NavDeepLinkBuilder
import com.arn.scrobble.R
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.friends.UserAccountTemp
import com.arn.scrobble.main.MainActivity
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.putSingle

class LoginDestinations(private val navController: NavController) {

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
//        val arguments = LoginFragmentArgs(
//            loginTitle = PlatformStuff.application.getString(R.string.gnufm),
//            textFieldLast = PlatformStuff.application.getString(R.string.password),
//            textField1 = PlatformStuff.application.getString(R.string.api_url),
//            textField2 = PlatformStuff.application.getString(R.string.username)
//        )
//            .toBundle()
        navController.navigate(R.id.loginGnufm)
    }

    private fun listenbrainz() {
//        val arguments = LoginFragmentArgs(
//            loginTitle = PlatformStuff.application.getString(R.string.listenbrainz),
//            textFieldLast = PlatformStuff.application.getString(R.string.pref_token_label),
//            infoText = PlatformStuff.application.getString(
//                R.string.listenbrainz_info,
//                "https://listenbrainz.org/profile"
//            )
//        )
//            .toBundle()
        navController.navigate(R.id.loginListenBrainz)
    }

    private fun customListenbrainz() {
//        val arguments = LoginFragmentArgs(
//            loginTitle = PlatformStuff.application.getString(R.string.custom_listenbrainz),
//            textFieldLast = PlatformStuff.application.getString(R.string.pref_token_label),
//            textField1 = PlatformStuff.application.getString(R.string.api_url),
//            infoText = PlatformStuff.application.getString(
//                R.string.listenbrainz_info,
//                "[API_URL]/profile"
//            )
//        )
//            .toBundle()
        navController.navigate(R.id.loginCustomListenBrainz)
    }

    private fun maloja() {
//        val arguments = LoginFragmentArgs(
//            loginTitle = PlatformStuff.application.getString(R.string.maloja),
//            textFieldLast = PlatformStuff.application.getString(R.string.pref_token_label),
//            textField1 = PlatformStuff.application.getString(R.string.server_url)
//        )
//            .toBundle()
        navController.navigate(R.id.loginMaloja)
    }

    private fun pleroma() {
//        val arguments = LoginFragmentArgs(
//            loginTitle = PlatformStuff.application.getString(R.string.pleroma),
//            textFieldLast = PlatformStuff.application.getString(R.string.server_url),
//        )
//            .toBundle()
        navController.navigate(R.id.loginPleroma)
    }

    private fun scrobbleToFile() {
        navController.navigate(R.id.loginScrobbleToFile)
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