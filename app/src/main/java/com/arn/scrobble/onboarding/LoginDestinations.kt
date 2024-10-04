package com.arn.scrobble.onboarding

import com.arn.scrobble.api.AccountType
import com.arn.scrobble.navigation.PanoRoute

object LoginDestinations {
    fun route(accountType: AccountType): PanoRoute = when (accountType) {
        AccountType.LASTFM -> {
            // todo implement webview
            PanoRoute.Placeholder
//            val arguments = Bundle().apply {
//                putString(Stuff.ARG_URL, Stuff.LASTFM_AUTH_CB_URL)
//                putBoolean(Stuff.ARG_SAVE_COOKIES, true)
//                putSingle(
//                    UserAccountTemp(
//                        AccountType.LASTFM,
//                        "",
//                    )
//                )
//            }
//
//            R.id.webViewFragment to arguments
        }

        AccountType.LIBREFM -> {
            PanoRoute.Placeholder
//            val arguments = Bundle().apply {
//                putString(Stuff.ARG_URL, Stuff.LIBREFM_AUTH_CB_URL)
//                putSingle(
//                    UserAccountTemp(
//                        AccountType.LIBREFM,
//                        "",
//                        Stuff.LIBREFM_API_ROOT
//                    )
//                )
//            }
//
//            R.id.webViewFragment to arguments
        }

        AccountType.GNUFM -> PanoRoute.LoginGnufm
        AccountType.LISTENBRAINZ -> PanoRoute.LoginListenBrainz
        AccountType.CUSTOM_LISTENBRAINZ -> PanoRoute.LoginCustomListenBrainz
        AccountType.MALOJA -> PanoRoute.LoginMaloja
        AccountType.PLEROMA -> PanoRoute.LoginPleroma
        AccountType.FILE -> PanoRoute.LoginFile
    }
}