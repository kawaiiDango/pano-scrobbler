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
import java.util.HashMap;
import java.util.Map;

/**
 * Contains bindings for all methods in the "library" namespace.
 *
 * @author Martin Chorley
 * @author Janni Kovacs
 */
public class Library {

	private Library() {
	}

	/**
	 * Retrieves a paginated list of all the artists in a user's library.
	 *
	 * @param user The user whose library you want to fetch.
	 * @param apiKey A Last.fm API key.
	 * @return a {@link PaginatedResult} of the artists
	 */
	public static PaginatedResult<Artist> getArtists(String user, String apiKey) {
		return getArtists(user, 1, 0, apiKey);
	}

	/**
	 * Retrieves a paginated list of all the artists in a user's library.
	 *
	 * @param user The user whose library you want to fetch.
	 * @param page The page number you wish to scan to.
	 * @param apiKey A Last.fm API key.
	 * @return a {@link PaginatedResult} of the artists
	 */
	public static PaginatedResult<Artist> getArtists(String user, int page, String apiKey) {
		return getArtists(user, page, 0, apiKey);
	}

	/**
	 * Retrieves a paginated list of all the artists in a user's library.
	 *
	 * @param user The user whose library you want to fetch.
	 * @param page The page number you wish to scan to.
	 * @param limit Limit the number of artists returned (maximum/default is 50).
	 * @param apiKey A Last.fm API key.
	 * @return a {@link PaginatedResult} of the artists
	 */
	public static PaginatedResult<Artist> getArtists(String user, int page, int limit, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("user", user);
		params.put("page", String.valueOf(page));
		if (limit > 0)
			params.put("limit", String.valueOf(limit));
		Result result = Caller.getInstance().call("library.getArtists", apiKey, params);
		return ResponseBuilder.buildPaginatedResult(result, Artist.class);
	}

	/**
	 * Retrieves all artists in a user's library. Pay attention if you use this method as it may produce
	 * a lot of network traffic and therefore may consume a long time.
	 *
	 * @param user The user whose library you want to fetch.
	 * @param apiKey A Last.fm API key.
	 * @return all artists in a user's library
	 */
	public static Collection<Artist> getAllArtists(String user, String apiKey) {
		Collection<Artist> artists = null;
		int page = 1, total;
		do {
			PaginatedResult<Artist> result = getArtists(user, page, apiKey);
			total = result.getTotalPages();
			Collection<Artist> pageResults = result.getPageResults();
			if (artists == null) {
				// artists is initialized here to initialize it with the right size and avoid array copying later on
				artists = new ArrayList<Artist>(total * pageResults.size());
			}
			artists.addAll(pageResults);
			page++;
		} while (page <= total);
		return artists;
	}


	/**
	 * Retrieves a paginated list of all the albums in a user's library.
	 *
	 * @param user The user whose library you want to fetch.
	 * @param apiKey A Last.fm API key.
	 * @return a {@link PaginatedResult} of the albums
	 */
	public static PaginatedResult<Album> getAlbums(String user, String apiKey) {
		return getAlbums(user, 1, 0, apiKey);
	}

	/**
	 * Retrieves a paginated list of all the albums in a user's library.
	 *
	 * @param user The user whose library you want to fetch.
	 * @param page The page number you wish to scan to.
	 * @param apiKey A Last.fm API key.
	 * @return a {@link PaginatedResult} of the albums
	 */
	public static PaginatedResult<Album> getAlbums(String user, int page, String apiKey) {
		return getAlbums(user, page, 0, apiKey);
	}

	/**
	 * Retrieves a paginated list of all the albums in a user's library.
	 *
	 * @param user The user whose library you want to fetch.
	 * @param page The page number you wish to scan to.
	 * @param limit Limit the number of albums returned (maximum/default is 50).
	 * @param apiKey A Last.fm API key.
	 * @return a {@link PaginatedResult} of the albums
	 */
	public static PaginatedResult<Album> getAlbums(String user, int page, int limit, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("user", user);
		params.put("page", String.valueOf(page));
		if (limit > 0)
			params.put("limit", String.valueOf(limit));
		Result result = Caller.getInstance().call("library.getAlbums", apiKey, params);
		return ResponseBuilder.buildPaginatedResult(result, Album.class);
	}

	/**
	 * Retrieves all albums in a user's library. Pay attention if you use this method as it may produce
	 * a lot of network traffic and therefore may consume a long time.
	 *
	 * @param user The user whose library you want to fetch.
	 * @param apiKey A Last.fm API key.
	 * @return all albums in a user's library
	 */
	public static Collection<Album> getAllAlbums(String user, String apiKey) {
		Collection<Album> albums = null;
		int page = 1, total;
		do {
			PaginatedResult<Album> result = getAlbums(user, page, apiKey);
			total = result.getTotalPages();
			Collection<Album> pageResults = result.getPageResults();
			if (albums == null) {
				// albums is initialized here to initialize it with the right size and avoid array copying later on
				albums = new ArrayList<Album>(total * pageResults.size());
			}
			albums.addAll(pageResults);
			page++;
		} while (page <= total);
		return albums;
	}


