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
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.umass.lastfm.scrobble.IgnoredMessageCode;
import de.umass.lastfm.scrobble.ScrobbleData;
import de.umass.lastfm.scrobble.ScrobbleResult;
import de.umass.util.MapUtilities;
import de.umass.util.StringUtilities;
import de.umass.xml.DomElement;

/**
 * Bean that contains information related to <code>Track</code>s and provides bindings to methods
 * in the <code>track.</code> namespace.
 *
 * @author Janni Kovacs
 */
public class Track extends MusicEntry {

	private enum ScrobbleResultType {
		NOW_PLAYING,
		SINGLE_SCROBBLE,
		MULTIPLE_SCROBBLES
	}

	static final ItemFactory<Track> FACTORY = new TrackFactory();

	public static final String ARTIST_PAGE = "artistpage";
	public static final String ALBUM_PAGE = "albumpage";
	public static final String TRACK_PAGE = "trackpage";

	private String artist;
	private String artistMbid;

	protected String album;		// protected for use in Playlist.playlistFromElement
	private String albumMbid;
	private int position = -1;

	private boolean fullTrackAvailable;
	private boolean nowPlaying;

	private Date playedWhen;
	protected int duration;		// protected for use in Playlist.playlistFromElement
	protected String location;		// protected for use in Playlist.playlistFromElement

	protected Map<String, String> lastFmExtensionInfos = new HashMap<String, String>();		// protected for use in Playlist.playlistFromElement


	protected Track(String name, String url, String artist) {
		super(name, url);
		this.artist = artist;
	}

	protected Track(String name, String url, String mbid, int playcount, int listeners, boolean streamable,
					String artist, String artistMbid, boolean fullTrackAvailable, boolean nowPlaying) {
		super(name, url, mbid, playcount, listeners, streamable);
		this.artist = artist;
		this.artistMbid = artistMbid;
		this.fullTrackAvailable = fullTrackAvailable;
		this.nowPlaying = nowPlaying;
	}

	/**
	 * Returns the duration of the song, if available, in seconds. The duration attribute is only available
	 * for tracks retrieved by {@link Playlist#fetch(String, String) Playlist.fetch} and
	 * {@link Track#getInfo(String, String, String) Track.getInfo}.
	 *
	 * @return duration in seconds
	 */
	public int getDuration() {
		return duration;
	}

	public String getArtist() {
		return artist;
	}

	public String getArtistMbid() {
		return artistMbid;
	}

	public String getAlbum() {
		return album;
	}

	public String getAlbumMbid() {
		return albumMbid;
	}

	public boolean isFullTrackAvailable() {
		return fullTrackAvailable;
	}

	public boolean isNowPlaying() {
		return nowPlaying;
	}

	/**
	 * Returns the location (URL) of this Track. This information is only available with the {@link Radio} services.
	 *
	 * @return the location
	 */
	public String getLocation() {
		return location;
	}

	/**
	 * Returns last.fm specific information about this Track. Only available in Tracks fetched from
	 * radio playlists. <tt>key</tt> can be one of the following:
	 * <ul>
	 * <li>artistpage</li>
	 * <li>albumpage</li>
	 * <li>trackpage</li>
	 * <li>buyTrackURL</li>
	 * <li>buyAlbumURL</li>
	 * <li>freeTrackURL</li>
	 * </ul>
	 * Or use the available constants in this class.<br/>
	 * Note that the key string is case sensitive.
	 *
	 * @param key A key
	 * @return associated value
	 * @see #ARTIST_PAGE
	 * @see #ALBUM_PAGE
	 * @see #TRACK_PAGE
	 */
	public String getLastFmInfo(String key) {
		return lastFmExtensionInfos.get(key);
	}

	/**
	 * Returns the time when the track was played, if this data is available (e.g. for recent tracks) or <code>null</code>,
	 * if this data is not available.<br/>
	 *
	 * @return the date when the track was played or <code>null</code>
	 */
	public Date getPlayedWhen() {
		return playedWhen;
	}

	/**
	 * Returns the position of this track in its associated album, or -1 if not available.
	 *
	 * @return the album position
	 */
	public int getPosition() {
		return position;
	}

