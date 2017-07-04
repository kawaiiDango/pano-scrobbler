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

import de.umass.xml.DomElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Bean for music playlists. Contains the {@link #fetch(String, String) fetch} method and various <code>fetchXXX</code>
 * methods to retrieve playlists from the server. Playlists are identified by lastfm:// playlist urls. Valid urls
 * include:
 * <ul>
 * <li><b>Album Playlists:</b> lastfm://playlist/album/{@literal <album_id>}</li>
 * <li><b>User Playlists:</b> lastfm://playlist/{@literal <playlist_id>}</li>
 * <li><b>Tag Playlists:</b> lastfm://playlist/tag/{@literal <tag_name>}/freetracks</li>
 * </ul>
 * See <a href="http://www.last.fm/api/playlists">http://www.last.fm/api/playlists</a> for more information about playlists.
 *
 * @author Janni Kovacs
 */
public class Playlist {

	static final ItemFactory<Playlist> FACTORY = new PlaylistFactory();

	private int id;
	private String title;
	private String annotation;
	private int size;
	private String creator;

	private List<Track> tracks = new ArrayList<Track>();

	private Playlist() {
	}

	public String getCreator() {
		return creator;
	}

	public int getId() {
		return id;
	}

	public int getSize() {
		return size;
	}

	public String getTitle() {
		return title;
	}

	public String getAnnotation() {
		return annotation;
	}

	public List<Track> getTracks() {
		return tracks;
	}

	/**
	 * Fetches an album playlist, which contains the tracks of the specified album.
	 *
	 * @param albumId The album id as returned in {@link Album#getInfo(String, String, String) Album.getInfo}.
	 * @param apiKey A Last.fm API key.
	 * @return a playlist
	 */
	public static Playlist fetchAlbumPlaylist(String albumId, String apiKey) {
		return fetch("lastfm://playlist/album/" + albumId, apiKey);
	}

	/**
	 * Fetches a user-created playlist.
	 *
	 * @param playlistId A playlist id.
	 * @param apiKey A Last.fm API key.
	 * @return a playlist
	 */
	public static Playlist fetchUserPlaylist(int playlistId, String apiKey) {
		return fetch("lastfm://playlist/" + playlistId, apiKey);
	}

	/**
	 * Fetches a playlist of freetracks for a given tag name.
	 *
	 * @param tag A tag name.
	 * @param apiKey A Last.fm API key.
	 * @return a playlist
	 */
	public static Playlist fetchTagPlaylist(String tag, String apiKey) {
		return fetch("lastfm://playlist/tag/" + tag + "/freetracks", apiKey);
	}

	/**
	 * Fetches a playlist using a lastfm playlist url. See the class description for a list of valid
	 * playlist urls.
	 *
	 * @param playlistUrl A valid playlist url.
	 * @param apiKey A Last.fm API key.
	 * @return a playlist
	 */
	public static Playlist fetch(String playlistUrl, String apiKey) {
		Result result = Caller.getInstance().call("playlist.fetch", apiKey, "playlistURL", playlistUrl);
		return ResponseBuilder.buildItem(result, Playlist.class);
	}

	/**
	 * Add a track to a Last.fm user's playlist.
	 *
	 * @param playlistId The ID of the playlist - this is available in user.getPlaylists
	 * @param artist The artist name that corresponds to the track to be added.
	 * @param track The track name to add to the playlist.
	 * @param session A Session instance.
	 * @return the result of the operation
	 */
	public static Result addTrack(int playlistId, String artist, String track, Session session) {
		return Caller.getInstance()
				.call("playlist.addTrack", session, "playlistID", String.valueOf(playlistId), "artist", artist, "track",
						track);
	}

	/**
	 * Creates a Last.fm playlist.
	 *
	 * @param title A title for the playlist
	 * @param description A description for the playlist
	 * @param session A Session instance
	 * @return the result of the operation
	 */
	public static Playlist create(String title, String description, Session session) {
		Result result = Caller.getInstance().call("playlist.create", session, "title", title, "description", description);
		if (!result.isSuccessful())
			return null;
		return ResponseBuilder.buildItem(result.getContentElement().getChild("playlist"), Playlist.class);
	}

	private static class PlaylistFactory implements ItemFactory<Playlist> {
		public Playlist createItemFromElement(DomElement element) {
			Playlist playlist = new Playlist();

			if (element.hasChild("id"))
				playlist.id = Integer.parseInt(element.getChildText("id"));

			playlist.title = element.getChildText("title");

			if (element.hasChild("size"))
				playlist.size = Integer.parseInt(element.getChildText("size"));

			playlist.creator = element.getChildText("creator");
			playlist.annotation = element.getChildText("annotation");

			DomElement trackList = element.getChild("trackList");
			if (trackList != null) {
				for (DomElement te : trackList.getChildren("track")) {
					Track t = new Track(te.getChildText("title"), te.getChildText("identifier"), te.getChildText("creator"));
					t.album = te.getChildText("album");
					t.duration = Integer.parseInt(te.getChildText("duration")) / 1000;
					t.imageUrls.put(ImageSize.LARGE, te.getChildText("image"));
					t.imageUrls.put(ImageSize.ORIGINAL, te.getChildText("image"));
					t.location = te.getChildText("location");
					for (DomElement ext : te.getChildren("extension")) {
						if ("http://www.last.fm".equals(ext.getAttribute("application"))) {
							for (DomElement child : ext.getChildren()) {
								t.lastFmExtensionInfos.put(child.getTagName(), child.getText());
							}
						}
					}
					playlist.tracks.add(t);
				}

				if (playlist.size == 0)
					playlist.size = playlist.tracks.size();
			}

			return playlist;
		}
	}
}
