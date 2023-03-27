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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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
	private Track recentTrack;
    private int artistCount = -1;
    private int albumCount = -1;
    private int trackCount = -1;

    public User(String name, String url) {
		this.name = name;
		this.url = url;
	}

    public User(String name, String url, String realname, String country, long registeredTime, Map<ImageSize, String> imageUrls) {
        this.name = name;
        this.url = url;
        this.realname = realname;
        this.country = country;
        this.registeredDate = new Date(registeredTime);
        this.imageUrls = imageUrls;
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

	public void setPlaycount(int playcount) {
		this.playcount = playcount;
	}

	public boolean isSubscriber() {
		return subscriber;
	}

	public Map<ImageSize, String> getImageUrls() {
		return imageUrls;
	}

	public String getImageURL() {
		return getImageURL(ImageSize.MEDIUM);
	}

	public void setImageURL(String url) {
	    imageUrls.put(ImageSize.MEDIUM, url);
	}

	public String getId() {
		return id;
	}

	public Date getRegisteredDate() {
		return registeredDate;
	}

	public Track getRecentTrack() {
		return recentTrack;
	}
	public void setRecentTrack(Track recentTrack) {
		this.recentTrack = recentTrack;
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

	public static PaginatedResult<User> getFriends(String user, Session session) {
		return getFriends(user, 1, 50, session);
	}

	public static PaginatedResult<User> getFriends(String user, int page, int limit, Session session) {
        Map<String, String> params = new HashMap<String, String>();
        MapUtilities.nullSafePut(params, "user", user);
        params.put("page", String.valueOf(page));
        params.put("limit", String.valueOf(limit));
        Result result = Caller.getInstance().call(null, "user.getFriends",
                null, params, session);
		return ResponseBuilder.buildPaginatedResult(result, User.class);
	}

	public static Collection<User> getNeighbours(String user, String apiKey) {
		return getNeighbours(user, 100, apiKey);
	}

	public static Collection<User> getNeighbours(String user, int limit, String apiKey) {
		Result result = Caller.getInstance().call("user.getNeighbours", apiKey, "user", user, "limit", String.valueOf(limit));
		return ResponseBuilder.buildCollection(result, User.class);
	}

	public static PaginatedResult<Track> getRecentTracks(String user, Session session) {
		return getRecentTracks(user, 1, 10, session);
	}

	public static PaginatedResult<Track> getRecentTracks(String user, int page, int limit, Session session) {
        return getRecentTracks(user, page, limit, false, 0, 0, session);
    }
	public static PaginatedResult<Track> getRecentTracks(String user, int page, int limit, boolean extended, Session session) {
        return getRecentTracks(user, page, limit, extended, 0, 0, session);
    }
	public static PaginatedResult<Track> getRecentTracks(String user, int page, int limit, long fromTime, long toTime, Session session) {
        return getRecentTracks(user, page, limit, false, fromTime, toTime, session);
    }
	public static PaginatedResult<Track> getRecentTracks(String user, int page, int limit,
                boolean extended, long fromTime, long toTime, Session session) {
		Map<String, String> params = new HashMap<String, String>();
        MapUtilities.nullSafePut(params, "user", user);
		params.put("limit", String.valueOf(limit));
		params.put("page", String.valueOf(page));
		if (extended)
		    params.put("extended", "1");
		if (fromTime > 0)
		    params.put("from", String.valueOf(fromTime));
		if (toTime > 0)
		    params.put("to", String.valueOf(toTime));
        Result result;
            result = Caller.getInstance().call(null, "user.getRecentTracks",
                null, params, session);
		return ResponseBuilder.buildPaginatedResult(result, Track.class);
	}

	public static PaginatedResult<Album> getTopAlbums(String user, Session session) {
		return getTopAlbums(user, Period.OVERALL, 50, 1, session);
	}

	public static PaginatedResult<Album> getTopAlbums(String user, Period period, int limit, int page, Session session) {
		Result result = Caller.getInstance().call("user.getTopAlbums", session,
                "user", user, "period", period.getString(), "limit", Integer.toString(limit), "page", Integer.toString(page));
		return ResponseBuilder.buildPaginatedResult(result, Album.class);
	}

	public static PaginatedResult<Artist> getTopArtists(String user, Session session) {
		return getTopArtists(user, Period.OVERALL, 50, 1, session);
	}

	public static PaginatedResult<Artist> getTopArtists(String user, Period period, int limit, int page, Session session) {
		Result result = Caller.getInstance().call("user.getTopArtists", session,
                "user", user, "period", period.getString(), "limit", Integer.toString(limit), "page", Integer.toString(page));
		return ResponseBuilder.buildPaginatedResult(result, Artist.class);
	}

	public static PaginatedResult<Track> getTopTracks(String user, Session session) {
		return getTopTracks(user, Period.OVERALL, 50, 1, session);
	}

	public static PaginatedResult<Track> getTopTracks(String user, Period period, int limit, int page, Session session) {
		Result result = Caller.getInstance().call("user.getTopTracks", session,
                "user", user, "period", period.getString(), "limit", Integer.toString(limit), "page", Integer.toString(page));
		return ResponseBuilder.buildPaginatedResult(result, Track.class);
	}

	public static Collection<Tag> getTopTags(String user, Session session) {
		return getTopTags(user, -1, session);
	}

	public static Collection<Tag> getTopTags(String user, int limit, Session session) {
		Map<String, String> params = new HashMap<String, String>();
        MapUtilities.nullSafePut(params, "user", user);
		MapUtilities.nullSafePut(params, "limit", limit);
		Result result = Caller.getInstance().call("user.getTopTags", session, params);
		return ResponseBuilder.buildCollection(result, Tag.class);
	}

	public static Chart<Album> getWeeklyAlbumChart(String user, Session session) {
		return getWeeklyAlbumChart(user, null, null, -1, session);
	}

	public static Chart<Album> getWeeklyAlbumChart(String user, int limit, Session session) {
		return getWeeklyAlbumChart(user, null, null, limit, session);
	}

	public static Chart<Album> getWeeklyAlbumChart(String user, String from, String to, int limit, Session session) {
		return Chart.getChart("user.getWeeklyAlbumChart", "user", user, "album", from, to, limit, session);
	}

	public static Chart<Artist> getWeeklyArtistChart(String user, Session session) {
		return getWeeklyArtistChart(user, null, null, -1, session);
	}

	public static Chart<Artist> getWeeklyArtistChart(String user, int limit, Session session) {
		return getWeeklyArtistChart(user, null, null, limit, session);
	}

	public static Chart<Artist> getWeeklyArtistChart(String user, String from, String to, int limit, Session session) {
		return Chart.getChart("user.getWeeklyArtistChart", "user", user, "artist", from, to, limit, session);
	}

	public static Chart<Track> getWeeklyTrackChart(String user, Session session) {
		return getWeeklyTrackChart(user, null, null, -1, session);
	}

	public static Chart<Track> getWeeklyTrackChart(String user, int limit, Session session) {
		return getWeeklyTrackChart(user, null, null, limit, session);
	}

	public static Chart<Track> getWeeklyTrackChart(String user, String from, String to, int limit, Session session) {
		return Chart.getChart("user.getWeeklyTrackChart", "user", user, "track", from, to, limit, session);
	}

	public static LinkedHashMap<String, String> getWeeklyChartList(String user, Session session) {
		return Chart.getWeeklyChartList("user.getWeeklyChartList", "user", user, session);
	}

	public static Collection<Chart> getWeeklyChartListAsCharts(String user, Session session) {
		return Chart.getWeeklyChartListAsCharts("user", user, session);
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
     * @param session A Last.fm session.
	 * @return the loved tracks
	 */
	public static PaginatedResult<Track> getLovedTracks(String user, Session session) {
		return getLovedTracks(user, 50, 1, session);
	}

	/**
	 * Retrieves the loved tracks by a user.
	 *
	 * @param user The user name to fetch the loved tracks for.
	 * @param page The page number to scan to
	 * @param session A Last.fm session.
	 * @return the loved tracks
	 */

	public static PaginatedResult<Track> getLovedTracks(String user, int limit, int page, Session session) {
        Map<String, String> params = new HashMap<String, String>();
        MapUtilities.nullSafePut(params, "user", user);
        params.put("page", String.valueOf(page));
        params.put("limit", String.valueOf(limit));
        Result result = Caller.getInstance().call(null, "user.getLovedTracks",
                null, params, session);
		return ResponseBuilder.buildPaginatedResult(result, Track.class);
	}

	/**
	 * Retrieves profile information about the specified user.
	 *
	 * @param user A username
     * @param session A Last.fm session.
	 * @return User info
	 */
	public static User getInfo(String user, Session session) {
		Result result = Caller.getInstance().call("user.getInfo", session, "user", user);
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
     * @param session A Last.fm session.
	 * @return the items the user has tagged with the specified tag
	 * @throws IllegalArgumentException if <code>taggingType</code> is <code>null</code> or not one of the above mentioned classes
	 */
	public static <T extends MusicEntry> PaginatedResult<T> getPersonalTags(String user, String tag, Class<T> taggingType, Session session) {
		return getPersonalTags(user, tag, taggingType, -1, -1, session);
	}

	/**
	 * Get the user's personal tags.
	 *
	 * @param user The user who performed the taggings
	 * @param tag The tag you're interested in
	 * @param taggingType Either <code>Artist.class</code>, <code>Album.class</code> or <code>Track.class</code>
	 * @param page The page number to fetch
     * @param session A Last.fm session.
	 * @return the items the user has tagged with the specified tag
	 * @throws IllegalArgumentException if <code>taggingType</code> is <code>null</code> or not one of the above mentioned classes
	 */
	public static <T extends MusicEntry> PaginatedResult<T> getPersonalTags(String user, String tag, Class<T> taggingType, int page, Session session) {
		return getPersonalTags(user, tag, taggingType, page, -1, session);
	}

	/**
	 * Get the user's personal tags.
	 *
	 * @param user The user who performed the taggings
	 * @param tag The tag you're interested in
	 * @param taggingType Either <code>Artist.class</code>, <code>Album.class</code> or <code>Track.class</code>
	 * @param page The page number to fetch
	 * @param limit The number of results to fetch per page. Defaults to 50
     * @param session A Last.fm session.
	 * @return the items the user has tagged with the specified tag
	 * @throws IllegalArgumentException if <code>taggingType</code> is <code>null</code> or not one of the above mentioned classes
	 */
	public static <T extends MusicEntry> PaginatedResult<T> getPersonalTags(String user, String tag, Class<T> taggingType, int page, int limit, Session session) {
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

		Result result = Caller.getInstance().call("user.getPersonalTags", session, params);
		if (!result.isSuccessful())
			return new PaginatedResult<T>(0, 0, 0, Collections.<T>emptyList());

		String childElementName = params.get(taggingTypeParam) + "s";
		DomElement contentElement = result.getContentElement();
		DomElement childElement = contentElement.getChild(childElementName);
		return ResponseBuilder.buildPaginatedResult(contentElement, childElement, taggingType);
	}

    public static PaginatedResult<Track> getTrackScrobbles(String artist, String track, String user, Session session) {
        return getTrackScrobbles(artist, track, user, 1, 50, session);
    }

    public static PaginatedResult<Track> getTrackScrobbles(String artist, String track, String user, int page, int limit, Session session) {
        return getTrackScrobbles(artist, track, user, page, limit, 0, 0, session);
    }

    public static PaginatedResult<Track> getTrackScrobbles(String artist, String track, String user, int page, int limit,
                                                         long fromTime, long toTime, Session session) {
        Map<String, String> params = new HashMap<String, String>();
        MapUtilities.nullSafePut(params, "artist", artist);
        MapUtilities.nullSafePut(params, "track", track);
        MapUtilities.nullSafePut(params, "user", user);
        params.put("limit", String.valueOf(limit));
        params.put("page", String.valueOf(page));
        if (fromTime > 0)
            params.put("from", String.valueOf(fromTime));
        if (toTime > 0)
            params.put("to", String.valueOf(toTime));
        Result result;
        result = Caller.getInstance().call(null, "user.getTrackScrobbles",
                null, params, session);
        return ResponseBuilder.buildPaginatedResult(result, Track.class);
    }

    public int getArtistCount() {
        return artistCount;
    }

    public int getAlbumCount() {
        return albumCount;
    }

    public int getTrackCount() {
        return trackCount;
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
				try {
					user.registeredDate = new Date(Long.parseLong(unixtime) * 1000);
				} catch (NumberFormatException e) {
					// no registered date
				}
			}
			if (element.hasChild("artist_count")) {
				try {
				    user.artistCount = Integer.parseInt(element.getChildText("artist_count"));
				} catch (NumberFormatException e) {
				}
			}
			if (element.hasChild("album_count")) {
				try {
                    user.albumCount = Integer.parseInt(element.getChildText("album_count"));
				} catch (NumberFormatException e) {
				}
			}
			if (element.hasChild("track_count")) {
				try {
                    user.trackCount = Integer.parseInt(element.getChildText("track_count"));
				} catch (NumberFormatException e) {
				}
			}
			return user;
		}
	}
}