	/**
	 * Searches for a track with the given name and returns a list of possible matches.
	 *
	 * @param track Track name
	 * @param apiKey The API key
	 * @return a list of possible matches
	 * @see #search(String, String, int, String)
	 */
	public static Collection<Track> search(String track, String apiKey) {
		return search(null, track, 30, apiKey);
	}

	/**
	 * Searches for a track with the given name and returns a list of possible matches.
	 * Specify an artist name or a limit to narrow down search results.
	 * Pass <code>null</code> for the artist parameter if you want to specify a limit but don't want
	 * to define an artist.
	 *
	 * @param artist Artist's name or <code>null</code>
	 * @param track Track name
	 * @param limit Number of maximum results
	 * @param apiKey The API key
	 * @return a list of possible matches
	 */
	public static Collection<Track> search(String artist, String track, int limit, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("track", track);
		params.put("limit", String.valueOf(limit));
		MapUtilities.nullSafePut(params, "artist", artist);
		Result result = Caller.getInstance().call("track.search", apiKey, params);
		if(!result.isSuccessful())
			return Collections.emptyList();
		DomElement element = result.getContentElement();
		DomElement matches = element.getChild("trackmatches");
		return ResponseBuilder.buildCollection(matches, Track.class);
	}

	/**
	 * Retrieves the top tags for the given track. You either have to specify a track and artist name or
	 * a mbid. If you specify an mbid you may pass <code>null</code> for the first parameter.
	 *
	 * @param artist Artist name or <code>null</code> if an MBID is specified
	 * @param trackOrMbid Track name or MBID
	 * @param apiKey The API key
	 * @return list of tags
	 */
	public static Collection<Tag> getTopTags(String artist, String trackOrMbid, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		if (StringUtilities.isMbid(trackOrMbid)) {
			params.put("mbid", trackOrMbid);
		} else {
			params.put("artist", artist);
			params.put("track", trackOrMbid);
		}
		Result result = Caller.getInstance().call("track.getTopTags", apiKey, params);
		return ResponseBuilder.buildCollection(result, Tag.class);
	}

	/**
	 * Retrieves the top fans for the given track. You either have to specify a track and artist name or
	 * a mbid. If you specify an mbid you may pass <code>null</code> for the first parameter.
	 *
	 * @param artist Artist name or <code>null</code> if an MBID is specified
	 * @param trackOrMbid Track name or MBID
	 * @param apiKey The API key
	 * @return list of fans
	 */
	public static Collection<User> getTopFans(String artist, String trackOrMbid, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		if (StringUtilities.isMbid(trackOrMbid)) {
			params.put("mbid", trackOrMbid);
		} else {
			params.put("artist", artist);
			params.put("track", trackOrMbid);
		}
		Result result = Caller.getInstance().call("track.getTopFans", apiKey, params);
		return ResponseBuilder.buildCollection(result, User.class);
	}

	/**
	 * Tag an album using a list of user supplied tags.
	 *
	 * @param artist The artist name in question
	 * @param track The track name in question
	 * @param tags A comma delimited list of user supplied tags to apply to this track. Accepts a maximum of 10 tags.
	 * @param session A Session instance.
	 * @return the Result of the operation
	 */
	public static Result addTags(String artist, String track, String tags, Session session) {
		return Caller.getInstance().call("track.addTags", session, "artist", artist, "track", track, "tags", tags);
	}

	/**
	 * Remove a user's tag from a track.
	 *
	 * @param artist The artist name in question
	 * @param track The track name in question
	 * @param tag A single user tag to remove from this track.
	 * @param session A Session instance.
	 * @return the Result of the operation
	 */
	public static Result removeTag(String artist, String track, String tag, Session session) {
		return Caller.getInstance().call("track.removeTag", session, "artist", artist, "track", track, "tag", tag);
	}

	/**
	 * Share a track twith one or more Last.fm users or other friends.
	 *
	 * @param artist An artist name.
	 * @param track A track name.
	 * @param message A message to send with the recommendation or <code>null</code>. If not supplied a default message will be used.
	 * @param recipient A comma delimited list of email addresses or Last.fm usernames. Maximum is 10.
	 * @param session A Session instance
	 * @return the Result of the operation
	 */
	public static Result share(String artist, String track, String message, String recipient, Session session) {
		Map<String, String> params = StringUtilities.map("artist", artist, "track", track, "recipient", recipient);
		MapUtilities.nullSafePut(params, "message", message);
		return Caller.getInstance().call("track.share", session, params);
	}

