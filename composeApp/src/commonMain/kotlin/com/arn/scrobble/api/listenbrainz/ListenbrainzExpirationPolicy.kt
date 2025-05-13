package com.arn.scrobble.api.listenbrainz

import com.arn.scrobble.api.cache.ExpirationPolicy
import io.ktor.http.Url
import java.util.concurrent.TimeUnit

class ListenbrainzExpirationPolicy : ExpirationPolicy {
    private val ONE_WEEK = TimeUnit.DAYS.toMillis(7)
    private val THREE_MINUTES = TimeUnit.MINUTES.toMillis(3)

    override fun getExpirationTime(url: Url) =
        when (url.segments.lastOrNull()) {
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