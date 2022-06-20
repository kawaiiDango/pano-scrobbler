package com.arn.scrobble

import android.content.Context
import com.arn.scrobble.pref.MainPrefs
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import com.frybits.harmony.getHarmonySharedPreferences
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.json.JSONObject


class LastfmUnscrobbler(context: Context?) {

    private val client by lazy {
        LFMRequester.okHttpClient.newBuilder()
            .cookieJar(cookieJar)
//                .addInterceptor(LoggingInterceptor())
//                .followRedirects(false)
            .build()
    }
    private val cookieCache = SetCookieCache()
    private val cookieJar: CookieJar
    private val username by lazy { MainPrefs(context!!).lastfmUsername!! }
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
        cookieJar = if (context != null)
            PersistentCookieJar(
                cookieCache,
                SharedPrefsCookiePersistor(context.getHarmonySharedPreferences("CookiePersistence"))
            )
        else { //for unit tests
            object : CookieJar {
                override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                    cookieCache.addAll(cookies)
                }

                override fun loadForRequest(url: HttpUrl) = cookieCache.toList()
            }
        }
    }

    fun putCookies(url: HttpUrl, cookies: List<Cookie>) {
        cookieJar.saveFromResponse(url, cookies)
    }

    fun clearCookies() {
        (cookieJar as PersistentCookieJar).clear()
    }

    fun haveCsrfCookie() = csrfToken != null

    fun loginWithPassword(password: String): String? {
        var errMsg: String? = "" //null if success
        val request = Request(URL_LOGIN.toHttpUrl())

        //fetch csrf token
        try {
            val resp = client.newCall(request).execute()
            if (resp.code == 200) {
                if (csrfToken == null)
                    Stuff.log("err: LastfmUnscrobbler csrfToken == null")
                else
                    errMsg = authenticate(username, password)
            } else
                errMsg = "Error: HTTP status " + resp.code
            resp.close()
        } catch (e: Exception) {
            errMsg = e.message
        }

        if (errMsg != null)
            clearCookies()
        return errMsg
    }

    private fun authenticate(username: String, password: String): String? {
        //null if success

        val body = FormBody.Builder()
            .add(FIELD_CSRFTOKEN, csrfToken ?: return "No csrf token")
            .add(FIELD_USERNAME, username)
            .add(FIELD_PASSWORD, password)
            .build()

        val request = Request(
            URL_LOGIN.toHttpUrl(),
            headers = Headers.headersOf("Referer", URL_LOGIN),
            body = body
        )

        try {
            val resp = client.newCall(request).execute()

            val respString = resp.body.string()

            if (resp.code != 200) {
                return if (resp.code == 403 || resp.code == 401) {
                    "Incorrect credentials"
                } else {
                    "authenticate status: " + resp.code
                }
            } else if (respString.indexOf("auth-avatar") == -1)
                return "Couldn't log in"
            resp.close()
        } catch (e: Exception) {
            return "Could not log in: " + e.message
        }

        return null
    }

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

            try {
                val resp = client.newCall(request).execute()
                val respStr = resp.body.string()
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

            } catch (e: Exception) {
                Stuff.log("err: LastfmUnscrobbler unscrobble err: " + e.message)
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