	/**
	 * Love a track for a user profile.
	 *
	 * @param artist An artist name
	 * @param track A track name
	 * @param session A Session instance
	 * @return the Result of the operation
	 */
	public static Result love(String artist, String track, Session session) {
		return Caller.getInstance().call("track.love", session, "artist", artist, "track", track);
	}

	/**
	 * UnLove a track for a user profile.
	 *
	 * @param artist An artist name
	 * @param track A track name
	 * @param session A Session instance
	 * @return the Result of the operation
	 */
	public static Result unlove(String artist, String track, Session session) {
		return Caller.getInstance().call("track.unlove", session, "artist", artist, "track", track);
	}

	/**
	 * Ban a track for a given user profile.
	 *
	 * @param artist An artist name
	 * @param track A track name
	 * @param session A Session instance
	 * @return the Result of the operation
	 */
	public static Result ban(String artist, String track, Session session) {
		return Caller.getInstance().call("track.ban", session, "artist", artist, "track", track);
	}

	/**
	 * UnBan a track for a given user profile.
	 *
	 * @param artist An artist name
	 * @param track A track name
	 * @param session A Session instance
	 * @return the Result of the operation
	 */
	public static Result unban(String artist, String track, Session session) {
		return Caller.getInstance().call("track.unban", session, "artist", artist, "track", track);
	}

	/**
	 * Get the similar tracks for this track on Last.fm, based on listening data.<br/>
	 * You have to provide either an artist and a track name <i>or</i> an mbid. Pass <code>null</code>
	 * for parameters you don't need.
	 *
	 * @param artist The artist name in question
	 * @param trackOrMbid The track name in question or the track's MBID
	 * @param apiKey A Last.fm API key.
	 * @return a list of similar <code>Track</code>s
	 */
	public static Collection<Track> getSimilar(String artist, String trackOrMbid, String apiKey) {
		return getSimilar(artist, trackOrMbid, apiKey, 100);
	}


	/**
	 * Get the similar tracks for this track on Last.fm, based on listening data.<br/>
	 * You have to provide either an artist and a track name <i>or</i> an mbid. Pass <code>null</code>
	 * for parameters you don't need.
	 *
	 * @param artist The artist name in question
	 * @param trackOrMbid The track name in question or the track's MBID
	 * @param apiKey A Last.fm API key.
	 * @param limit number of results to return
	 * @return a list of similar <code>Track</code>s
	 */
	public static Collection<Track> getSimilar(String artist, String trackOrMbid, String apiKey, int limit) {
		Map<String, String> params = new HashMap<String, String>();
		if (StringUtilities.isMbid(trackOrMbid)) {
			params.put("mbid", trackOrMbid);
		} else {
			params.put("artist", artist);
			params.put("track", trackOrMbid);
		}

		if (limit > 0){
			params.put("limit", "" + limit);
		}
		Result result = Caller.getInstance().call("track.getSimilar", apiKey, params);
		return ResponseBuilder.buildCollection(result, Track.class);
	}

	/**
	 * Get the tags applied by an individual user to an track on Last.fm.
	 *
	 * @param artist The artist name in question
	 * @param track The track name in question
	 * @param session A Session instance
	 * @return a list of tags
	 */
	public static Collection<String> getTags(String artist, String track, Session session) {
		Result result = Caller.getInstance().call("track.getTags", session, "artist", artist, "track", track);
		DomElement element = result.getContentElement();
		Collection<String> tags = new ArrayList<String>();
		for (DomElement domElement : element.getChildren("tag")) {
			tags.add(domElement.getChildText("name"));
		}
		return tags;
	}

