package com.arn.scrobble

import de.umass.lastfm.cache.DefaultExpirationPolicy

/**
 * Created by arn on 09/09/2017.
 */
class LFMCachePolicy : DefaultExpirationPolicy() {
    private val staleDataOffline = hashSetOf(
        "user.getLovedTracks",
        "user.getRecentTracks",
        "user.getFriends",
    )

    override fun getExpirationTime(method: String, params: Map<String, String>): Long {
        var time = super.getExpirationTime(method, params)
        if (time == -1L && method in staleDataOffline) {
            time = ONE_WEEK
        }
        return time
    }
}