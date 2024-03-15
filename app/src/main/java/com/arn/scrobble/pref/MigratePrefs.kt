package com.arn.scrobble.pref

import com.arn.scrobble.R
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.lastfm.LastfmUnscrobbler
import com.arn.scrobble.friends.UserAccountSerializable
import com.arn.scrobble.main.App
import com.arn.scrobble.utils.Stuff
import com.franmontiel.persistentcookiejar.persistence.SerializableCookie
import com.frybits.harmony.getHarmonySharedPreferences
import io.ktor.http.Url
import kotlinx.coroutines.runBlocking


object MigratePrefs {
    fun migrate(prefs: MainPrefs) {
        // fresh installation
        if (prefs.prefVersion == 0) {
            prefs.prefVersion = MainPrefs.PREF_VERSION_INT
            // do not display changelog for fresh installations
            prefs.changelogSeenHashcode = App.context.getString(R.string.changelog_text).hashCode()
            return
        }

        if (prefs.prefVersion < 3) {
            migrateV3(prefs)
            prefs.prefVersion = 3
        }

        if (prefs.prefVersion < 4) {
            migrateV4()
            prefs.prefVersion = 4
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

    private fun migrateV4() {

        // migrate old cookies
        val prefs = App.context.getHarmonySharedPreferences("CookiePersistence")
        prefs.all.forEach { (key, value) ->
            if ("|sessionid" in key || "|csrftoken" in key) {
                val cookie = SerializableCookie.decode(value as String)
                if (cookie != null)
                    runBlocking {
                        LastfmUnscrobbler.cookieStorage.addCookie(
                            Url("https://" + cookie.domain!!),
                            cookie
                        )
                    }
            }
        }

        // clear old cookies
        prefs.edit().clear().apply()
    }

}