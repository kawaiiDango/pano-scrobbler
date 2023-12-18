package com.arn.scrobble.api.lastfm

import com.arn.scrobble.App
import com.arn.scrobble.api.lastfm.CookieSerializable.Companion.toCookieSerializable
import com.arn.scrobble.utils.Stuff
import com.frybits.harmony.getHarmonySharedPreferences
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.http.Url
import io.ktor.util.date.GMTDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
data class CookieSerializable(
    val name: String,
    val value: String,
    val encoding: CookieEncoding = CookieEncoding.URI_ENCODING,
    val maxAge: Int = 0,
    val expires: Long? = null,
    val domain: String? = null,
    val path: String? = null,
    val secure: Boolean = false,
    val httpOnly: Boolean = false,
    val extensions: Map<String, String?> = emptyMap()
) {
    fun toCookie() = Cookie(
        name,
        value,
        encoding,
        maxAge,
        GMTDate(expires),
        domain,
        path,
        secure,
        httpOnly,
        extensions
    )

    companion object {
        fun Cookie.toCookieSerializable() = CookieSerializable(
            name,
            value,
            encoding,
            maxAge,
            expires?.timestamp,
            domain,
            path,
            secure,
            httpOnly,
            extensions
        )
    }
}

class SharedPreferencesCookiesStorage(prefsName: String) : CookiesStorage {

    private val sharedPreferences = App.context.getHarmonySharedPreferences(prefsName)
    private val json = Stuff.myJson

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        val editor = sharedPreferences.edit()
        // Include the domain in the key
        val key = "${cookie.name}@${requestUrl.host}"
        editor.putString(key, json.encodeToString(cookie.toCookieSerializable()))
        editor.apply()
    }

    override suspend fun get(requestUrl: Url) =
        sharedPreferences.all.mapNotNull { (key, value) ->
            // Only return cookies that match the request domain
            if (key.endsWith("@${requestUrl.host}")) {
                value?.let {
                    json.decodeFromString<CookieSerializable>(value as String).toCookie()
                }
            } else {
                null
            }
        }


    override fun close() {
        // No resources need to be released
    }

    fun clear() {
        sharedPreferences.edit().clear().apply()
    }

    companion object {
        const val LASTFM_COOKIES = "LastFmCookies"
    }
}