	/**
	 * Get the metadata for a track on Last.fm using the artist/track name or a musicbrainz id.
	 *
	 * @param artist The artist name in question or <code>null</code> if an mbid is specified
	 * @param trackOrMbid The track name in question or the musicbrainz id for the track
	 * @param apiKey A Last.fm API key.
	 * @return Track information
	 */
	public static Track getInfo(String artist, String trackOrMbid, String apiKey) {
		return getInfo(artist, trackOrMbid, null, null, apiKey);
	}

	/**
	 * Get the metadata for a track on Last.fm using the artist/track name or a musicbrainz id.
	 *
	 * @param artist The artist name in question or <code>null</code> if an mbid is specified
	 * @param trackOrMbid The track name in question or the musicbrainz id for the track
	 * @param locale The language to fetch info in, or <code>null</code>
	 * @param username The username for the context of the request, or <code>null</code>. If supplied, the user's playcount for this track and whether they have loved the track is included in the response
	 * @param apiKey A Last.fm API key.
	 * @return Track information
	 */
	public static Track getInfo(String artist, String trackOrMbid, Locale locale, String username, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		if (StringUtilities.isMbid(trackOrMbid)) {
			params.put("mbid", trackOrMbid);
		} else {
			params.put("artist", artist);
			params.put("track", trackOrMbid);
		}
		if (locale != null && locale.getLanguage().length() != 0) {
			params.put("lang", locale.getLanguage());
		}
		MapUtilities.nullSafePut(params, "username", username);
		Result result = Caller.getInstance().call("track.getInfo", apiKey, params);
		if (!result.isSuccessful())
			return null;
		DomElement content = result.getContentElement();
		DomElement album = content.getChild("album");
		Track track = FACTORY.createItemFromElement(content);
		if (album != null) {
			String pos = album.getAttribute("position");
			if ((pos != null) && pos.length() != 0) {
				track.position = Integer.parseInt(pos);
			}
			track.album = album.getChildText("title");
			track.albumMbid = album.getChildText("mbid");
			ImageHolder.loadImages(track, album);
		}
		return track;
	}

	/**
	 * Get a list of Buy Links for a particular Track. It is required that you supply either the artist and track params or the mbid param.
	 *
	 * @param artist The artist name in question
	 * @param albumOrMbid Track name or MBID
	 * @param country A country name, as defined by the ISO 3166-1 country names standard
	 * @param apiKey A Last.fm API key
	 * @return a Collection of {@link BuyLink}s
	 */
	public static Collection<BuyLink> getBuylinks(String artist, String albumOrMbid, String country, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		if (StringUtilities.isMbid(albumOrMbid)) {
			params.put("mbid", albumOrMbid);
		} else {
			params.put("artist", artist);
			params.put("album", albumOrMbid);
		}
		params.put("country", country);
		Result result = Caller.getInstance().call("track.getBuylinks", apiKey, params);
		if (!result.isSuccessful())
			return Collections.emptyList();
		DomElement element = result.getContentElement();
		DomElement physicals = element.getChild("physicals");
		DomElement downloads = element.getChild("downloads");
		Collection<BuyLink> links = new ArrayList<BuyLink>();
		for (DomElement e : physicals.getChildren("affiliation")) {
			links.add(BuyLink.linkFromElement(BuyLink.StoreType.PHYSICAl, e));
		}
		for (DomElement e : downloads.getChildren("affiliation")) {
			links.add(BuyLink.linkFromElement(BuyLink.StoreType.DIGITAL, e));
		}
		return links;
	}

	/**
	 * Use the last.fm corrections data to check whether the supplied track has a correction to a canonical track. This method returns a new
	 * {@link Track} object containing the corrected data, or <code>null</code> if the supplied Artist/Track combination was not found.
	 *
	 * @param artist The artist name to correct
	 * @param track The track name to correct
	 * @param apiKey A Last.fm API key
	 * @return a new {@link Track}, or <code>null</code>
	 */
	public static Track getCorrection(String artist, String track, String apiKey) {
		Result result = Caller.getInstance().call("track.getCorrection", apiKey, "artist", artist, "track", track);
		if (!result.isSuccessful())
			return null;
		DomElement correctionElement = result.getContentElement().getChild("correction");
		if (correctionElement == null)
			return new Track(track, null, artist);
		DomElement trackElem = correctionElement.getChild("track");
		return FACTORY.createItemFromElement(trackElem);
	}

