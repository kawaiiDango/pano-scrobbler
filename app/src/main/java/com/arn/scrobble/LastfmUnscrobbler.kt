package com.arn.scrobble

import com.arn.scrobble.scrobbleable.AccountType
import com.arn.scrobble.scrobbleable.Scrobblables
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import com.frybits.harmony.getHarmonySharedPreferences
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.json.JSONObject


class LastfmUnscrobbler {

    private val client by lazy {
        LFMRequester.okHttpClient.newBuilder()
            .cookieJar(cookieJar)
//                .addInterceptor(LoggingInterceptor())
//                .followRedirects(false)
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

    class LoggingInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()

            val t1 = System.nanoTime()
            println(
                String.format(
                    "--> Sending request %s on %s%n%s",
                    request.url, chain.connection(), request.headers
                )
            )

            val requestBuffer = Buffer()
            request.body?.writeTo(requestBuffer)
            println(requestBuffer.readUtf8())

            val response = chain.proceed(request)

            val t2 = System.nanoTime()
            println(
                String.format(
                    "<-- Received response for %s in %.1fms%n%s",
                    response.request.url, (t2 - t1) / 1e6, response.headers
                )
            )

            val contentType = response.body.contentType()
            val content = response.body.string()
            println(content)

            val wrappedBody = content.toResponseBody(contentType)
            return response.newBuilder().body(wrappedBody).build()
        }
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

    suspend fun unscrobble(artist: String, track: String, timeMillis: Long): Boolean =
        lock.withLock { // does this fix the csrf invalidation problem?
            var success = false
            val body = FormBody.Builder()
                .add(
                    FIELD_CSRFTOKEN,
                    csrfToken ?: throw RuntimeException("You've been logged out...")
                )
                .add(FIELD_ARTIST, artist)
                .add(FIELD_TRACK, track)
                .add(FIELD_TIMESTAMP, (timeMillis / 1000).toInt().toString())
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
                        Stuff.log("LastfmUnscrobbler unscrobbled: $track")
                } else if (resp.code == 403) {
                    clearCookies()
                    throw RuntimeException("csrf token invalidated")
                } else {
                    Stuff.log("LastfmUnscrobbler: error unscrobbling: " + resp.code + " response: " + respStr)
                }
            }

            success
        }

    companion object {
        const val COOKIE_CSRFTOKEN = "csrftoken"
        const val COOKIE_SESSIONID = "sessionid"

        private const val URL_LOGIN = "https://secure.last.fm/login"
        private const val URL_USER = "https://www.last.fm/user/"

        private const val FIELD_CSRFTOKEN = "csrfmiddlewaretoken"

        private const val FIELD_USERNAME = "username"
        private const val FIELD_PASSWORD = "password"

        private const val FIELD_ARTIST = "artist_name"
        private const val FIELD_TRACK = "track_name"
        private const val FIELD_TIMESTAMP = "timestamp"

        private val lock by lazy { Mutex() }
    }
}



