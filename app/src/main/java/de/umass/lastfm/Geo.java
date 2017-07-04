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
import de.umass.util.StringUtilities;
import de.umass.xml.DomElement;

/**
 * Provides nothing more than a namespace for the API methods starting with geo.
 *
 * @author Janni Kovacs
 */
public class Geo {

	/**
	 * This inner class represents a Metro, which is composed of its name and the name of its country.
	 *
	 * @see Geo#getMetros(String, String)
	 */
	public static class Metro {
		private String name;
		private String country;

		public Metro(String name, String country) {
			this.name = name;
			this.country = country;
		}

		public String getName() {
			return name;
		}

		public String getCountry() {
			return country;
		}
	}

	private Geo() {
	}

	/**
	 * Get all events in a specific location by country or city name.<br/> This method returns <em>all</em> events by subsequently calling
	 * {@link #getEvents(String, String, int, String)} and concatenating the single results into one list.<br/> Pay attention if you use this
	 * method as it may produce a lot of network traffic and therefore may consume a long time.
	 *
	 * @param location Specifies a location to retrieve events for
	 * @param distance Find events within a specified radius (in kilometres)
	 * @param apiKey A Last.fm API key.
	 * @return a list containing all events
	 */
	public static Collection<Event> getAllEvents(String location, String distance, String apiKey) {
		Collection<Event> events = null;
		int page = 1, total;
		do {
			PaginatedResult<Event> result = getEvents(location, distance, page, apiKey);
			total = result.getTotalPages();
			Collection<Event> pageResults = result.getPageResults();
			if (events == null) {
				// events is initialized here to initialize it with the right size and avoid array copying later on
				events = new ArrayList<Event>(total * pageResults.size());
			}
			for (Event artist : pageResults) {
				events.add(artist);
			}
			page++;
		} while (page <= total);
		return events;
	}

	/**
	 * Get all events in a specific location by country or city name.<br/> This method only returns the first page of a possibly paginated
	 * result. To retrieve all pages get the total number of pages via {@link de.umass.lastfm.PaginatedResult#getTotalPages()} and subsequently
	 * call {@link #getEvents(String, String, int, String)} with the successive page numbers.
	 *
	 * @param location Specifies a location to retrieve events for
	 * @param distance Find events within a specified radius (in kilometres)
	 * @param apiKey A Last.fm API key.
	 * @return a {@link PaginatedResult} containing a list of events
	 */
	public static PaginatedResult<Event> getEvents(String location, String distance, String apiKey) {
		return getEvents(location, distance, 1, apiKey);
	}

	/**
	 * Get all events in a specific location by country or city name.<br/> This method only returns the specified page of a paginated result.
	 *
	 * @param location Specifies a location to retrieve events for
	 * @param distance Find events within a specified radius (in kilometres)
	 * @param page A page number for pagination
	 * @param apiKey A Last.fm API key.
	 * @return a {@link PaginatedResult} containing a list of events
	 */
	public static PaginatedResult<Event> getEvents(String location, String distance, int page, String apiKey) {
		return getEvents(location, distance, page, -1, apiKey);
	}

	public static PaginatedResult<Event> getEvents(String location, String distance, int page, int limit, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("page", String.valueOf(page));
		MapUtilities.nullSafePut(params, "location", location);
		MapUtilities.nullSafePut(params, "distance", distance);
		MapUtilities.nullSafePut(params, "limit", limit);
		Result result = Caller.getInstance().call("geo.getEvents", apiKey, params);
		return ResponseBuilder.buildPaginatedResult(result, Event.class);
	}

	/**
	 * Get all events in a specific location by latitude/longitude.<br/> This method only returns the specified page of a paginated result.
	 *
	 * @param latitude Latitude
	 * @param longitude Longitude
	 * @param page A page number for pagination
	 * @param apiKey A Last.fm API key.
	 * @return a {@link PaginatedResult} containing a list of events
	 */
	public static PaginatedResult<Event> getEvents(double latitude, double longitude, int page, String apiKey) {
		return getEvents(latitude, longitude, page, -1, apiKey);
	}

