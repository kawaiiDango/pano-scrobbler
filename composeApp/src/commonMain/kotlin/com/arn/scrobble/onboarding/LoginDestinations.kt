package com.arn.scrobble.onboarding

import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.UserAccountTemp
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.DEEPLINK_PROTOCOL_NAME
import com.arn.scrobble.utils.Stuff.LAST_KEY
import com.arn.scrobble.utils.Stuff.LIBREFM_KEY

object LoginDestinations {
    fun route(accountType: AccountType): PanoRoute = when (accountType) {
        AccountType.LASTFM -> PanoRoute.WebView(
            url = "https://www.last.fm/api/auth?api_key=$LAST_KEY&cb=$DEEPLINK_PROTOCOL_NAME://auth/lastfm",
            userAccountTemp = UserAccountTemp(
                AccountType.LASTFM,
                "",
            )
        )

        AccountType.LIBREFM -> {
            val userAccountTemp = UserAccountTemp(
                AccountType.LIBREFM,
                "",
                Stuff.LIBREFM_API_ROOT
            )
            if (PlatformStuff.isTv) {
                PanoRoute.WebView(
                    url = "https://libre.fm/api/auth/?api_key=$LIBREFM_KEY&cb=$DEEPLINK_PROTOCOL_NAME://auth/librefm",
                    userAccountTemp = userAccountTemp
                )
            } else {
                PanoRoute.OobLibreFmAuth(
                    userAccountTemp = userAccountTemp,
                )
            }
        }

        AccountType.GNUFM -> PanoRoute.LoginGnufm
        AccountType.LISTENBRAINZ -> PanoRoute.LoginListenBrainz
        AccountType.CUSTOM_LISTENBRAINZ -> PanoRoute.LoginCustomListenBrainz
//        AccountType.MALOJA -> PanoRoute.LoginMaloja
        AccountType.PLEROMA -> PanoRoute.LoginPleroma
        AccountType.FILE -> PanoRoute.LoginFile
    }
}