	/**
	 * Get shouts for a track.
	 *
	 * @param artist The artist name
	 * @param trackOrMbid The track name or a mausicbrainz id
	 * @param apiKey A Last.fm API key.
	 * @return a page of <code>Shout</code>s
	 */
	public static PaginatedResult<Shout> getShouts(String artist, String trackOrMbid, String apiKey) {
		return getShouts(artist, trackOrMbid, -1, -1, apiKey);
	}

	/**
	 * Get shouts for a track.
	 *
	 * @param artist The artist name
	 * @param trackOrMbid The track name or a mausicbrainz id
	 * @param page The page number to fetch
	 * @param apiKey A Last.fm API key.
	 * @return a page of <code>Shout</code>s
	 */
	public static PaginatedResult<Shout> getShouts(String artist, String trackOrMbid, int page, String apiKey) {
		return getShouts(artist, trackOrMbid, page, -1, apiKey);
	}

	/**
	 * Get shouts for a track.
	 *
	 * @param artist The artist name
	 * @param trackOrMbid The track name or a mausicbrainz id
	 * @param page The page number to fetch
	 * @param limit An integer used to limit the number of shouts returned per page or -1 for default
	 * @param apiKey A Last.fm API key.
	 * @return a page of <code>Shout</code>s
	 */
	public static PaginatedResult<Shout> getShouts(String artist, String trackOrMbid, int page, int limit, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		if (StringUtilities.isMbid(trackOrMbid)) {
			params.put("mbid", trackOrMbid);
		} else {
			params.put("artist", artist);
			params.put("track", trackOrMbid);
		}
		MapUtilities.nullSafePut(params, "limit", limit);
		MapUtilities.nullSafePut(params, "page", page);
		Result result = Caller.getInstance().call("track.getShouts", apiKey, params);
		return ResponseBuilder.buildPaginatedResult(result, Shout.class);
	}

	/**
	 * Converts a generic web services Result object into a more specific ScrobbleResult.
	 *
	 * @param result Web services Result.
	 * @param scrobbleResultType The type of scrobble result contained within the Result.
	 * @return A ScrobbleResult containing the original Result information plus extra fields specific to scrobble and now playing results.
	 */
	private static List<ScrobbleResult> convertToScrobbleResults(Result result, ScrobbleResultType scrobbleResultType) {
		List<ScrobbleResult> scrobbleResults = new ArrayList<ScrobbleResult>();
		if (!result.isSuccessful()) {
			// if result failed then we have no extra information
			ScrobbleResult scrobbleResult = new ScrobbleResult(result);
			scrobbleResults.add(scrobbleResult);
		} else {
			DomElement element = result.getContentElement();
			if (scrobbleResultType == ScrobbleResultType.NOW_PLAYING) {
				ScrobbleResult scrobbleResult = new ScrobbleResult(result);
				parseIntoScrobbleResult(element, scrobbleResult);
				scrobbleResults.add(scrobbleResult);
			} else if (scrobbleResultType == ScrobbleResultType.SINGLE_SCROBBLE) {
				ScrobbleResult scrobbleResult = new ScrobbleResult(result);
				parseIntoScrobbleResult(element.getChild("scrobble"), scrobbleResult);
				scrobbleResults.add(scrobbleResult);
			} else if (scrobbleResultType == ScrobbleResultType.MULTIPLE_SCROBBLES) {
				for (DomElement scrobbleElement : element.getChildren("scrobble")) {
					ScrobbleResult scrobbleResult = new ScrobbleResult(result);
					parseIntoScrobbleResult(scrobbleElement, scrobbleResult);
					scrobbleResults.add(scrobbleResult);
				}
			}
		}
		return scrobbleResults;
	}

