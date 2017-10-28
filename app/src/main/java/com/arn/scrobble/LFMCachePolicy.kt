package com.arn.scrobble

import de.umass.lastfm.cache.DefaultExpirationPolicy

/**
 * Created by arn on 09/09/2017.
 */
class LFMCachePolicy(private val isNetworkAvailable:Boolean) : DefaultExpirationPolicy() {
    private val staleDataOffline = setOf("user.getLovedTracks", "user.getRecentTracks", "user.getFriends")


    override fun getExpirationTime(method: String?, params: MutableMap<String, String>?): Long {
        var time = super.getExpirationTime(method, params)
        if (time == (-1).toLong() && staleDataOffline.contains(method)) {
            if (isNetworkAvailable) {
                time = NETWORK_AND_CACHE_CONST
            } else
                time = ONE_WEEK
        }
        return time
    }
    companion object {
        val NETWORK_AND_CACHE_CONST = ONE_WEEK + 1
    }
}