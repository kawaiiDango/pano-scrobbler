package com.arn.scrobble.api

import io.ktor.http.Url

interface ExpirationPolicy {
    fun getExpirationTime(url: Url): Long
}
