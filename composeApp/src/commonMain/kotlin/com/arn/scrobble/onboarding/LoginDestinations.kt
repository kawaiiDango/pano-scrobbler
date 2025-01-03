package com.arn.scrobble.onboarding

import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.UserAccountTemp
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.utils.Stuff

object LoginDestinations {
    fun route(accountType: AccountType): PanoRoute = when (accountType) {
        AccountType.LASTFM -> PanoRoute.WebView(
            url = Stuff.LASTFM_AUTH_CB_URL,
            userAccountTemp = UserAccountTemp(
                AccountType.LASTFM,
                "",
            )
        )

        AccountType.LIBREFM -> PanoRoute.WebView(
            url = Stuff.LIBREFM_AUTH_CB_URL,
            userAccountTemp = UserAccountTemp(
                AccountType.LIBREFM,
                "",
                Stuff.LIBREFM_API_ROOT
            )
        )

        AccountType.GNUFM -> PanoRoute.LoginGnufm
        AccountType.LISTENBRAINZ -> PanoRoute.LoginListenBrainz
        AccountType.CUSTOM_LISTENBRAINZ -> PanoRoute.LoginCustomListenBrainz
        AccountType.MALOJA -> PanoRoute.LoginMaloja
        AccountType.PLEROMA -> PanoRoute.LoginPleroma
        AccountType.FILE -> PanoRoute.LoginFile
    }
}