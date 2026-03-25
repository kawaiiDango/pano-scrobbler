package com.arn.scrobble.api.lastfm

import com.arn.scrobble.api.cache.ExpirationPolicy
import com.arn.scrobble.utils.Stuff
import io.ktor.http.Url
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.concurrent.TimeUnit

class DefaultExpirationPolicy : ExpirationPolicy {

    private val ONE_WEEK = TimeUnit.DAYS.toMillis(7)
    private val THREE_MINUTES = TimeUnit.MINUTES.toMillis(3)
    private val ONE_MONTH = TimeUnit.DAYS.toMillis(30)

    private val lastFmHost = Stuff.LASTFM_API_ROOT.toHttpUrl().host
    private val libreFmHost = Stuff.LIBREFM_API_ROOT.toHttpUrl().host
    private val listenBrainzHost = Stuff.LISTENBRAINZ_API_ROOT.toHttpUrl().host

    override fun getExpirationTime(url: Url): Long {
        return when (url.host) {
            lastFmHost, libreFmHost ->
                getLastFmExpiration(url)

            listenBrainzHost -> getListenBrainzExpiration(url)
            else -> -1
        }
    }

    private fun getLastFmExpiration(url: Url): Long {
        val method = url.parameters["method"]?.lowercase()
        val username = url.parameters["username"]
        val page = url.parameters["page"]
        if (method == null || page != null && page != "1") {
            // cache only the first page
            return -1
        }

        return when (method) {
            "artist.getsimilar",
            "tag.getsimilar",
            "geo.gettopartists",
            "geo.gettoptracks",
            "tag.gettopalbums",
            "tag.gettopartists",
            "tag.gettoptags",
            "tag.gettoptracks",
            "user.gettoptags",
            "user.getlovedtracks",
            "user.getrecenttracks",
            "user.getfriends",
            "track.getsimilar",
            "artist.gettopalbums",
            "artist.gettoptracks" -> ONE_WEEK

            "track.getinfo",
            "album.getinfo",
            "artist.getinfo" -> {
                if (username != null)
                    THREE_MINUTES
                else
                    ONE_MONTH
            }

            "track.gettoptags",
            "album.gettoptags",
            "artist.gettoptags" -> ONE_MONTH

            "user.gettopalbums",
            "user.gettopartists",
            "user.gettoptracks",
            "user.getweeklyalbumchart",
            "user.getweeklyartistchart",
            "user.getweeklytrackchart",
            "user.getweeklychartlist" -> THREE_MINUTES

            else -> -1
        }
    }

    private fun getListenBrainzExpiration(url: Url): Long {
        return when (url.segments.lastOrNull()) {
            "playing-now",
            "listens",
            "following",
            "get-feedback",
                -> ONE_WEEK

            "artists",
            "releases",
            "recordings",
                -> THREE_MINUTES

            else -> -1
        }
    }
}