	public static PaginatedResult<Event> getEvents(double latitude, double longitude, int page, int limit, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("page", String.valueOf(page));
		params.put("lat", String.valueOf(latitude));
		params.put("long", String.valueOf(longitude));
		MapUtilities.nullSafePut(params, "limit", limit);
		Result result = Caller.getInstance().call("geo.getEvents", apiKey, params);
		return ResponseBuilder.buildPaginatedResult(result, Event.class);
	}

	public static PaginatedResult<Event> getEvents(double latitude, double longitude, String distance, String apiKey) {
		return getEvents(latitude, longitude, distance, -1, -1, apiKey);
	}

	/**
	 * Get all events within the specified distance of the location specified by latitude/longitude.<br/>
	 * This method only returns the specified page of a paginated result.
	 *
	 * @param latitude Latitude
	 * @param longitude Longitude
	 * @param distance Find events within a specified radius (in kilometres)
	 * @param page A page number for pagination
	 * @param limit The maximum number of items returned per page
	 * @param apiKey A Last.fm API key.
	 * @return a {@link PaginatedResult} containing a list of events
	 */
	public static PaginatedResult<Event> getEvents(double latitude, double longitude, String distance, int page, int limit, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("lat", String.valueOf(latitude));
		params.put("long", String.valueOf(longitude));
		params.put("distance", distance);
		MapUtilities.nullSafePut(params, "page", page);
		MapUtilities.nullSafePut(params, "limit", limit);
		Result result = Caller.getInstance().call("geo.getEvents", apiKey, params);
		return ResponseBuilder.buildPaginatedResult(result, Event.class);
	}

	/**
	 * Get the most popular artists on Last.fm by country
	 *
	 * @param country A country name, as defined by the ISO 3166-1 country names standard
	 * @param apiKey A Last.fm API key.
	 * @return list of Artists
	 */
	public static Collection<Artist> getTopArtists(String country, String apiKey) {
		Result result = Caller.getInstance().call("geo.getTopArtists", apiKey, "country", country);
		return ResponseBuilder.buildCollection(result, Artist.class);
	}

	/**
	 * Get the most popular tracks on Last.fm by country
	 *
	 * @param country A country name, as defined by the ISO 3166-1 country names standard
	 * @param apiKey A Last.fm API key.
	 * @return a list of Tracks
	 */
	public static Collection<Track> getTopTracks(String country, String apiKey) {
		Result result = Caller.getInstance().call("geo.getTopTracks", apiKey, "country", country);
		return ResponseBuilder.buildCollection(result, Track.class);
	}

	/**
	 * Get a list of valid countries and {@link Metro}s for use in the other webservices.
	 *
	 * @param apiKey A Last.fm API key
	 * @return a List of {@link Metro}s
	 */
	public static Collection<Metro> getMetros(String apiKey) {
		return getMetros(null, apiKey);
	}

	/**
	 * Get a list of valid countries and {@link Metro}s for use in the other webservices.
	 *
	 * @param country Optionally restrict the results to those Metros from a particular country, as defined by the ISO 3166-1 country names
	 * standard
	 * @param apiKey A Last.fm API key
	 * @return a List of {@link Metro}s
	 */
	public static Collection<Metro> getMetros(String country, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		MapUtilities.nullSafePut(params, "country", country);
		Result result = Caller.getInstance().call("geo.getMetros", apiKey, params);
		if (!result.isSuccessful())
			return Collections.emptyList();
		Collection<DomElement> children = result.getContentElement().getChildren("metro");
		Collection<Metro> metros = new ArrayList<Metro>(children.size());
		for (DomElement child : children) {
			metros.add(new Metro(child.getChildText("name"), child.getChildText("country")));
		}
		return metros;
	}

	/**
	 * Get a list of available chart periods for this metro, expressed as date ranges which can be sent to the chart services.
	 *
	 * @param metro The name of the metro, or <code>null</code>
	 * @param apiKey A Last.fm API key
	 * @return a list of available charts as a Map
	 */
	public static LinkedHashMap<String, String> getMetroWeeklyChartList(String metro, String apiKey) {
		return Chart.getWeeklyChartList("geo.getMetroWeeklyChartList", "metro", metro, apiKey);
	}

