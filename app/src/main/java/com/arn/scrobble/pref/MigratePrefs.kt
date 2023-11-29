package com.arn.scrobble.pref

import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.friends.UserAccountSerializable
import com.arn.scrobble.api.AccountType

object MigratePrefs {
    fun migrate(prefs: MainPrefs) {
        if (prefs.prefVersion < 3) {
            migrateV3(prefs)
            prefs.prefVersion = 3
        }
    }

    private fun migrateV3(prefs: MainPrefs) {
        val accounts = mutableListOf<UserAccountSerializable>()
        val accountTypes = mutableSetOf<AccountType>()

        prefs.scrobbleAccounts.forEach {


            var acc = if (it.type == AccountType.LIBREFM && it.apiRoot == null)
                it.copy(apiRoot = Stuff.LIBREFM_API_ROOT)
            else if (it.type == AccountType.CUSTOM_LISTENBRAINZ && it.apiRoot == Stuff.LISTENBRAINZ_API_ROOT)
                it.copy(type = AccountType.LISTENBRAINZ)
            else if (it.type == AccountType.LISTENBRAINZ && it.apiRoot != Stuff.LISTENBRAINZ_API_ROOT)
                it.copy(type = AccountType.CUSTOM_LISTENBRAINZ)
            else
                it

            if (acc.type == AccountType.LISTENBRAINZ && acc.apiRoot == null)
                acc = acc.copy(apiRoot = Stuff.LISTENBRAINZ_API_ROOT)
            else if (acc.type in arrayOf(
                    AccountType.CUSTOM_LISTENBRAINZ,
                    AccountType.GNUFM
                ) && acc.apiRoot == null
            )
                return@forEach

            if (acc.type !in accountTypes)
                accounts += acc

            accountTypes += acc.type
        }

        prefs.scrobbleAccounts = accounts
    }
}