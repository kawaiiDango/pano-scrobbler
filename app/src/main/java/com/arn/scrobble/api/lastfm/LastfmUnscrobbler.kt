package com.arn.scrobble.api.lastfm

import com.arn.scrobble.App
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.utils.Stuff
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import com.frybits.harmony.getHarmonySharedPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.json.JSONObject


class LastfmUnscrobbler {

    private val client by lazy {
        Requesters.okHttpClient.newBuilder()
            .cookieJar(cookieJar)
            .build()
    }
    private val cookieCache = SetCookieCache()
    private val cookieJar: CookieJar
    private val username by lazy { Scrobblables.byType(AccountType.LASTFM)!!.userAccount.user.name }
    private val csrfToken by lazy {
        cookieCache.find {
            it.name == COOKIE_CSRFTOKEN && it.expiresAt > System.currentTimeMillis()
        }?.value
    }

    init {
        cookieJar = PersistentCookieJar(
            cookieCache,
            SharedPrefsCookiePersistor(App.context.getHarmonySharedPreferences("CookiePersistence"))
        )
    }

    fun putCookies(url: HttpUrl, cookies: List<Cookie>) {
        cookieJar.saveFromResponse(url, cookies)
    }

    fun clearCookies() {
        (cookieJar as PersistentCookieJar).clear()
    }

    // intercepted from webview login
    fun haveCsrfCookie() = csrfToken != null

    suspend fun unscrobble(artist: String, track: String, timeSecs: Int): Boolean =
        lock.withLock { // does this fix the csrf invalidation problem?
            var success = false
            val body = FormBody.Builder()
                .add(
                    FIELD_CSRFTOKEN,
                    csrfToken ?: throw RuntimeException("You've been logged out...")
                )
                .add(FIELD_ARTIST, artist)
                .add(FIELD_TRACK, track)
                .add(FIELD_TIMESTAMP, timeSecs.toString())
                .add("ajax", "1")
                .build()

            val url = "$URL_USER$username/library/delete"
            val request = Request(
                url.toHttpUrl(),
                headers = Headers.headersOf("Referer", URL_USER + username),
                body = body
            )

            client.newCall(request).execute().use { resp ->
                val respStr = kotlin.runCatching { resp.body.string() }.getOrDefault("")
                if (resp.code == 200) {
                    success = JSONObject(respStr).getBoolean("result")

                    if (success)
                        Stuff.log("LastfmUnscrobbler unscrobbled")
                } else if (resp.code == 403) {
                    clearCookies()
                    throw RuntimeException("csrf token invalidated")
                } else {
                    Stuff.logW("LastfmUnscrobbler: error unscrobbling: " + resp.code)
                }
            }

            // add a random delay to prevent 406 error
            delay((250L..1000L).random())

            success
        }

    companion object {
        const val COOKIE_CSRFTOKEN = "csrftoken"
        const val COOKIE_SESSIONID = "sessionid"

        private const val URL_USER = "https://www.last.fm/user/"

        private const val FIELD_CSRFTOKEN = "csrfmiddlewaretoken"

        private const val FIELD_ARTIST = "artist_name"
        private const val FIELD_TRACK = "track_name"
        private const val FIELD_TIMESTAMP = "timestamp"

        private val lock by lazy { Mutex() }
    }
}



