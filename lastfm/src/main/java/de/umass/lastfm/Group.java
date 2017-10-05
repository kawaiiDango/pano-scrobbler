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

package de.umass.lastfm;

import java.util.Collection;
import java.util.LinkedHashMap;

/**
 * Provides nothing more than a namespace for the API methods starting with group.
 *
 * @author Janni Kovacs
 */
public class Group {

	private Group() {
	}

	public static Chart<Album> getWeeklyAlbumChart(String group, String apiKey) {
		return getWeeklyAlbumChart(group, null, null, -1, apiKey);
	}

	public static Chart<Album> getWeeklyAlbumChart(String group, int limit, String apiKey) {
		return getWeeklyAlbumChart(group, null, null, limit, apiKey);
	}

	public static Chart<Album> getWeeklyAlbumChart(String group, String from, String to, int limit, String apiKey) {
		return Chart.getChart("group.getWeeklyAlbumChart", "group", group, "album", from, to, limit, apiKey);
	}

	public static Chart<Artist> getWeeklyArtistChart(String group, String apiKey) {
		return getWeeklyArtistChart(group, null, null, -1, apiKey);
	}

	public static Chart<Artist> getWeeklyArtistChart(String group, int limit, String apiKey) {
		return getWeeklyArtistChart(group, null, null, limit, apiKey);
	}

	public static Chart<Artist> getWeeklyArtistChart(String group, String from, String to, int limit, String apiKey) {
		return Chart.getChart("group.getWeeklyArtistChart", "group", group, "artist", from, to, limit, apiKey);
	}

	public static Chart<Track> getWeeklyTrackChart(String group, String apiKey) {
		return getWeeklyTrackChart(group, null, null, -1, apiKey);
	}

	public static Chart<Track> getWeeklyTrackChart(String group, int limit, String apiKey) {
		return getWeeklyTrackChart(group, null, null, limit, apiKey);
	}

	public static Chart<Track> getWeeklyTrackChart(String group, String from, String to, int limit, String apiKey) {
		return Chart.getChart("group.getWeeklyTrackChart", "group", group, "track", from, to, limit, apiKey);
	}

	public static LinkedHashMap<String, String> getWeeklyChartList(String group, String apiKey) {
		return Chart.getWeeklyChartList("group.getWeeklyChartList", "group", group, apiKey);
	}

	public static Collection<Chart> getWeeklyChartListAsCharts(String group, String apiKey) {
		return Chart.getWeeklyChartListAsCharts("group", group, apiKey);
	}

	/**
	 * Get a list of members for this group.
	 *
	 * @param group The group name to fetch the members of
	 * @param apiKey A Last.fm API key
	 * @return the list of {@link User}s
	 */
	public static PaginatedResult<User> getMembers(String group, String apiKey) {
		return getMembers(group, 1, apiKey);
	}

	/**
	 * Get a list of members for this group.
	 *
	 * @param group The group name to fetch the members of
	 * @param page The results page you would like to fetch
	 * @param apiKey A Last.fm API key
	 * @return the list of {@link User}s
	 */
	public static PaginatedResult<User> getMembers(String group, int page, String apiKey) {
		Result result = Caller.getInstance().call("group.getMembers", apiKey, "group", group, "page", String.valueOf(page));
		return ResponseBuilder.buildPaginatedResult(result, User.class);
	}

	/**
	 * Get the hype list for a group.
	 *
	 * @param group The last.fm group name
	 * @param apiKey A Last.fm API key
	 * @return a Collection of {@link Artist}s
	 */
	public static Collection<Artist> getHype(String group, String apiKey) {
		Result result = Caller.getInstance().call("group.getHype", apiKey, "group", group);
		return ResponseBuilder.buildCollection(result, Artist.class);
	}
}
