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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import de.umass.util.MapUtilities;
import de.umass.util.StringUtilities;
import de.umass.xml.DomElement;

/**
 * Wrapper class for Album related API calls and Album Bean.
 *
 * @author Janni Kovacs
 */
public class Album extends MusicEntry {

	static final ItemFactory<Album> FACTORY = new AlbumFactory();

	private static final DateFormat RELEASE_DATE_FORMAT = new SimpleDateFormat("d MMM yyyy, HH:mm", Locale.ENGLISH);
	private static final DateFormat RELEASE_DATE_FORMAT_2 = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss Z",
			Locale.ENGLISH); /* only used in User.getNewReleases() */
	
	private String artist;
	private Date releaseDate;
	private Collection<Track> tracks;

	private Album(String name, String url, String artist) {
		super(name, url);
		this.artist = artist;
	}

	private Album(String name, String url, String mbid, int playcount, int listeners, boolean streamable,
					String artist) {
		super(name, url, mbid, playcount, listeners, streamable);
		this.artist = artist;
	}

	public String getArtist() {
		return artist;
	}

	public Date getReleaseDate() {
		return releaseDate;
	}

	/**
	 * Returns the list of {@link Track}s on this album. This information is only available in
	 * {@link Album#getInfo(String, String, String)} responses.
	 * 
	 * @return the list of tracks
	 * @see Album#getInfo(String, String, String) 
	 */
	public Collection<Track> getTracks() {
		return tracks;
	}

	/**
	 * Get the metadata for an album on Last.fm using the album name or a musicbrainz id.
	 * See playlist.fetch on how to get the album playlist.
	 *
	 * @param artist Artist's name
	 * @param albumOrMbid Album name or MBID
	 * @param apiKey The API key
	 * @return Album metadata
	 */
	public static Album getInfo(String artist, String albumOrMbid, String apiKey) {
		return getInfo(artist, albumOrMbid, null, apiKey);
	}

	/**
	 * Get the metadata for an album on Last.fm using the album name or a musicbrainz id.
	 * See playlist.fetch on how to get the album playlist.
	 *
	 * @param artist Artist's name
	 * @param albumOrMbid Album name or MBID
	 * @param username The username for the context of the request. If supplied, the user's playcount for this album is included in the response.
	 * @param apiKey The API key
	 * @return Album metadata
	 */
	public static Album getInfo(String artist, String albumOrMbid, String username, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		if (StringUtilities.isMbid(albumOrMbid)) {
			params.put("mbid", albumOrMbid);
		} else {
			params.put("artist", artist);
			params.put("album", albumOrMbid);
		}
		MapUtilities.nullSafePut(params, "username", username);
		Result result = Caller.getInstance().call("album.getInfo", apiKey, params);
		return ResponseBuilder.buildItem(result, Album.class);
	}

	/**
	 * Tag an album using a list of user supplied tags.<br/>
	 *
	 * @param artist The artist name in question
	 * @param album The album name in question
	 * @param tags A comma delimited list of user supplied tags to apply to this album. Accepts a maximum of 10 tags.
	 * @param session The Session instance
	 * @return the Result of the operation
	 * @see Authenticator
	 */
	public static Result addTags(String artist, String album, String tags, Session session) {
		return Caller.getInstance().call("album.addTags", session, "artist", artist, "album", album, "tags", tags);
	}

	/**
	 * Remove a user's tag from an album.
	 *
	 * @param artist The artist name in question
	 * @param album The album name in question
	 * @param tag A single user tag to remove from this album.
	 * @param session The Session instance
	 * @return the Result of the operation
	 * @see Authenticator
	 */
	public static Result removeTag(String artist, String album, String tag, Session session) {
		return Caller.getInstance().call("album.removeTag", session, "artist", artist, "album", album, "tag", tag);
	}

	/**
	 * Get the tags applied by an individual user to an album on Last.fm.
	 *
	 * @param artist The artist name in question
	 * @param album The album name in question
	 * @param session A Session instance
	 * @return a list of tags
	 */
	public static Collection<String> getTags(String artist, String album, Session session) {
		Result result = Caller.getInstance().call("album.getTags", session, "artist", artist, "album", album);
		if (!result.isSuccessful())
			return Collections.emptyList();
		DomElement element = result.getContentElement();
		Collection<String> tags = new ArrayList<String>();
		for (DomElement domElement : element.getChildren("tag")) {
			tags.add(domElement.getChildText("name"));
		}
		return tags;
	}

	/**
	 * Search for an album by name. Returns album matches sorted by relevance.
	 *
	 * @param album The album name in question.
	 * @param apiKey A Last.fm API key.
	 * @return a Collection of matches
	 */
	public static Collection<Album> search(String album, String apiKey) {
		Result result = Caller.getInstance().call("album.search", apiKey, "album", album);
		DomElement matches = result.getContentElement().getChild("albummatches");
		Collection<DomElement> children = matches.getChildren("album");
		Collection<Album> albums = new ArrayList<Album>(children.size());
		for (DomElement element : children) {
			albums.add(FACTORY.createItemFromElement(element));
		}
		return albums;
	}

	/**
	 * Get a list of Buy Links for a particular Album. It is required that you supply either the artist and track params or the mbid param.
	 *
	 * @param artist The artist name in question
	 * @param albumOrMbid Album name or MBID
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
		Result result = Caller.getInstance().call("album.getBuylinks", apiKey, params);
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
	 * Get the top tags for an album on Last.fm, ordered by popularity. You either have to specify an album and artist name or
	 * an mbid. If you specify an mbid you may pass <code>null</code> for the first parameter.
	 *
	 * @param artist The artist name
	 * @param albumOrMbid Album name or MBID
	 * @param apiKey A Last.fm API key
	 * @return list of top tags
	 */
	public static Collection<Tag> getTopTags(String artist, String albumOrMbid, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		if (StringUtilities.isMbid(albumOrMbid)) {
			params.put("mbid", albumOrMbid);
		} else {
			params.put("artist", artist);
			params.put("album", albumOrMbid);
		}
		Result result = Caller.getInstance().call("album.getTopTags", apiKey, params);
		return ResponseBuilder.buildCollection(result, Tag.class);
	}

	/**
	 * Get shouts for an album.
	 *
	 * @param artist The artist name
	 * @param albumOrMbid The album name or a mausicbrainz id
	 * @param apiKey A Last.fm API key.
	 * @return a page of <code>Shout</code>s
	 */
	public static PaginatedResult<Shout> getShouts(String artist, String albumOrMbid, String apiKey) {
		return getShouts(artist, albumOrMbid, -1, -1, apiKey);
	}

	/**
	 * Get shouts for an album.
	 *
	 * @param artist The artist name
	 * @param albumOrMbid The album name or a mausicbrainz id
	 * @param page The page number to fetch
	 * @param apiKey A Last.fm API key.
	 * @return a page of <code>Shout</code>s
	 */
	public static PaginatedResult<Shout> getShouts(String artist, String albumOrMbid, int page, String apiKey) {
		return getShouts(artist, albumOrMbid, page, -1, apiKey);
	}

	/**
	 * Get shouts for an album.
	 *
	 * @param artist The artist name
	 * @param albumOrMbid The album name or a mausicbrainz id
	 * @param page The page number to fetch
	 * @param limit An integer used to limit the number of shouts returned per page or -1 for default
	 * @param apiKey A Last.fm API key.
	 * @return a page of <code>Shout</code>s
	 */
	public static PaginatedResult<Shout> getShouts(String artist, String albumOrMbid, int page, int limit, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		if (StringUtilities.isMbid(albumOrMbid)) {
			params.put("mbid", albumOrMbid);
		} else {
			params.put("artist", artist);
			params.put("album", albumOrMbid);
		}
		MapUtilities.nullSafePut(params, "limit", limit);
		MapUtilities.nullSafePut(params, "page", page);
		Result result = Caller.getInstance().call("album.getShouts", apiKey, params);
		return ResponseBuilder.buildPaginatedResult(result, Shout.class);
	}

	private static class AlbumFactory implements ItemFactory<Album> {
		public Album createItemFromElement(DomElement element) {
			Album album = new Album(null, null, null);
			MusicEntry.loadStandardInfo(album, element);
			if (element.hasChild("artist")) {
				album.artist = element.getChild("artist").getChildText("name");
				if (album.artist == null)
					album.artist = element.getChildText("artist");
			}
			if (element.hasChild("tracks")) {
				album.tracks = ResponseBuilder.buildCollection(element.getChild("tracks"), Track.class);
			}
			if (element.hasChild("releasedate")) {
				try {
					album.releaseDate = RELEASE_DATE_FORMAT.parse(element.getChildText("releasedate"));
				} catch (ParseException e) {
					// uh oh
				}
			}
			String releaseDateAttribute = element.getAttribute("releasedate");
			if (releaseDateAttribute != null) {
				try {
					album.releaseDate = RELEASE_DATE_FORMAT_2.parse(releaseDateAttribute);
				} catch (ParseException e) {
					// uh oh
				}
			}
			return album;
		}
	}
}
