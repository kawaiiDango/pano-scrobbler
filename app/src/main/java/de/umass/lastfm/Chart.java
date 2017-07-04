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

import java.util.*;

import de.umass.util.MapUtilities;
import de.umass.xml.DomElement;

/**
 * Bean for Chart information. Contains a start date, an end date and a list of entries.
 *
 * @author Janni Kovacs
 */
public class Chart<T extends MusicEntry> {

	private Date from, to;
	private Collection<T> entries;

	public Chart(Date from, Date to, Collection<T> entries) {
		this.from = from;
		this.to = to;
		this.entries = entries;
	}

	public Collection<T> getEntries() {
		return entries;
	}

	public Date getFrom() {
		return from;
	}

	public Date getTo() {
		return to;
	}

	/**
	 * This is an internal method to retrieve Chart data.
	 *
	 * @param method The method to call, must be one of the getWeeklyXXXChart methods
	 * @param sourceType The name of the parameter to get the charts for, either "user", "tag" or "group"
	 * @param source The username, tag or group to get charts from
	 * @param target The expected chart type, either "album", "artist" or "track"
	 * @param from Start date or <code>null</code>
	 * @param to End date or <code>null</code>
	 * @param limit The number of chart items to return or -1
	 * @param apiKey A Last.fm API key.
	 * @return a Chart
	 */
	static <T extends MusicEntry> Chart<T> getChart(String method, String sourceType, String source,
													String target, String from, String to, int limit,
													String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		params.put(sourceType, source);
		return getChart(method, target, params, from, to, limit, apiKey);
	}

	/**
	 * This is an internal method to retrieve Chart data.
	 *
	 * @param method The method to call, must be one of the getWeeklyXXXChart methods
	 * @param params Extra parameters that will be passed to the webservice, e.g. containing user or tag name
	 * @param target The expected chart type, either "album", "artist" or "track"
	 * @param from Start date or <code>null</code>
	 * @param to End date or <code>null</code>
	 * @param limit The number of chart items to return or -1
	 * @param apiKey A Last.fm API key.
	 * @return a Chart
	 */
	@SuppressWarnings("unchecked")
	static <T extends MusicEntry> Chart<T> getChart(String method, String target, Map<String, String> params, String from, String to,
												 	int limit, String apiKey) {
		if (from != null && to != null) {
			params.put("from", from);
			params.put("to", to);
		}
		MapUtilities.nullSafePut(params, "limit", limit);
		Result result = Caller.getInstance().call(method, apiKey, params);
		if (!result.isSuccessful())
			return null;
		DomElement element = result.getContentElement();
		Collection<DomElement> children = element.getChildren(target);
		Collection collection = new ArrayList(children.size());
		boolean targetArtist = "artist".equals(target);
		boolean targetTrack = "track".equals(target);
		boolean targetAlbum = "album".equals(target);
		for (DomElement domElement : children) {
			if (targetArtist)
				collection.add(ResponseBuilder.buildItem(domElement, Artist.class));
			if (targetTrack)
				collection.add(ResponseBuilder.buildItem(domElement, Track.class));
			if (targetAlbum)
				collection.add(ResponseBuilder.buildItem(domElement, Album.class));
		}
		long fromTime = 0;
		long toTime = 0;
		// workaround for geo.getMetroXXX methods, since they don't have from & to attributes if no dates were given upon calling 
		if (element.hasAttribute("from")) {
			fromTime = 1000 * Long.parseLong(element.getAttribute("from"));
			toTime = 1000 * Long.parseLong(element.getAttribute("to"));
		}
		return new Chart<T>(new Date(fromTime), new Date(toTime), collection);
	}

	/**
	 * This is an internal method to get a list of available charts.
	 *
	 * @param methodName The name of the method to be called, e.g. <code>user.getWeeklyChartList</code>
	 * @param paramName The name of the parameter which is passed to the specified method, e.g. <code>user</code>
	 * @param paramValue The value of the parameter which is passed to the specified method, e.g. the user name
	 * @param apiKey A Last.fm API key.
	 * @return a list of available charts as a Map
	 */
	static LinkedHashMap<String, String> getWeeklyChartList(String methodName, String paramName, String paramValue, String apiKey) {
		Result result = Caller.getInstance().call(methodName, apiKey, paramName, paramValue);
		if (!result.isSuccessful())
			return new LinkedHashMap<String, String>(0);
		DomElement element = result.getContentElement();
		LinkedHashMap<String, String> list = new LinkedHashMap<String, String>();
		for (DomElement domElement : element.getChildren("chart")) {
			list.put(domElement.getAttribute("from"), domElement.getAttribute("to"));
		}
		return list;
	}