	/**
	 * Parses a DomElement containing scrobble or now playing response data into the passed ScrobbleResult.
	 *
	 * @param scrobbleElement DomElement containing scrobble or now playing response data.
	 * @param scrobbleResult ScrobbleResult to add the response data to.
	 */
	private static void parseIntoScrobbleResult(DomElement scrobbleElement, ScrobbleResult scrobbleResult) {
		DomElement trackElement = scrobbleElement.getChild("track");
		scrobbleResult.setTrack(trackElement.getText());
		scrobbleResult.setArtistCorrected(StringUtilities.convertToBoolean(trackElement.getAttribute("corrected")));

		DomElement artistElement = scrobbleElement.getChild("artist");
		scrobbleResult.setArtist(artistElement.getText());
		scrobbleResult.setArtistCorrected(StringUtilities.convertToBoolean(artistElement.getAttribute("corrected")));

		DomElement albumElement = scrobbleElement.getChild("album");
		scrobbleResult.setAlbum(albumElement.getText());
		scrobbleResult.setAlbumCorrected(StringUtilities.convertToBoolean(albumElement.getAttribute("corrected")));

		DomElement albumArtistElement = scrobbleElement.getChild("albumArtist");
		scrobbleResult.setAlbumArtist(albumArtistElement.getText());
		scrobbleResult.setAlbumArtistCorrected(StringUtilities.convertToBoolean(albumArtistElement.getAttribute("corrected")));

		String timeString = scrobbleElement.getChildText("timestamp");
		if (timeString != null) {
			// will be non-null for scrobble results only
			scrobbleResult.setTimestamp(Integer.parseInt(timeString));
		}

		DomElement ignoredMessageElement = scrobbleElement.getChild("ignoredMessage");
		int ignoredMessageCode = Integer.parseInt(ignoredMessageElement.getAttribute("code"));
		if (ignoredMessageCode > 0) {
			scrobbleResult.setIgnored(true);
			scrobbleResult.setIgnoredMessageCode(IgnoredMessageCode.valueOfCode(ignoredMessageCode));
			scrobbleResult.setIgnoredMessage(ignoredMessageElement.getText());
		}
	}

	public static ScrobbleResult scrobble(ScrobbleData scrobbleData, Session session) {
		Map<String, String> params = new HashMap<String, String>();
		// required params
		params.put("artist", scrobbleData.getArtist());
		params.put("track", scrobbleData.getTrack());
		params.put("timestamp", String.valueOf(scrobbleData.getTimestamp()));
		// optional params
		MapUtilities.nullSafePut(params, "album", scrobbleData.getAlbum());
		MapUtilities.nullSafePut(params, "albumArtist", scrobbleData.getAlbumArtist());
		MapUtilities.nullSafePut(params, "duration", scrobbleData.getDuration());
		MapUtilities.nullSafePut(params, "mbid", scrobbleData.getMusicBrainzId());
		MapUtilities.nullSafePut(params, "trackNumber", scrobbleData.getTrackNumber());
		MapUtilities.nullSafePut(params, "streamId", scrobbleData.getStreamId());
		params.put("chosenByUser", StringUtilities.convertFromBoolean(scrobbleData.isChosenByUser()));

		Result result = Caller.getInstance().call("track.scrobble", session, params);
		return convertToScrobbleResults(result, ScrobbleResultType.SINGLE_SCROBBLE).get(0);
	}

	public static ScrobbleResult scrobble(String artistName, String trackName, int timestamp, Session session) {
		ScrobbleData scrobbleData = new ScrobbleData(artistName, trackName, timestamp);
		return scrobble(scrobbleData, session);
	}

	public static List<ScrobbleResult> scrobble(List<ScrobbleData> scrobbleData, Session session) {
		Map<String, String> params = new HashMap<String, String>();
		for (int i = 0; i < scrobbleData.size(); i++) {
			ScrobbleData scrobble = scrobbleData.get(i);
			// required params
			params.put("artist[" + i + "]", scrobble.getArtist());
			params.put("track[" + i + "]", scrobble.getTrack());
			params.put("timestamp[" + i + "]", String.valueOf(scrobble.getTimestamp()));
			// optional params
			MapUtilities.nullSafePut(params, "album[" + i + "]", scrobble.getAlbum());
			MapUtilities.nullSafePut(params, "albumArtist[" + i + "]", scrobble.getAlbumArtist());
			MapUtilities.nullSafePut(params, "duration[" + i + "]", scrobble.getDuration());
			MapUtilities.nullSafePut(params, "mbid[" + i + "]", scrobble.getMusicBrainzId());
			MapUtilities.nullSafePut(params, "trackNumber[" + i + "]", scrobble.getTrackNumber());
			MapUtilities.nullSafePut(params, "streamId[" + i + "]", scrobble.getStreamId());
			params.put("chosenByUser[" + i + "]", StringUtilities.convertFromBoolean(scrobble.isChosenByUser()));
		}

		Result result = Caller.getInstance().call("track.scrobble", session, params);
		return convertToScrobbleResults(result, ScrobbleResultType.MULTIPLE_SCROBBLES);
	}

