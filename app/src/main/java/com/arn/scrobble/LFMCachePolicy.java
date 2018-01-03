package com.arn.scrobble;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.umass.lastfm.cache.DefaultExpirationPolicy;

/**
 * Created by arn on 09/09/2017.
 */
class LFMCachePolicy extends DefaultExpirationPolicy {
    private Set<String> staleDataOffline = new HashSet<>();
    boolean isNetworkAvailable;
    public LFMCachePolicy (boolean isNetworkAvailable) {
        this.isNetworkAvailable = isNetworkAvailable;
        staleDataOffline.add("user.getLovedTracks");
        staleDataOffline.add("user.getRecentTracks");
        staleDataOffline.add("user.getFriends");
    }

    public long getExpirationTime(String method, Map<String, String> params) {
        long time = super.getExpirationTime(method, params);
        if (time == -1 && staleDataOffline.contains(method)) {
            if (isNetworkAvailable) {
                time = NETWORK_AND_CACHE_CONST;
            } else
                time = ONE_WEEK;
        }
        return time;
    }
//    static long NETWORK_AND_CACHE_CONST = ONE_WEEK + 1;
}