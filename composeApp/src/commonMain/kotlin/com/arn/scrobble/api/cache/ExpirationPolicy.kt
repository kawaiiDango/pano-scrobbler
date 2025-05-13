package com.arn.scrobble.api.cache

import io.ktor.http.Url

interface ExpirationPolicy {
    fun getExpirationTime(url: Url): Long
}