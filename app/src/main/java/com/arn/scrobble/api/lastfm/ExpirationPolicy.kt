package com.arn.scrobble.api.lastfm

import okhttp3.HttpUrl

interface ExpirationPolicy {
    fun getExpirationTimeSecs(url: HttpUrl): Int
}
