package com.arn.scrobble.pref

import com.arn.scrobble.friends.UserAccountSerializable
import com.arn.scrobble.friends.UserSerializable
import com.arn.scrobble.scrobbleable.ScrobblableEnum
import de.umass.lastfm.ImageSize

object MigratePrefs {
    fun migrate(prefs: MainPrefs) {
        if (prefs.prefVersion < 2)
            migrateV2(prefs)
    }

    fun migrateV2(prefs: MainPrefs) {
        val sharedPreferences = prefs.sharedPreferences
        val lastfmUsername = sharedPreferences.getString(MainPrefs.PREF_LASTFM_USERNAME, null)
        val lastfmSessKey = sharedPreferences.getString(MainPrefs.PREF_LASTFM_SESS_KEY, null)
        val scrobblingSince =
            sharedPreferences.getLong(MainPrefs.PREF_ACTIVITY_SCROBBLING_SINCE, -1)
        val profilePicUrlCached =
            sharedPreferences.getString(MainPrefs.PREF_ACTIVITY_PROFILE_PIC, null)
        val librefmSessKey = sharedPreferences.getString(MainPrefs.PREF_LIBREFM_SESS_KEY, null)
        val librefmUsername = sharedPreferences.getString(MainPrefs.PREF_LIBREFM_USERNAME, null)
        val gnufmSessKey = sharedPreferences.getString(MainPrefs.PREF_GNUFM_SESS_KEY, null)
        val gnufmUsername = sharedPreferences.getString(MainPrefs.PREF_GNUFM_USERNAME, null)
        val gnufmRoot = sharedPreferences.getString(MainPrefs.PREF_GNUFM_ROOT, null)
        val gnufmTlsNoVerify =
            sharedPreferences.getBoolean(MainPrefs.PREF_GNUFM_TLS_NO_VERIFY, false)
        val listenbrainzToken = sharedPreferences.getString(MainPrefs.PREF_LISTENBRAINZ_TOKEN, null)
        val listenbrainzUsername =
            sharedPreferences.getString(MainPrefs.PREF_LISTENBRAINZ_USERNAME, null)
        val customListenbrainzToken =
            sharedPreferences.getString(MainPrefs.PREF_LB_CUSTOM_TOKEN, null)
        val customListenbrainzUsername =
            sharedPreferences.getString(MainPrefs.PREF_LB_CUSTOM_USERNAME, null)
        val customListenbrainzRoot =
            sharedPreferences.getString(MainPrefs.PREF_LB_CUSTOM_ROOT, null)
        val customListenbrainzTlsNoVerify =
            sharedPreferences.getBoolean(MainPrefs.PREF_LB_CUSTOM_TLS_NO_VERIFY, false)


        if (lastfmSessKey != null && lastfmUsername != null) {
            val scrobbleAccounts = mutableListOf<UserAccountSerializable>()

            // lastfm
            var user = UserSerializable(
                lastfmUsername,
                "https://last.fm/user/$lastfmUsername",
                lastfmUsername,
                "",
                scrobblingSince,
                mapOf(
                    ImageSize.MEDIUM to (profilePicUrlCached ?: ""),
                    ImageSize.LARGE to (profilePicUrlCached ?: ""),
                    ImageSize.EXTRALARGE to (profilePicUrlCached ?: ""),
                )
            )
            var userAccountSerializable = UserAccountSerializable(
                ScrobblableEnum.LASTFM,
                user,
                lastfmSessKey,
            )
            prefs.currentUser = userAccountSerializable
            scrobbleAccounts += userAccountSerializable


            // librefm
            if (librefmSessKey != null && librefmUsername != null) {
                user = UserSerializable(
                    librefmUsername,
                    "https://www.libre.fm/user/$librefmUsername",
                    librefmUsername,
                    "",
                    -1,
                    mapOf()
                )
                userAccountSerializable = UserAccountSerializable(
                    ScrobblableEnum.LIBREFM,
                    user,
                    librefmSessKey,
                )
                scrobbleAccounts += userAccountSerializable
            }


            // GNUFM
            if (gnufmSessKey != null && gnufmUsername != null) {
                user = UserSerializable(
                    gnufmUsername,
                    "$gnufmRoot/user/$gnufmUsername",
                    gnufmUsername,
                    "",
                    -1,
                    mapOf()
                )
                userAccountSerializable = UserAccountSerializable(
                    ScrobblableEnum.GNUFM,
                    user,
                    gnufmSessKey,
                    gnufmRoot,
                    gnufmTlsNoVerify
                )
                scrobbleAccounts += userAccountSerializable
            }


            // listenbrainz
            if (listenbrainzToken != null && listenbrainzUsername != null) {
                user = UserSerializable(
                    listenbrainzUsername,
                    "https://listenbrainz.org/user/$listenbrainzUsername",
                    listenbrainzUsername,
                    "",
                    -1,
                    mapOf()
                )
                userAccountSerializable = UserAccountSerializable(
                    ScrobblableEnum.LISTENBRAINZ,
                    user,
                    listenbrainzToken,
                )
                scrobbleAccounts += userAccountSerializable
            }


            // custom listenbrainz
            if (customListenbrainzToken != null && customListenbrainzUsername != null) {
                user = UserSerializable(
                    customListenbrainzUsername,
                    "$customListenbrainzRoot/user/$customListenbrainzUsername",
                    customListenbrainzUsername,
                    "",
                    -1,
                    mapOf()
                )
                userAccountSerializable = UserAccountSerializable(
                    ScrobblableEnum.CUSTOM_LISTENBRAINZ,
                    user,
                    customListenbrainzToken,
                    customListenbrainzRoot,
                    customListenbrainzTlsNoVerify
                )
                scrobbleAccounts += userAccountSerializable
            }

            prefs.scrobbleAccounts = scrobbleAccounts

        }
        prefs.prefVersion = 2
    }

}