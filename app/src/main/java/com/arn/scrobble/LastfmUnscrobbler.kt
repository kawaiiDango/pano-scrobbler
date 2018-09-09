package com.arn.scrobble

import android.content.Context
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import okhttp3.*
import okio.Buffer


class LastfmUnscrobbler(context: Context?) {

    private var csrfToken: String? = null
    private var username = ""
    private var client: OkHttpClient? = null
    get() {
        if (field == null) {
            val builder = OkHttpClient.Builder()
                    .cookieJar(cookieJar)
//                .addInterceptor(LoggingInterceptor())
//                .followRedirects(false)

            field = builder.build()
        }
       return field
    }
    private var cookieCache = SetCookieCache()
    private val cookieJar: CookieJar

    class LoggingInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()

            val t1 = System.nanoTime()
            println(String.format("--> Sending request %s on %s%n%s", request.url(), chain.connection(), request.headers()))

            val requestBuffer = Buffer()
            request.body()?.writeTo(requestBuffer)
            println(requestBuffer.readUtf8())

            val response = chain.proceed(request)

            val t2 = System.nanoTime()
            println(String.format("<-- Received response for %s in %.1fms%n%s", response.request().url(), (t2 - t1) / 1e6, response.headers()))

            val contentType = response.body()?.contentType()
            val content = response.body()?.string()
            println(content)

            val wrappedBody = ResponseBody.create(contentType, content)
            return response.newBuilder().body(wrappedBody).build()
        }
    }

    init{
        if (context != null)
            cookieJar = object: PersistentCookieJar(cookieCache, SharedPrefsCookiePersistor(context)) {
                override fun saveFromResponse(url: HttpUrl, cookies: MutableList<Cookie>?) {
                    super.saveFromResponse(url, cookies)
                    cookies
                            ?.find { it.name() == COOKIE_CSRFTOKEN }
                            ?.let { csrfToken = it.value() }
                }
            }
        else { //for unit tests
            cookieJar = object : CookieJar{
                override fun saveFromResponse(url: HttpUrl, cookies: MutableList<Cookie>?) {
                    cookies
                            ?.find { it.name() == COOKIE_CSRFTOKEN }
                            ?.let { csrfToken = it.value() }

                    cookieCache.addAll(cookies)
                }

                override fun loadForRequest(url: HttpUrl): MutableList<Cookie> {
                    return cookieCache.toMutableList()
                }
            }
        }
    }

    fun clearCookies() {
        (cookieJar as PersistentCookieJar).clear()
    }

    fun checkCsrf(username: String): Boolean {
        this.username = username
        csrfToken = cookieCache.find {
            it.name() == COOKIE_CSRFTOKEN && it.expiresAt() > System.currentTimeMillis()
        }?.value()
        if (csrfToken == null)
            return false
        return true
    }

    fun loginWithPassword(password: String): Boolean {
        var succ = false
        val request = Request.Builder()
                .url(URL_LOGIN)
                .build()

        //fetch csrf token
        val resp = client!!.newCall(request).execute()

        if (resp.code() == 200) {
            if (csrfToken == null)
                Stuff.log("err: LastfmUnscrobbler csrfToken == null")
            else if (authenticate(username, password))
                succ = true
        } else
            Stuff.log("err: LastfmUnscrobbler status: " + resp.code())

        resp.close()

        if (!succ)
            clearCookies()
        return succ
    }

    private fun authenticate(username: String, password: String): Boolean {
        println("authenticate")

        val body = FormBody.Builder()
                .add(FIELD_CSRFTOKEN, csrfToken ?: return false)
                .add(FIELD_USERNAME, username)
                .add(FIELD_PASSWORD, password)
                .build()

        val request = Request.Builder()
                .url(URL_LOGIN)
                .header("Referer", URL_LOGIN)
                .post(body)
                .build()

        try {
            val resp = client!!.newCall(request).execute()

            val respString = resp.body()?.string() ?: ""

            if (resp.code() != 200) {
                if (resp.code() == 403 || resp.code() == 401) {
                    Stuff.log("err: LastfmUnscrobbler Incorrect credentials")
                } else {
                    Stuff.log("err: LastfmUnscrobbler authenticate status: "+ resp.code())
                }
                return false
            } else if (respString.indexOf("auth-avatar") == -1)
                return false
            resp.close()
        } catch (e: Exception) {
            Stuff.log("Could not post: " + e.message)
            return false
        }

        return true
    }

    fun unscrobble(artist: String, track: String, timeMillis: Long): Boolean {

        val body = FormBody.Builder()
                .add(FIELD_CSRFTOKEN, csrfToken ?:
                        throw RuntimeException("You've been logged out..."))
                .add(FIELD_ARTIST, artist)
                .add(FIELD_TRACK, track)
                .add(FIELD_TIMESTAMP, (timeMillis / 1000).toInt().toString())
                .add("ajax", "1")
                .build()

        val url = "$URL_USER$username/unscrobble"
        val request = Request.Builder()
                .url(url)
                .header("Referer", URL_USER+ username)
                .post(body)
                .build()

        try {
            val resp = client!!.newCall(request).execute()
            val respStr = resp.body()?.string()
            if (resp.code() == 200 && respStr?.contains("{\"result\": true}") == true) {
                Stuff.log("LastfmUnscrobbler unscrobbled: $track")
            } else if (resp.code() == 403) { //invalid csrf (session was probably invalidated)
                clearCookies()
                throw RuntimeException("You've been logged out.")
            } else {
                Stuff.log("err: LastfmUnscrobbler unscrobble status: " + resp.code())
                Stuff.log("err: $respStr " )
                return false
            }
        } catch (e: Exception) {
            Stuff.log("err: LastfmUnscrobbler unscrobble err: " + e.message)
        }
        return true
    }
}

private const val URL_LOGIN = "https://secure.last.fm/login"
private const val URL_USER = "https://www.last.fm/user/"

private const val FIELD_CSRFTOKEN = "csrfmiddlewaretoken"
private const val COOKIE_CSRFTOKEN = "csrftoken"
private const val COOKIE_SESSIONID = "sessionid"

private const val FIELD_USERNAME = "username"
private const val FIELD_PASSWORD = "password"

private const val FIELD_ARTIST = "artist_name"
private const val FIELD_TRACK = "track_name"
private const val FIELD_TIMESTAMP = "timestamp"

