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
 * Contains user information and provides bindings to the methods in the user. namespace.
 *
 * @author Janni Kovacs
 */
public class User extends ImageHolder {

	static final ItemFactory<User> FACTORY = new UserFactory();

	private String id;
	private String name;
	private String url;

	private String realname;

	private String language;
	private String country;
	private int age = -1;
	private String gender;
	private boolean subscriber;
	private int numPlaylists;
	private int playcount;
	private Date registeredDate;

	private User(String name, String url) {
		this.name = name;
		this.url = url;
	}

	public String getName() {
		return name;
	}

	public String getRealname() {
		return realname;
	}

	public String getUrl() {
		return url;
	}

	public int getAge() {
		return age;
	}

	public String getCountry() {
		return country;
	}

	public String getGender() {
		return gender;
	}

	public String getLanguage() {
		return language;
	}

	public int getNumPlaylists() {
		return numPlaylists;
	}

	public int getPlaycount() {
		return playcount;
	}

	public boolean isSubscriber() {
		return subscriber;
	}

	public String getImageURL() {
		return getImageURL(ImageSize.MEDIUM);
	}

	public String getId() {
		return id;
	}

	public Date getRegisteredDate() {
		return registeredDate;
	}

	/**
	 * Get a list of tracks by a given artist scrobbled by this user, including scrobble time. Can be limited to specific timeranges, defaults
	 * to all time.
	 *
	 * @param user The last.fm username to fetch the recent tracks of
	 * @param artist The artist name you are interested in
	 * @param apiKey A Last.fm API key
	 * @return a list of Tracks
	 */
	public static PaginatedResult<Track> getArtistTracks(String user, String artist, String apiKey) {
		return getArtistTracks(user, artist, 1, 0, 0, apiKey);
	}

	/**
	 * Get a list of tracks by a given artist scrobbled by this user, including scrobble time. Can be limited to specific timeranges, defaults
	 * to all time.
	 *
	 * @param user The last.fm username to fetch the recent tracks of
	 * @param artist The artist name you are interested in
	 * @param page An integer used to fetch a specific page of tracks
	 * @param startTimestamp An unix timestamp to start at
	 * @param endTimestamp An unix timestamp to end at
	 * @param apiKey A Last.fm API key
	 * @return a list of Tracks
	 */
	public static PaginatedResult<Track> getArtistTracks(String user, String artist, int page, long startTimestamp, long endTimestamp, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("user", user);
		params.put("artist", artist);
		params.put("page", String.valueOf(page));
		params.put("startTimestamp", String.valueOf(startTimestamp));
		params.put("endTimestamp", String.valueOf(endTimestamp));
		Result result = Caller.getInstance().call("user.getArtistTracks", apiKey, params);
		return ResponseBuilder.buildPaginatedResult(result, Track.class);
	}

	public static PaginatedResult<User> getFriends(String user, String apiKey) {
		return getFriends(user, false, 1, 50, apiKey);
	}

	public static PaginatedResult<User> getFriends(String user, boolean recenttracks, int page, int limit, String apiKey) {
		Result result = Caller.getInstance().call("user.getFriends", apiKey, "user", user, "recenttracks",
				String.valueOf(recenttracks ? 1 : 0), "limit", String.valueOf(limit), "page", String.valueOf(page));
		return ResponseBuilder.buildPaginatedResult(result, User.class);
	}

	public static Collection<User> getNeighbours(String user, String apiKey) {
		return getNeighbours(user, 100, apiKey);
	}

	public static Collection<User> getNeighbours(String user, int limit, String apiKey) {
		Result result = Caller.getInstance().call("user.getNeighbours", apiKey, "user", user, "limit", String.valueOf(limit));
		return ResponseBuilder.buildCollection(result, User.class);
	}

	public static PaginatedResult<Track> getRecentTracks(String user, String apiKey) {
		return getRecentTracks(user, 1, 10, apiKey);
	}

	public static PaginatedResult<Track> getRecentTracks(String user, int page, int limit, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("user", user);
		params.put("limit", String.valueOf(limit));
		params.put("page", String.valueOf(page));
		Result result = Caller.getInstance().call("user.getRecentTracks", apiKey, params);
		return ResponseBuilder.buildPaginatedResult(result, Track.class);
	}

	public static Collection<Album> getTopAlbums(String user, String apiKey) {
		return getTopAlbums(user, Period.OVERALL, apiKey);
	}

