package com.arn.scrobble

import de.umass.lastfm.cache.DefaultExpirationPolicy

/**
 * Created by arn on 09/09/2017.
 */
class LFMCachePolicy(val isNetworkAvailable:Boolean) : DefaultExpirationPolicy() {
    private val staleDataOffline = setOf("user.getLovedTracks", "user.getRecentTracks")

    override fun getExpirationTime(method: String?, params: MutableMap<String, String>?): Long {
        var time =  super.getExpirationTime(method, params)
        if (time == (-1).toLong() && !isNetworkAvailable)
            time = if (staleDataOffline.contains(method)) ONE_WEEK else -1
//        Stuff.log(method +" cache expires "+ time)
        return time
    }
}