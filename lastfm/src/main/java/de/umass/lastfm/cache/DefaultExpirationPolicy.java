/*
 * Copyright (c) 2012, the Last.fm Java Project and Committers
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package de.umass.lastfm.cache;

import java.util.HashSet;
import java.util.Set;

import de.umass.lastfm.Caller;
import okhttp3.HttpUrl;

/**
 * This Policy maintains a list of methods which should be cached one week. Everything else won't be cached if
 * using this policy.
 *
 * @author Janni Kovacs
 */
public class DefaultExpirationPolicy implements ExpirationPolicy {


    /**
     * One day in milliseconds
     */
    protected static final long ONE_DAY = 1000 * 60 * 60 * 24;

    /**
     * One week in milliseconds
     */
    protected static final long ONE_WEEK = ONE_DAY * 7;
    public static final long FIVE_MINUTES = 1000 * 60 * 5;
    public static final long ONE_MONTH = ONE_DAY * 30;

    /**
     * Contains the lower case method names for all requests that should be cached 1 week
     */
    protected static final Set<String> ONE_WEEK_METHODS = new HashSet<>();
    protected static final Set<String> ONE_MONTH_METHODS = new HashSet<>();
    protected static final Set<String> FIVE_MINUTES_METHODS = new HashSet<>();

    static {
        // similar data
        ONE_WEEK_METHODS.add("artist.getsimilar");
        ONE_WEEK_METHODS.add("tag.getsimilar");
//		ONE_WEEK_METHODS.add("track.getsimilar");  //scrobble counts get outdated
        // top chart data
//		ONE_WEEK_METHODS.add("artist.gettopalbums");
//		ONE_WEEK_METHODS.add("artist.gettoptracks");
        ONE_WEEK_METHODS.add("geo.gettopartists");
        ONE_WEEK_METHODS.add("geo.gettoptracks");
        ONE_WEEK_METHODS.add("tag.gettopalbums");
        ONE_WEEK_METHODS.add("tag.gettopartists");
        ONE_WEEK_METHODS.add("tag.gettoptags");
        ONE_WEEK_METHODS.add("tag.gettoptracks");
//		ONE_WEEK_METHODS.add("user.gettopalbums");
//		ONE_WEEK_METHODS.add("user.gettopartists");
//		ONE_WEEK_METHODS.add("user.gettoptracks");
        ONE_WEEK_METHODS.add("user.gettoptags");

        ONE_WEEK_METHODS.add("user.getlovedtracks");
        ONE_WEEK_METHODS.add("user.getrecenttracks");
        ONE_WEEK_METHODS.add("user.getfriends");

        //track info without username
        ONE_MONTH_METHODS.add("track.getinfo");
        //album info without username
        ONE_MONTH_METHODS.add("album.getinfo");
        //artist info without username
        ONE_MONTH_METHODS.add("artist.getinfo");

        // artist picture changes
        ONE_WEEK_METHODS.add("artist.getinfo.spotify");

        ONE_MONTH_METHODS.add("track.gettoptags");
        ONE_MONTH_METHODS.add("album.gettoptags");
        ONE_MONTH_METHODS.add("artist.gettoptags");

        FIVE_MINUTES_METHODS.add("track.getsimilar");
        FIVE_MINUTES_METHODS.add("artist.gettopalbums");
        FIVE_MINUTES_METHODS.add("artist.gettoptracks");
        FIVE_MINUTES_METHODS.add("user.gettopalbums");
        FIVE_MINUTES_METHODS.add("user.gettopartists");
        FIVE_MINUTES_METHODS.add("user.gettoptracks");

        FIVE_MINUTES_METHODS.add("user.getweeklyalbumchart");
        FIVE_MINUTES_METHODS.add("user.getweeklyartistchart");
        FIVE_MINUTES_METHODS.add("user.getweeklytrackchart");
        FIVE_MINUTES_METHODS.add("user.getweeklychartlist");
    }

    /**
     * Contains the expiration time for weekly chart data for the current week, which is
     * one week by default; last.fm TOS says:
     * <blockquote>
     * You agree to cache similar artist and any chart data (top tracks, top artists, top albums) for a minimum of one week.
     * </blockquote>
     * but they might be outdated the next day.
     * For now we will cache them one week. If you always need the latest charts but don't want to disable
     * caching use the {@link #setCacheRecentWeeklyCharts(long)} method to set this value.
     * This variable also applies to the getWeeklyChartList method
     */
    protected long cacheRecentWeeklyCharts = FIVE_MINUTES;

    public long getExpirationTime(HttpUrl url) {
        String method = url.queryParameter(Caller.PARAM_METHOD);
        String username = url.queryParameter("username");
        String page = url.queryParameter("page");

        if (method == null || (page != null && !page.equals("1"))) {
            // cache only the first page
            return -1;
        }

        method = method.toLowerCase();
//		if (method.contains("weekly")) {
//			if (!method.contains("list"))
//				return params.containsKey("to") && params.containsKey("from") ? Long.MAX_VALUE : cacheRecentWeeklyCharts;
//			else
//				return cacheRecentWeeklyCharts;
//		}
        if (method.equals("track.getinfo") || method.equals("album.getinfo"))
            return username == null ? ONE_WEEK : -1;

        if (ONE_WEEK_METHODS.contains(method))
            return ONE_WEEK;
        else if (ONE_MONTH_METHODS.contains(method))
            return ONE_MONTH;
        else if (FIVE_MINUTES_METHODS.contains(method))
            return FIVE_MINUTES;
        else
            return -1;
    }

    public void setCacheRecentWeeklyCharts(long cacheRecentWeeklyCharts) {
        this.cacheRecentWeeklyCharts = cacheRecentWeeklyCharts;
    }
}