	public static Collection<Album> getTopAlbums(String user, Period period, String apiKey) {
		Result result = Caller.getInstance().call("user.getTopAlbums", apiKey, "user", user, "period", period.getString());
		return ResponseBuilder.buildCollection(result, Album.class);
	}

	public static Collection<Artist> getTopArtists(String user, String apiKey) {
		return getTopArtists(user, Period.OVERALL, apiKey);
	}

	public static Collection<Artist> getTopArtists(String user, Period period, String apiKey) {
		Result result = Caller.getInstance().call("user.getTopArtists", apiKey, "user", user, "period", period.getString());
		return ResponseBuilder.buildCollection(result, Artist.class);
	}

	public static Collection<Track> getTopTracks(String user, String apiKey) {
		return getTopTracks(user, Period.OVERALL, apiKey);
	}

	public static Collection<Track> getTopTracks(String user, Period period, String apiKey) {
		Result result = Caller.getInstance().call("user.getTopTracks", apiKey, "user", user, "period", period.getString());
		return ResponseBuilder.buildCollection(result, Track.class);
	}

	public static Collection<Tag> getTopTags(String user, String apiKey) {
		return getTopTags(user, -1, apiKey);
	}

	public static Collection<Tag> getTopTags(String user, int limit, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("user", user);
		MapUtilities.nullSafePut(params, "limit", limit);
		Result result = Caller.getInstance().call("user.getTopTags", apiKey, params);
		return ResponseBuilder.buildCollection(result, Tag.class);
	}

	public static Chart<Album> getWeeklyAlbumChart(String user, String apiKey) {
		return getWeeklyAlbumChart(user, null, null, -1, apiKey);
	}

	public static Chart<Album> getWeeklyAlbumChart(String user, int limit, String apiKey) {
		return getWeeklyAlbumChart(user, null, null, limit, apiKey);
	}

	public static Chart<Album> getWeeklyAlbumChart(String user, String from, String to, int limit, String apiKey) {
		return Chart.getChart("user.getWeeklyAlbumChart", "user", user, "album", from, to, limit, apiKey);
	}

	public static Chart<Artist> getWeeklyArtistChart(String user, String apiKey) {
		return getWeeklyArtistChart(user, null, null, -1, apiKey);
	}

	public static Chart<Artist> getWeeklyArtistChart(String user, int limit, String apiKey) {
		return getWeeklyArtistChart(user, null, null, limit, apiKey);
	}

	public static Chart<Artist> getWeeklyArtistChart(String user, String from, String to, int limit, String apiKey) {
		return Chart.getChart("user.getWeeklyArtistChart", "user", user, "artist", from, to, limit, apiKey);
	}

	public static Chart<Track> getWeeklyTrackChart(String user, String apiKey) {
		return getWeeklyTrackChart(user, null, null, -1, apiKey);
	}

	public static Chart<Track> getWeeklyTrackChart(String user, int limit, String apiKey) {
		return getWeeklyTrackChart(user, null, null, limit, apiKey);
	}

	public static Chart<Track> getWeeklyTrackChart(String user, String from, String to, int limit, String apiKey) {
		return Chart.getChart("user.getWeeklyTrackChart", "user", user, "track", from, to, limit, apiKey);
	}

	public static LinkedHashMap<String, String> getWeeklyChartList(String user, String apiKey) {
		return Chart.getWeeklyChartList("user.getWeeklyChartList", "user", user, apiKey);
	}

	public static Collection<Chart> getWeeklyChartListAsCharts(String user, String apiKey) {
		return Chart.getWeeklyChartListAsCharts("user", user, apiKey);
	}

	/**
	 * GetS a list of upcoming events that this user is attending.
	 *
	 * @param user The user to fetch the events for.
	 * @param apiKey A Last.fm API key.
	 * @return a list of upcoming events
	 */
	public static PaginatedResult<Event> getEvents(String user, String apiKey) {
		return getEvents(user, -1, apiKey);
	}

	/**
	 * GetS a list of upcoming events that this user is attending.
	 *
	 * @param user The user to fetch the events for.
	 * @param page The page number to fetch. Defaults to first page.
	 * @param apiKey A Last.fm API key.
	 * @return a list of upcoming events
	 */
	public static PaginatedResult<Event> getEvents(String user, int page, String apiKey) {
		return getEvents(user, false, page, -1, apiKey);
	}