	public static ScrobbleResult updateNowPlaying(ScrobbleData scrobbleData, Session session) {
		Map<String, String> params = new HashMap<String, String>();
		// required params
		params.put("artist", scrobbleData.getArtist());
		params.put("track", scrobbleData.getTrack());
		// optional params
		MapUtilities.nullSafePut(params, "album", scrobbleData.getAlbum());
		MapUtilities.nullSafePut(params, "albumArtist", scrobbleData.getAlbumArtist());
		MapUtilities.nullSafePut(params, "duration", scrobbleData.getDuration());
		MapUtilities.nullSafePut(params, "mbid", scrobbleData.getMusicBrainzId());
		MapUtilities.nullSafePut(params, "trackNumber", scrobbleData.getTrackNumber());
		MapUtilities.nullSafePut(params, "streamId", scrobbleData.getStreamId());
		Result result = Caller.getInstance().call("track.updateNowPlaying", session, params);
		return convertToScrobbleResults(result, ScrobbleResultType.NOW_PLAYING).get(0);
	}

	public static ScrobbleResult updateNowPlaying(String artistName, String trackName, Session session) {
		ScrobbleData scrobbleData = new ScrobbleData();
		scrobbleData.setArtist(artistName);
		scrobbleData.setTrack(trackName);
		return updateNowPlaying(scrobbleData, session);
	}

	@Override
	public String toString() {
		return "Track[name=" + name + ",artist=" + artist + ", album=" + album + ", position=" + position + ", duration=" + duration
				+ ", location=" + location + ", nowPlaying=" + nowPlaying + ", fullTrackAvailable=" + fullTrackAvailable + ", playedWhen="
				+ playedWhen + ", artistMbId=" + artistMbid + ", albumMbId" + albumMbid + "]";
	}

	private static class TrackFactory implements ItemFactory<Track> {
		public Track createItemFromElement(DomElement element) {
			Track track = new Track(null, null, null);
			MusicEntry.loadStandardInfo(track, element);
			final String nowPlayingAttr = element.getAttribute("nowplaying");
			if (nowPlayingAttr != null)
				track.nowPlaying = Boolean.valueOf(nowPlayingAttr);
			if (element.hasChild("duration")) {
				String duration = element.getChildText("duration");
				if(duration.length() != 0) {
					int durationLength = Integer.parseInt(duration);
					// So it seems last.fm couldn't decide which format to send the duration in.
					// It's supplied in milliseconds for Playlist.fetch and Track.getInfo but Artist.getTopTracks returns (much saner) seconds
					// so we're doing a little sanity check for the duration to be over or under 10'000 and decide what to do
					track.duration = durationLength > 10000 ? durationLength / 1000 : durationLength;
				}
			}
			DomElement album = element.getChild("album");
			if (album != null) {
				track.album = album.getText();
				track.albumMbid = album.getAttribute("mbid");
			}
			DomElement artist = element.getChild("artist");
			if (artist.getChild("name") != null) {
				track.artist = artist.getChildText("name");
				track.artistMbid = artist.getChildText("mbid");
			} else {
				track.artist = artist.getText();
				track.artistMbid = artist.getAttribute("mbid");
			}
			DomElement date = element.getChild("date");
			if (date != null) {
				String uts = date.getAttribute("uts");
				long utsTime = Long.parseLong(uts);
				track.playedWhen = new Date(utsTime * 1000);
			}
			DomElement stream = element.getChild("streamable");
			if (stream != null) {
				String s = stream.getAttribute("fulltrack");
				track.fullTrackAvailable = s != null && Integer.parseInt(s) == 1;
			}
			return track;
		}
	}
}
