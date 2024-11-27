package com.arn.scrobble.api.lastfm

import co.touchlab.kermit.Logger
import com.arn.scrobble.api.Requesters
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.Url
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


object LastfmUnscrobbler {

    class CookiesInvalidatedException(msg: String = "cookies invalidated") :
        IllegalStateException(msg)

    suspend fun unscrobble(track: Track, username: String): Unit =
        lock.withLock { // does this fix the csrf invalidation problem?
            val csrfToken =
                cookieStorage.get(Url(URL_USER)).find { it.name == COOKIE_CSRFTOKEN }?.value

            val parameters = Parameters.build {
                append(
                    FIELD_CSRFTOKEN,
                    csrfToken ?: throw CookiesInvalidatedException()
                )
                append(FIELD_ARTIST, track.artist.name)
                append(FIELD_TRACK, track.name)
                append(
                    FIELD_TIMESTAMP,
                    track.date?.div(1000)?.toString() ?: throw IllegalStateException("no date")
                )
                append(FIELD_AJAX, "1")
            }

            val url = "$URL_USER$username/library/delete"

            val response = client.submitForm(url, parameters) {
                header(HttpHeaders.Referrer, URL_USER + username)
            }

            if (response.status == HttpStatusCode.OK) {
                val success = response.body<DeleteScrobbleResponse>().result

                if (success)
                    Logger.i { "LastfmUnscrobbler unscrobbled" }
                else
                    throw IllegalStateException("LastfmUnscrobbler: error unscrobbling")
            } else if (response.status == HttpStatusCode.Forbidden) {
                cookieStorage.clear()
                throw CookiesInvalidatedException()
            } else {
                throw ResponseException(
                    response,
                    "LastfmUnscrobbler: error unscrobbling: " + response.status.value
                )
            }

            // add a random delay to prevent 406 error
            delay((1000L..10000L).random())
        }

    const val COOKIE_CSRFTOKEN = "csrftoken"
    const val COOKIE_SESSIONID = "sessionid"

    private const val URL_USER = "https://www.last.fm/user/"

    private const val FIELD_CSRFTOKEN = "csrfmiddlewaretoken"

    private const val FIELD_ARTIST = "artist_name"
    private const val FIELD_TRACK = "track_name"
    private const val FIELD_TIMESTAMP = "timestamp"
    private const val FIELD_AJAX = "ajax"

    private val lock by lazy { Mutex() }

    val cookieStorage by lazy { CookiesDatastore() }
    private val client by lazy {
        Requesters.genericKtorClient.config {
            install(HttpCookies) {
                storage = cookieStorage
            }
        }
    }
}