	/**
	 * GetS a list of upcoming events that this user is attending.
	 *
	 * @param user The user to fetch the events for.
	 * @param page The page number to fetch. Defaults to first page.
	 * @param limit The number of results to fetch per page. Defaults to 50.
	 * @param festivalsOnly Whether only festivals should be returned, or all events.
	 * @param apiKey A Last.fm API key.
	 * @return a list of upcoming events
	 */
	public static PaginatedResult<Event> getEvents(String user, boolean festivalsOnly, int page, int limit, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		MapUtilities.nullSafePut(params, "user", user);
		MapUtilities.nullSafePut(params, "page", page);
		MapUtilities.nullSafePut(params, "limit", limit);
		if (festivalsOnly) {
			params.put("festivalsonly", "1");
		}
		Result result = Caller.getInstance().call("user.getEvents", apiKey, params);
		return ResponseBuilder.buildPaginatedResult(result, Event.class);
	}

	/**
	 * Get the first page of a paginated result of all events a user has attended in the past.
	 *
	 * @param user The username to fetch the events for.
	 * @param apiKey A Last.fm API key.
	 * @return a list of past {@link Event}s
	 */
	public static PaginatedResult<Event> getPastEvents(String user, String apiKey) {
		return getPastEvents(user, -1, apiKey);
	}

	/**
	 * Gets a paginated list of all events a user has attended in the past.
	 *
	 * @param user The username to fetch the events for.
	 * @param page The page number to scan to.
	 * @param apiKey A Last.fm API key.
	 * @return a list of past {@link Event}s
	 */
	public static PaginatedResult<Event> getPastEvents(String user, int page, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("user", user);
		MapUtilities.nullSafePut(params, "page", page);
		Result result = Caller.getInstance().call("user.getPastEvents", apiKey, params);
		return ResponseBuilder.buildPaginatedResult(result, Event.class);
	}

	public static PaginatedResult<Event> getRecommendedEvents(Session session) {
		return getRecommendedEvents(1, session);
	}

	public static PaginatedResult<Event> getRecommendedEvents(int page, Session session) {
		Result result = Caller.getInstance().call("user.getRecommendedEvents", session, "page", String.valueOf(page), "user",
				session.getUsername());
		return ResponseBuilder.buildPaginatedResult(result, Event.class);
	}

	/**
	 * Gets a list of a user's playlists on Last.fm. Note that this method only fetches metadata regarding the user's playlists. If you want to
	 * retrieve the list of tracks in a playlist use {@link Playlist#fetch(String, String) Playlist.fetch()}.
	 *
	 * @param user The last.fm username to fetch the playlists of.
	 * @param apiKey A Last.fm API key.
	 * @return a list of Playlists
	 */
	public static Collection<Playlist> getPlaylists(String user, String apiKey) {
		Result result = Caller.getInstance().call("user.getPlaylists", apiKey, "user", user);
		if (!result.isSuccessful())
			return Collections.emptyList();
		Collection<Playlist> playlists = new ArrayList<Playlist>();
		for (DomElement element : result.getContentElement().getChildren("playlist")) {
			playlists.add(ResponseBuilder.buildItem(element, Playlist.class));
		}
		return playlists;
	}

	/**
	 * Retrieves the loved tracks by a user.
	 *
	 * @param user The user name to fetch the loved tracks for.
	 * @param apiKey A Last.fm API key.
	 * @return the loved tracks
	 */
	public static PaginatedResult<Track> getLovedTracks(String user, String apiKey) {
		return getLovedTracks(user, 1, apiKey);
	}

	/**
	 * Retrieves the loved tracks by a user.
	 *
	 * @param user The user name to fetch the loved tracks for.
	 * @param page The page number to scan to
	 * @param apiKey A Last.fm API key.
	 * @return the loved tracks
	 */
	public static PaginatedResult<Track> getLovedTracks(String user, int page, String apiKey) {
		Result result = Caller.getInstance().call("user.getLovedTracks", apiKey, "user", user, "page", String.valueOf(page));
		return ResponseBuilder.buildPaginatedResult(result, Track.class);
	}

	/**
	 * Retrieves profile information about the specified user.
	 *
	 * @param user A username
	 * @param apiKey A Last.fm API key.
	 * @return User info
	 */
	public static User getInfo(String user, String apiKey) {
		Result result = Caller.getInstance().call("user.getInfo", apiKey, "user", user);
		return ResponseBuilder.buildItem(result, User.class);
	}

	/**
	 * Retrieves profile information about the authenticated user.
	 *
	 * @param session A session for the user, for whom to get the profile for
	 * @return User info
	 */
	public static User getInfo(Session session) {
		Result result = Caller.getInstance().call("user.getInfo", session);
		return ResponseBuilder.buildItem(result, User.class);
	}

