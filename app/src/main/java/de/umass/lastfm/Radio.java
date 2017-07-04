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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import de.umass.util.MapUtilities;
import de.umass.xml.DomElement;

/**
 * Provides access to the Last.fm radio streaming service.<br/>
 * Note that you have to be a subscriber (or have a special API key) to use this API.
 * Official documentation can be found here: <a href="http://www.last.fm/api/radio">http://www.last.fm/api/radio</a>
 *
 * @author Janni Kovacs
 */
public class Radio {

	private String type;
	private String stationName;
	private String stationUrl;
	private boolean supportsDiscovery;

	private Session session;
	private int expiry = -1;

	private Radio(Session session) {
		this.session = session;
	}

	public String getType() {
		return type;
	}

	public String getStationName() {
		return stationName;
	}

	public String getStationUrl() {
		return stationUrl;
	}

	public boolean supportsDiscovery() {
		return supportsDiscovery;
	}

	/**
	 * Returns the playlist expiration value for the last playlist fetchet, or -1 if no playlist has been fetched yet.
	 *
	 * @return playlist expiration in seconds
	 */
	public int playlistExpiresIn() {
		return expiry;
	}

	/**
	 * Resolve the name of a resource into a station depending on which resource it is most likely to represent
	 *
	 * @param name The tag or artist to resolve
	 * @param apiKey A Last.fm API key.
	 * @return A {@link RadioStation} or <code>null</code>
	 */
	public static RadioStation search(String name, String apiKey) {
		Result result = Caller.getInstance().call("radio.search", apiKey, "name", name);
		if(!result.isSuccessful())
			return null;

		DomElement stationElement = result.getContentElement().getChild("station");
		if(stationElement == null)
			return null;

		return new RadioStation(stationElement.getChildText("url"), stationElement.getChildText("name"));
	}

	/**
	 * Tune in to a Last.fm radio station.
	 *
	 * @param station An instance of {@link RadioStation}
	 * @param session A Session instance
	 * @return a Radio instance
	 */
	public static Radio tune(RadioStation station, Session session) {
		return tune(station, Locale.getDefault(), session);
	}

	/**
	 * Tune in to a Last.fm radio station.
	 *
	 * @param station An instance of {@link RadioStation}
	 * @param locale The language you want the radio's name in
	 * @param session A Session instance
	 * @return a Radio instance
	 */
	public static Radio tune(RadioStation station, Locale locale, Session session) {
		return tune(station.getUrl(), locale, session);
	}

	/**
	 * Tune in to a Last.fm radio station.
	 *
	 * @param station A lastfm radio URL
	 * @param locale The language you want the radio's name in
	 * @param session A Session instance
	 * @return a Radio instance
	 */
	public static Radio tune(String station, Locale locale, Session session) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("station", station);
		if (locale != null && locale.getLanguage().length() != 0) {
			params.put("lang", locale.getLanguage());
		}

		Result result = Caller.getInstance().call("radio.tune", session, params);
		if (!result.isSuccessful())
			return null;

		DomElement root = result.getContentElement();
		Radio radio = new Radio(session);
		radio.type = root.getChildText("type");
		radio.stationName = root.getChildText("name");
		radio.stationUrl = root.getChildText("url");
		radio.supportsDiscovery = "1".equals(root.getChildText("type"));
		return radio;
	}

	/**
	 * Fetches a new radio playlist or <code>null</code> if an error occured, such as when the user is not allowed to stream radio
	 * (no subscriber).
	 *
	 * @return a new {@link Playlist} or <code>null</code>
	 */
	public Playlist getPlaylist() {
		return getPlaylist(false, false);
	}

	/**
	 * Fetches a new radio playlist.
	 *
	 * @param discovery Whether to request last.fm content with discovery mode switched on
	 * @param rtp Whether the user is scrobbling or not during this radio session (helps content generation)
	 * @return a new Playlist
	 */
	public Playlist getPlaylist(boolean discovery, boolean rtp) {
		return getPlaylist(discovery, rtp, false, -1, -1);
	}

	/**
	 * Fetches a new radio playlist.
	 *
	 * @param discovery Whether to request last.fm content with discovery mode switched on
	 * @param rtp Whether the user is scrobbling or not during this radio session (helps content generation)
	 * @param buyLinks Whether the response should contain links for purchase/download, if available
	 * @param speedMultiplier The rate at which to provide the stream (supported multipliers are 1.0 and 2.0)
	 * @param bitrate What bitrate to stream content at, in kbps (supported bitrates are 64 and 128)
	 * @return a new Playlist
	 */
	public Playlist getPlaylist(boolean discovery, boolean rtp, boolean buyLinks, double speedMultiplier, int bitrate) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("discovery", String.valueOf(discovery));
		params.put("rtp", String.valueOf(rtp));
		params.put("buylinks", String.valueOf(buyLinks));
		MapUtilities.nullSafePut(params, "speed_multiplier", speedMultiplier);
		MapUtilities.nullSafePut(params, "bitrate", bitrate);
		Result result = Caller.getInstance().call("radio.getPlaylist", session, params);

		if (!result.isSuccessful())
			return null;

		DomElement root = result.getContentElement();
		for (DomElement e : root.getChildren("link")) {
			if ("http://www.last.fm/expiry".equals(e.getAttribute("rel"))) {
				this.expiry = Integer.parseInt(e.getText());
				break;
			}
		}
		return ResponseBuilder.buildItem(root, Playlist.class);
	}

	public static class RadioStation {
		private String name;
		private String url;

		public RadioStation(String url) {
			this(url, null);
		}

		public RadioStation(String url, String name) {
			this.url = url;
			this.name = name;
		}

		public String getUrl() {
			return url;
		}

		public String getName() {
			return name;
		}

		public static RadioStation similarArtists(String artist) {
			return new RadioStation("lastfm://artist/" + artist + "/similarartists");
		}

		public static RadioStation artistFans(String artist) {
			return new RadioStation("lastfm://artist/" + artist + "/fans");
		}

		public static RadioStation library(String user) {
			return new RadioStation("lastfm://user/" + user + "/library");
		}

		public static RadioStation neighbours(String user) {
			return new RadioStation("lastfm://user/" + user + "/neighbours");
		}

		/**
		 * @deprecated This station has been deprected as of nov. 2010, see <a href="http://www.last.fm/stationchanges2010">here</a>
		 */
		public static RadioStation lovedTracks(String user) {
			return new RadioStation("lastfm://user/" + user + "/loved");
		}

		public static RadioStation recommended(String user) {
			return new RadioStation("lastfm://user/" + user + "/recommended");
		}

		public static RadioStation tagged(String tag) {
			return new RadioStation("lastfm://globaltags/" + tag);
		}

		/**
		 * @deprecated This station has been deprected as of nov. 2010, see <a href="http://www.last.fm/stationchanges2010">here</a>
		 */
		public static RadioStation playlist(String playlistId) {
			return new RadioStation("lastfm://playlist/" + playlistId);
		}

		/**
		 * @deprecated This station has been deprected as of nov. 2010, see <a href="http://www.last.fm/stationchanges2010">here</a>
		 */
		public static RadioStation personalTag(String user, String tag) {
			return new RadioStation("lastfm://usertags/" + user + "/" + tag);
		}
	}
}