	/**
	 * This is an internal method to get a list of available charts.
	 *
	 * @param sourceType The name of the parameter to get the charts for, either "user", "tag" or "group"
	 * @param source The username, tag or group to get charts from
	 * @param apiKey A Last.fm API key.
	 * @return a list of available charts as a Collection of Charts
	 */
	@SuppressWarnings("unchecked")
	static Collection<Chart> getWeeklyChartListAsCharts(String sourceType, String source, String apiKey) {
		Result result = Caller.getInstance().call(sourceType + ".getWeeklyChartList", apiKey, sourceType, source);
		if (!result.isSuccessful())
			return Collections.emptyList();
		DomElement element = result.getContentElement();
		List<Chart> list = new ArrayList<Chart>();
		for (DomElement domElement : element.getChildren("chart")) {
			long fromTime = 1000 * Long.parseLong(domElement.getAttribute("from"));
			long toTime = 1000 * Long.parseLong(domElement.getAttribute("to"));
			list.add(new Chart(new Date(fromTime), new Date(toTime), null));
		}
		return list;
	}

	/**
	 * Get the top artists chart.
	 * 
	 * @param apiKey A Last.fm API key
	 * @return Top artists chart
	 */
	public static PaginatedResult<Artist> getTopArtists(String apiKey) {
		return getTopArtists(1, apiKey);
	}
	
	/**
	 * Get the top artists chart.
	 *
	 * @param page The page to fetch
	 * @param apiKey A Last.fm API key
	 * @return Top artists chart
	 */
	public static PaginatedResult<Artist> getTopArtists(int page, String apiKey) {
		Result result = Caller.getInstance().call("chart.getTopArtists", apiKey, "page", String.valueOf(page));
		return ResponseBuilder.buildPaginatedResult(result, Artist.class);
	}

	/**
	 * Get the top tags chart.
	 *
	 * @param apiKey A Last.fm API key
	 * @return Top tags chart
	 */
	public static PaginatedResult<Tag> getTopTags(String apiKey) {
		return getTopTags(1, apiKey);
	}

	/**
	 * Get the top tags chart.
	 *
	 * @param page The page to fetch
	 * @param apiKey A Last.fm API key
	 * @return Top tags chart
	 */
	public static PaginatedResult<Tag> getTopTags(int page, String apiKey) {
		Result result = Caller.getInstance().call("chart.getTopTags", apiKey, "page", String.valueOf(page));
		return ResponseBuilder.buildPaginatedResult(result, Tag.class);
	}

	/**
	 * Get the top tracks chart.
	 *
	 * @param apiKey A Last.fm API key
	 * @return Top tracks chart
	 */
	public static PaginatedResult<Track> getTopTracks(String apiKey) {
		return getTopTracks(1, apiKey);
	}

	/**
	 * Get the top tracks chart.
	 *
	 * @param page The page to fetch
	 * @param apiKey A Last.fm API key
	 * @return Top tracks chart
	 */
	public static PaginatedResult<Track> getTopTracks(int page, String apiKey) {
		Result result = Caller.getInstance().call("chart.getTopTracks", apiKey, "page", String.valueOf(page));
		return ResponseBuilder.buildPaginatedResult(result, Track.class);
	}

	/**
	 * Get the most loved tracks chart.
	 *
	 * @param apiKey A Last.fm API key
	 * @return Most loved tracks chart
	 */
	public static PaginatedResult<Track> getLovedTracks(String apiKey) {
		return getLovedTracks(1, apiKey);
	}

	/**
	 * Get the most loved tracks chart.
	 *
	 * @param page The page to fetch
	 * @param apiKey A Last.fm API key
	 * @return Most loved tracks chart
	 */
	public static PaginatedResult<Track> getLovedTracks(int page, String apiKey) {
		Result result = Caller.getInstance().call("chart.getLovedTracks", apiKey, "page", String.valueOf(page));
		return ResponseBuilder.buildPaginatedResult(result, Track.class);
	}

	/**
	 * Get the hyped tracks chart.
	 *
	 * @param apiKey A Last.fm API key
	 * @return Hyped tracks chart
	 */
	public static PaginatedResult<Track> getHypedTracks(String apiKey) {
		return getHypedTracks(1, apiKey);
	}

	/**
	 * Get the hyped tracks chart.
	 *
	 * @param page The page to fetch
	 * @param apiKey A Last.fm API key
	 * @return Hyped tracks chart
	 */
	public static PaginatedResult<Track> getHypedTracks(int page, String apiKey) {
		Result result = Caller.getInstance().call("chart.getHypedTracks", apiKey, "page", String.valueOf(page));
		return ResponseBuilder.buildPaginatedResult(result, Track.class);
	}

	/**
	 * Get the hyped artists chart.
	 *
	 * @param apiKey A Last.fm API key
	 * @return Hyped artists chart
	 */
	public static PaginatedResult<Artist> getHypedArtists(String apiKey) {
		return getHypedArtists(1, apiKey);
	}

	/**
	 * Get the hyped artists chart.
	 *
	 * @param page The page to fetch
	 * @param apiKey A Last.fm API key
	 * @return Hyped artists chart
	 */
	public static PaginatedResult<Artist> getHypedArtists(int page, String apiKey) {
		Result result = Caller.getInstance().call("chart.getHypedArtists", apiKey, "page", String.valueOf(page));
		return ResponseBuilder.buildPaginatedResult(result, Artist.class);
	}
}
