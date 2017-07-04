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
package de.umass.lastfm.scrobble;

import de.umass.lastfm.Result;

/**
 * Result object which contains extra information returned by scrobble and now playing requests.
 *
 * @author Adrian Woodhead
 */
public class ScrobbleResult extends Result {

	private String track;
	private String artist;
	private String album;
	private String albumArtist;
	private int timestamp;
	private boolean trackCorrected;
	private boolean artistCorrected;
	private boolean albumCorrected;
	private boolean albumArtistCorrected;
	private boolean ignored;
	private IgnoredMessageCode ignoredMessageCode;
	private String ignoredMessage;

	public ScrobbleResult(Result result) {
		super(result.getResultDocument());
		super.status = result.getStatus();
		super.errorMessage = result.getErrorMessage();
		super.errorCode = result.getErrorCode();
		super.httpErrorCode = result.getHttpErrorCode();
	}

	public String getTrack() {
		return track;
	}

	public void setTrack(String track) {
		this.track = track;
	}

	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public String getAlbum() {
		return album;
	}

	public void setAlbum(String album) {
		this.album = album;
	}

	public String getAlbumArtist() {
		return albumArtist;
	}

	public void setAlbumArtist(String albumArtist) {
		this.albumArtist = albumArtist;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}

	public boolean isTrackCorrected() {
		return trackCorrected;
	}

	public void setTrackCorrected(boolean trackCorrected) {
		this.trackCorrected = trackCorrected;
	}

	public boolean isArtistCorrected() {
		return artistCorrected;
	}

	public void setArtistCorrected(boolean artistCorrected) {
		this.artistCorrected = artistCorrected;
	}

	public boolean isAlbumCorrected() {
		return albumCorrected;
	}

	public void setAlbumCorrected(boolean albumCorrected) {
		this.albumCorrected = albumCorrected;
	}

	public boolean isAlbumArtistCorrected() {
		return albumArtistCorrected;
	}

	public void setAlbumArtistCorrected(boolean albumArtistCorrected) {
		this.albumArtistCorrected = albumArtistCorrected;
	}

	public boolean isIgnored() {
		return ignored;
	}

	public void setIgnored(boolean ignored) {
		this.ignored = ignored;
	}

	public IgnoredMessageCode getIgnoredMessageCode() {
		return ignoredMessageCode;
	}

	public void setIgnoredMessageCode(IgnoredMessageCode ignoredMessageCode) {
		this.ignoredMessageCode = ignoredMessageCode;
	}

	public String getIgnoredMessage() {
		return ignoredMessage;
	}

	public void setIgnoredMessage(String ignoredMessage) {
		this.ignoredMessage = ignoredMessage;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */

	@Override
	public String toString() {
		return "ScrobbleResult [" + super.toString() + ", track=" + track + ", trackCorrected=" + trackCorrected + ", artist=" + artist
				+ ", artistCorrected=" + artistCorrected + ", album=" + album + ", albumCorrected=" + albumCorrected + ", albumArtist="
				+ albumArtist + ", albumArtistCorrected=" + albumArtistCorrected + ", ignored=" + ignored + ", ignoredMessageCode="
				+ ignoredMessageCode + ", ignoredMessage=" + ignoredMessage + ", timestamp=" + timestamp + "]";
	}

}
