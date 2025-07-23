package com.arn.scrobble.onboarding

import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.UserAccountTemp
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.DEEPLINK_PROTOCOL_NAME
import com.arn.scrobble.utils.Stuff.LAST_KEY

object LoginDestinations {
    fun route(accountType: AccountType): PanoRoute = when (accountType) {
        AccountType.LASTFM -> {
            val userAccountTemp = UserAccountTemp(
                AccountType.LASTFM,
                "",
            )

            if (!PlatformStuff.isTv) {
                val url =
                    "https://www.last.fm/api/auth?api_key=$LAST_KEY&cb=$DEEPLINK_PROTOCOL_NAME://auth/lastfm"
                PanoRoute.WebView(
                    url = url,
                    userAccountTemp = userAccountTemp
                )
            } else {
                PanoRoute.OobLastfmLibreFmAuth(
                    userAccountTemp = userAccountTemp,
                )
            }
        }

        AccountType.LIBREFM -> {
            val userAccountTemp = UserAccountTemp(
                AccountType.LIBREFM,
                "",
                Stuff.LIBREFM_API_ROOT
            )

            PanoRoute.OobLastfmLibreFmAuth(
                userAccountTemp = userAccountTemp,
            )
        }

        AccountType.GNUFM -> PanoRoute.LoginGnufm
        AccountType.LISTENBRAINZ -> PanoRoute.LoginListenBrainz
        AccountType.CUSTOM_LISTENBRAINZ -> PanoRoute.LoginCustomListenBrainz
//        AccountType.MALOJA -> PanoRoute.LoginMaloja
        AccountType.PLEROMA -> PanoRoute.LoginPleroma
        AccountType.FILE -> PanoRoute.LoginFile
    }
}