package com.arn.scrobble.onboarding

import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.UserAccountTemp
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import io.ktor.http.URLBuilder
import io.ktor.http.set

object LoginDestinations {
    fun route(accountType: AccountType): PanoRoute = when (accountType) {
        AccountType.LASTFM -> {
            val userAccountTemp = UserAccountTemp(
                AccountType.LASTFM,
                "",
            )

            if (!PlatformStuff.isTv) {

                val urlBuilder = URLBuilder("https://www.last.fm/api/auth")
                urlBuilder.set {
                    parameters.append("api_key", Requesters.lastfmUnauthedRequester.apiKey)
                    parameters.append("cb", "${Stuff.DEEPLINK_SCHEME}://auth/lastfm")
                }

                PanoRoute.WebView(
                    url = urlBuilder.buildString(),
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