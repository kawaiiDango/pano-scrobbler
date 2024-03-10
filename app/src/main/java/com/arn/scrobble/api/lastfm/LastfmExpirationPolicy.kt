package com.arn.scrobble.api.lastfm

import okhttp3.HttpUrl
import java.util.concurrent.TimeUnit

class LastfmExpirationPolicy : ExpirationPolicy {

    private val ONE_WEEK = TimeUnit.DAYS.toSeconds(7).toInt()
    private val FIVE_MINUTES = TimeUnit.MINUTES.toSeconds(5).toInt()
    private val ONE_MONTH = TimeUnit.DAYS.toSeconds(30).toInt()

    override fun getExpirationTimeSecs(url: HttpUrl): Int {
        val method = url.queryParameter("method")?.lowercase()
        val username = url.queryParameter("username")
        val page = url.queryParameter("page")
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
                    FIVE_MINUTES
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
            "user.getweeklychartlist" -> FIVE_MINUTES

            else -> -1
        }
    }
}
