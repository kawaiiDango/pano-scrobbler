package com.arn.scrobble.api.lastfm

import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.api.lastfm.CookieSerializable.Companion.toCookieSerializable
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.http.Url
import io.ktor.util.date.GMTDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

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
            maxAge ?: 0,
            expires?.timestamp,
            domain,
            path,
            secure,
            httpOnly,
            extensions
        )
    }
}

class CookiesDatastore : CookiesStorage {

    private val mainPrefs = PlatformStuff.mainPrefs

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        // Include the domain in the key
        val key = "${cookie.name}@${requestUrl.host}"
        mainPrefs.updateData { it.copy(cookies = it.cookies + (key to cookie.toCookieSerializable())) }
    }

    override suspend fun get(requestUrl: Url) =
        mainPrefs.data.map { it.cookies }
            .first()
            .mapNotNull { (key, value) ->
                // Only return cookies that match the request domain
                if (key.endsWith("@${requestUrl.host}")) {
                    value.toCookie()
                } else {
                    null
                }
            }

    override fun close() {
        // No resources need to be released
    }

    suspend fun clear() {
        mainPrefs.updateData { it.copy(cookies = emptyMap()) }
    }

    companion object {
        const val LASTFM_COOKIES = "LastFmCookies"
    }
}