	/**
	 * Get Last.fm artist recommendations for a user.
	 *
	 * @param session A Session instance
	 * @return a list of {@link Artist}s
	 */
	public static PaginatedResult<Artist> getRecommendedArtists(Session session) {
		return getRecommendedArtists(1, session);
	}

	/**
	 * Get Last.fm artist recommendations for a user.
	 *
	 * @param page The page to fetch
	 * @param session A Session instance
	 * @return a list of {@link Artist}s
	 */
	public static PaginatedResult<Artist> getRecommendedArtists(int page, Session session) {
		Result result = Caller.getInstance().call("user.getRecommendedArtists", session, "page", String.valueOf(page));
		return ResponseBuilder.buildPaginatedResult(result, Artist.class);
	}

	/**
	 * Shout on this user's shoutbox
	 *
	 * @param user The name of the user to shout on
	 * @param message The message to post to the shoutbox
	 * @param session A Session instance
	 * @return the result of the operation
	 */
	public static Result shout(String user, String message, Session session) {
		return Caller.getInstance().call("user.shout", session, "user", user, "message", message);
	}

	/**
	 * Gets a list of forthcoming releases based on a user's musical taste.
	 *
	 * @param user The Last.fm username
	 * @param apiKey A Last.fm API key
	 * @return a Collection of new {@link Album} releases
	 */
	public static Collection<Album> getNewReleases(String user, String apiKey) {
		return getNewReleases(user, false, apiKey);
	}

	/**
	 * Gets a list of forthcoming releases based on a user's musical taste.
	 *
	 * @param user The Last.fm username
	 * @param useRecommendations If <code>true</code>, the feed contains new releases based on Last.fm's artist recommendations for this user.
	 * Otherwise, it is based on their library (the default)
	 * @param apiKey A Last.fm API key
	 * @return a Collection of new {@link Album} releases
	 */
	public static Collection<Album> getNewReleases(String user, boolean useRecommendations, String apiKey) {
		Result result = Caller.getInstance().call("user.getNewReleases", apiKey, "user", user, "userecs", useRecommendations ? "1" : "0");
		return ResponseBuilder.buildCollection(result, Album.class);
	}

	/**
	 * Returns the tracks banned by the user.
	 *
	 * @param user The user name
	 * @param apiKey A Last.fm API key
	 * @return the banned tracks
	 */
	public static PaginatedResult<Track> getBannedTracks(String user, String apiKey) {
		return getBannedTracks(user, 1, apiKey);
	}

	/**
	 * Returns the tracks banned by the user.
	 *
	 * @param user The user name
	 * @param page The page number to fetch
	 * @param apiKey A Last.fm API key
	 * @return the banned tracks
	 */
	public static PaginatedResult<Track> getBannedTracks(String user, int page, String apiKey) {
		Result result = Caller.getInstance().call("user.getBannedTracks", apiKey, "user", user, "page", String.valueOf(page));
		return ResponseBuilder.buildPaginatedResult(result, Track.class);
	}

	/**
	 * Get shouts for a user.
	 *
	 * @param user The username to fetch shouts for
	 * @param apiKey A Last.fm API key.
	 * @return a page of <code>Shout</code>s
	 */
	public static PaginatedResult<Shout> getShouts(String user, String apiKey) {
		return getShouts(user, -1, -1, apiKey);
	}

	/**
	 * Get shouts for a user.
	 *
	 * @param user The username to fetch shouts for
	 * @param page The page number to fetch
	 * @param apiKey A Last.fm API key.
	 * @return a page of <code>Shout</code>s
	 */
	public static PaginatedResult<Shout> getShouts(String user, int page, String apiKey) {
		return getShouts(user, page, -1, apiKey);
	}

	/**
	 * Get shouts for a user.
	 *
	 * @param user The username to fetch shouts for
	 * @param page The page number to fetch
	 * @param limit An integer used to limit the number of shouts returned per page or -1 for default
	 * @param apiKey A Last.fm API key.
	 * @return a page of <code>Shout</code>s
	 */
	public static PaginatedResult<Shout> getShouts(String user, int page, int limit, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("user", user);
		MapUtilities.nullSafePut(params, "limit", limit);
		MapUtilities.nullSafePut(params, "page", page);
		Result result = Caller.getInstance().call("user.getShouts", apiKey, params);
		return ResponseBuilder.buildPaginatedResult(result, Shout.class);
	}