	/**
	 * Retrieves a paginated list of all the tracks in a user's library.
	 *
	 * @param user The user whose library you want to fetch.
	 * @param apiKey A Last.fm API key.
	 * @return a {@link PaginatedResult} of the tracks
	 */
	public static PaginatedResult<Track> getTracks(String user, String apiKey) {
		return getTracks(user, 1, 0, apiKey);
	}

	/**
	 * Retrieves a paginated list of all the tracks in a user's library.
	 *
	 * @param user The user whose library you want to fetch.
	 * @param page The page number you wish to scan to.
	 * @param apiKey A Last.fm API key.
	 * @return a {@link PaginatedResult} of the tracks
	 */
	public static PaginatedResult<Track> getTracks(String user, int page, String apiKey) {
		return getTracks(user, page, 0, apiKey);
	}

	/**
	 * Retrieves a paginated list of all the tracks in a user's library.
	 *
	 * @param user The user whose library you want to fetch.
	 * @param page The page number you wish to scan to.
	 * @param limit Limit the number of albums returned (maximum/default is 50).
	 * @param apiKey A Last.fm API key.
	 * @return a {@link PaginatedResult} of the tracks
	 */
	public static PaginatedResult<Track> getTracks(String user, int page, int limit, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("user", user);
		params.put("page", String.valueOf(page));
		if (limit > 0)
			params.put("limit", String.valueOf(limit));
		Result result = Caller.getInstance().call("library.getTracks", apiKey, params);
		return ResponseBuilder.buildPaginatedResult(result, Track.class);
	}

	/**
	 * Retrieves all tracks in a user's library. Pay attention if you use this method as it may produce
	 * a lot of network traffic and therefore may consume a long time.
	 *
	 * @param user The user whose library you want to fetch.
	 * @param apiKey A Last.fm API key.
	 * @return all tracks in a user's library
	 */
	public static Collection<Track> getAllTracks(String user, String apiKey) {
		Collection<Track> tracks = null;
		int page = 1, total;
		do {
			PaginatedResult<Track> result = getTracks(user, page, apiKey);
			total = result.getTotalPages();
			Collection<Track> pageResults = result.getPageResults();
			if (tracks == null) {
				// tracks is initialized here to initialize it with the right size and avoid array copying later on
				tracks = new ArrayList<Track>(total * pageResults.size());
			}
			tracks.addAll(pageResults);
			page++;
		} while (page <= total);
		return tracks;
	}

	/**
	 * Add an artist to a user's Last.fm library
	 *
	 * @param artist The artist name you wish to add
	 * @param session A Session instance
	 * @return the result of the operation
	 */
	public static Result addArtist(String artist, Session session) {
		return Caller.getInstance().call("library.addArtist", session, "artist", artist);
	}

	/**
	 * Add an album to a user's Last.fm library
	 *
	 * @param artist The artist that composed the track
	 * @param album The album name you wish to add
	 * @param session A Session instance
	 * @return the result of the operation
	 */
	public static Result addAlbum(String artist, String album, Session session) {
		return Caller.getInstance().call("library.addAlbum", session, "artist", artist, "album", album);
	}

	/**
	 * Add a track to a user's Last.fm library
	 *
	 * @param artist The artist that composed the track
	 * @param track The track name you wish to add
	 * @param session A Session instance
	 * @return the result of the operation
	 */
	public static Result addTrack(String artist, String track, Session session) {
		return Caller.getInstance().call("library.addTrack", session, "artist", artist, "track", track);
	}

	/**
	 * Remove an artist from a user's Last.fm library
	 *
	 * @param artist The artist name you wish to remove
	 * @param session A Session instance
	 * @return the result of the operation
	 */
	public static Result removeArtist(String artist, Session session) {
		return Caller.getInstance().call("library.removeArtist", session, "artist", artist);
	}

	/**
	 * Remove an album from a user's Last.fm library
	 *
	 * @param artist The artist that composed the album
	 * @param album The name of the album you wish to remove
	 * @param session A Session instance
	 * @return the result of the operation
	 */
	public static Result removeAlbum(String artist, String album, Session session) {
		return Caller.getInstance().call("library.removeAlbum", session, "artist", artist, "album", album);
	}

	/**
	 * Remove a track from a user's Last.fm library
	 *
	 * @param artist The artist that composed the track
	 * @param track The name of the track that you wish to remove
	 * @param session A Session instance
	 * @return the result of the operation
	 */
	public static Result removeTrack(String artist, String track, Session session) {
		return Caller.getInstance().call("library.removeTrack", session, "artist", artist, "track", track);
	}

	/**
	 * Remove a scrobble from a user's Last.fm library
	 *
	 * @param artist The artist that composed the track
	 * @param track The name of the track
	 * @param timestamp The unix timestamp of the scrobble that you wish to remove
	 * @param session A Session instance
	 * @return the result of the operation
	 */
	public static Result removeScrobble(String artist, String track, long timestamp, Session session) {
		return Caller.getInstance().call("library.removeScrobble", session, "artist", artist, "track", track, "timestamp",
				String.valueOf(timestamp));
	}
}