	public static Chart<Artist> getMetroArtistChart(String country, String metro, String apiKey) {
		return getMetroArtistChart(country, metro, null, null, apiKey);
	}

	public static Chart<Artist> getMetroArtistChart(Metro metro, String start, String end, String apiKey) {
		return getMetroArtistChart(metro.getCountry(), metro.getName(), start, end, apiKey);
	}

	public static Chart<Artist> getMetroArtistChart(String country, String metro, String start, String end, String apiKey) {
		return Chart.getChart("geo.getMetroArtistChart", "artist", StringUtilities.map("country", country, "metro", metro), start, end, -1,
				apiKey);
	}

	public static Chart<Track> getMetroTrackChart(String country, String metro, String apiKey) {
		return getMetroTrackChart(country, metro, null, null, apiKey);
	}

	public static Chart<Track> getMetroTrackChart(Metro metro, String start, String end, String apiKey) {
		return getMetroTrackChart(metro.getCountry(), metro.getName(), start, end, apiKey);
	}

	public static Chart<Track> getMetroTrackChart(String country, String metro, String start, String end, String apiKey) {
		return Chart.getChart("geo.getMetroTrackChart", "track", StringUtilities.map("country", country, "metro", metro), start, end, -1,
				apiKey);
	}

	public static Chart<Artist> getMetroHypeArtistChart(String country, String metro, String apiKey) {
		return getMetroHypeArtistChart(country, metro, null, null, apiKey);
	}

	public static Chart<Artist> getMetroHypeArtistChart(Metro metro, String start, String end, String apiKey) {
		return getMetroHypeArtistChart(metro.getCountry(), metro.getName(), start, end, apiKey);
	}

	public static Chart<Artist> getMetroHypeArtistChart(String country, String metro, String start, String end, String apiKey) {
		return Chart.getChart("geo.getMetroHypeArtistChart", "artist", StringUtilities.map("country", country, "metro", metro), start, end,
				-1, apiKey);
	}

	public static Chart<Track> getMetroHypeTrackChart(String country, String metro, String apiKey) {
		return getMetroHypeTrackChart(country, metro, null, null, apiKey);
	}

	public static Chart<Track> getMetroHypeTrackChart(Metro metro, String start, String end, String apiKey) {
		return getMetroHypeTrackChart(metro.getCountry(), metro.getName(), start, end, apiKey);
	}

	public static Chart<Track> getMetroHypeTrackChart(String country, String metro, String start, String end, String apiKey) {
		return Chart.getChart("geo.getMetroHypeTrackChart", "track", StringUtilities.map("country", country, "metro", metro), start, end,
				-1, apiKey);
	}

	public static Chart<Artist> getMetroUniqueArtistChart(String country, String metro, String apiKey) {
		return getMetroUniqueArtistChart(country, metro, null, null, apiKey);
	}

	public static Chart<Artist> getMetroUniqueArtistChart(Metro metro, String start, String end, String apiKey) {
		return getMetroUniqueArtistChart(metro.getCountry(), metro.getName(), start, end, apiKey);
	}

	public static Chart<Artist> getMetroUniqueArtistChart(String country, String metro, String start, String end, String apiKey) {
		return Chart.getChart("geo.getMetroUniqueArtistChart", "artist", StringUtilities.map("country", country, "metro", metro), start,
				end, -1, apiKey);
	}

	public static Chart<Track> getMetroUniqueTrackChart(String country, String metro, String apiKey) {
		return getMetroUniqueTrackChart(country, metro, null, null, apiKey);
	}

	public static Chart<Track> getMetroUniqueTrackChart(Metro metro, String start, String end, String apiKey) {
		return getMetroUniqueTrackChart(metro.getCountry(), metro.getName(), start, end, apiKey);
	}

	public static Chart<Track> getMetroUniqueTrackChart(String country, String metro, String start, String end, String apiKey) {
		return Chart.getChart("geo.getMetroUniqueTrackChart", "track", StringUtilities.map("country", country, "metro", metro), start, end,
				-1, apiKey);
	}
}