	/**
	 * Get the user's personal tags.
	 *
	 * @param user The user who performed the taggings
	 * @param tag The tag you're interested in
	 * @param taggingType Either <code>Artist.class</code>, <code>Album.class</code> or <code>Track.class</code>
	 * @param apiKey A Last.fm API key
	 * @return the items the user has tagged with the specified tag
	 * @throws IllegalArgumentException if <code>taggingType</code> is <code>null</code> or not one of the above mentioned classes
	 */
	public static <T extends MusicEntry> PaginatedResult<T> getPersonalTags(String user, String tag, Class<T> taggingType, String apiKey) {
		return getPersonalTags(user, tag, taggingType, -1, -1, apiKey);
	}

	/**
	 * Get the user's personal tags.
	 *
	 * @param user The user who performed the taggings
	 * @param tag The tag you're interested in
	 * @param taggingType Either <code>Artist.class</code>, <code>Album.class</code> or <code>Track.class</code>
	 * @param page The page number to fetch
	 * @param apiKey A Last.fm API key
	 * @return the items the user has tagged with the specified tag
	 * @throws IllegalArgumentException if <code>taggingType</code> is <code>null</code> or not one of the above mentioned classes
	 */
	public static <T extends MusicEntry> PaginatedResult<T> getPersonalTags(String user, String tag, Class<T> taggingType, int page, String apiKey) {
		return getPersonalTags(user, tag, taggingType, page, -1, apiKey);
	}

	/**
	 * Get the user's personal tags.
	 *
	 * @param user The user who performed the taggings
	 * @param tag The tag you're interested in
	 * @param taggingType Either <code>Artist.class</code>, <code>Album.class</code> or <code>Track.class</code>
	 * @param page The page number to fetch
	 * @param limit The number of results to fetch per page. Defaults to 50
	 * @param apiKey A Last.fm API key
	 * @return the items the user has tagged with the specified tag
	 * @throws IllegalArgumentException if <code>taggingType</code> is <code>null</code> or not one of the above mentioned classes
	 */
	public static <T extends MusicEntry> PaginatedResult<T> getPersonalTags(String user, String tag, Class<T> taggingType, int page, int limit, String apiKey) {
		Map<String, String> params = StringUtilities.map("user", user, "tag", tag);
		MapUtilities.nullSafePut(params, "page", page);
		MapUtilities.nullSafePut(params, "limit", limit);

		String taggingTypeParam = "taggingtype";
		if (taggingType == Track.class)
			params.put(taggingTypeParam, "track");
		else if (taggingType == Artist.class)
			params.put(taggingTypeParam, "artist");
		else if (taggingType == Album.class)
			params.put(taggingTypeParam, "album");
		else
			throw new IllegalArgumentException("Parameter taggingType has to be one of Artist.class, Album.class or Track.class.");

		Result result = Caller.getInstance().call("user.getPersonalTags", apiKey, params);
		if (!result.isSuccessful())
			return new PaginatedResult<T>(0, 0, Collections.<T>emptyList());

		String childElementName = params.get(taggingTypeParam) + "s";
		DomElement contentElement = result.getContentElement();
		DomElement childElement = contentElement.getChild(childElementName);
		return ResponseBuilder.buildPaginatedResult(contentElement, childElement, taggingType);
	}


	private static class UserFactory implements ItemFactory<User> {
		public User createItemFromElement(DomElement element) {
			User user = new User(element.getChildText("name"), element.getChildText("url"));
			user.id = element.getChildText("id");
			if (element.hasChild("realname"))
				user.realname = element.getChildText("realname");
			ImageHolder.loadImages(user, element);
			user.language = element.getChildText("lang");
			user.country = element.getChildText("country");
			if (element.hasChild("age")) {
				try {
					user.age = Integer.parseInt(element.getChildText("age"));
				} catch (NumberFormatException e) {
					// no age
				}
			}
			user.gender = element.getChildText("gender");
			user.subscriber = "1".equals(element.getChildText("subscriber"));
			if (element.hasChild("playcount")) { // extended user information
				try {
					user.playcount = Integer.parseInt(element.getChildText("playcount"));
				} catch (NumberFormatException e) {
					// no playcount
				}
			}
			if (element.hasChild("playlists")) { // extended user information
				try {
					user.numPlaylists = Integer.parseInt(element.getChildText("playlists"));
				} catch (NumberFormatException e) {
					// no playlists
				}
			}
			if (element.hasChild("registered")) {
				String unixtime = element.getChild("registered").getAttribute("unixtime");
				user.registeredDate = new Date(Long.parseLong(unixtime) * 1000);
			}
			return user;
		}
	}
}
