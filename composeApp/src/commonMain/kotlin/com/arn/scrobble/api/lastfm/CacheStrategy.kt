package com.arn.scrobble.api.lastfm

enum class CacheStrategy {
    CACHE_FIRST,
    CACHE_FIRST_ONE_DAY,
    CACHE_FIRST_ONE_WEEK,
    CACHE_ONLY_INCLUDE_EXPIRED,
    NETWORK_ONLY
}
