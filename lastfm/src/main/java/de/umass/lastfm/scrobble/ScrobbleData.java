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

/**
 * Class that holds all available fields for scrobble (and now playing) requests.
 *
 * @author Adrian Woodhead
 */
public class ScrobbleData {

    /**
     * The artist name. Required for scrobbling and now playing.
     */
    private String artist;

    /**
     * The track name. Required for scrobbling and now playing.
     */
    private String track;

    /**
     * The time the track started playing, in UNIX timestamp format (integer number of seconds since 00:00:00, January 1st 1970 UTC). This must
     * be in the UTC time zone. Required for scrobbling only.
     */
    private int timestamp = -1;

    /**
     * The length of the track in seconds. Optional.
     */
    private int duration = -1;

    /**
     * The album name. Optional.
     */
    private String album;

    /**
     * The album artist, if this differs from the track artist. Optional.
     */
    private String albumArtist;

    /**
     * The MusicBrainz track id. Optional.
     */
    private String musicBrainzId;

    /**
     * The position of the track on the album. Optional.
     */
    private int trackNumber = -1;

    /**
     * The stream id for this track if received from the radio.getPlaylist service. Optional.
     */
    private String streamId;

    /**
     * Set to true if the user chose this song, or false if the song was chosen by someone else (such as a radio station or recommendation
     * service). Optional.
     */
    private boolean chosenByUser = true;

    private String pkgName;

    public ScrobbleData() {
    }

    public ScrobbleData(ScrobbleData scrobbleData) {
        this(
                scrobbleData.getArtist(),
                scrobbleData.getTrack(),
                scrobbleData.getTimestamp(),
                scrobbleData.getDuration(),
                scrobbleData.getAlbum(),
                scrobbleData.getAlbumArtist(),
                scrobbleData.getMusicBrainzId(),
                scrobbleData.getTrackNumber(),
                scrobbleData.getStreamId(),
                scrobbleData.getPkgName(),
                scrobbleData.isChosenByUser()
        );
    }

    public ScrobbleData(String artist, String track, int timestamp) {
        this.artist = artist;
        this.track = track;
        this.timestamp = timestamp;
    }

    public ScrobbleData(String artist, String track, int timestamp, int duration, String album, String albumArtist, String musicBrainzId,
                        int trackNumber, String streamId, String pkgName, boolean chosenByUser) {
        this.artist = artist;
        this.track = track;
        this.timestamp = timestamp;
        this.duration = duration;
        this.album = album;
        this.albumArtist = albumArtist;
        this.musicBrainzId = musicBrainzId;
        this.trackNumber = trackNumber;
        this.streamId = streamId;
        this.chosenByUser = chosenByUser;
        this.pkgName = pkgName;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getTrack() {
        return track;
    }

    public void setTrack(String track) {
        this.track = track;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
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

    public String getMusicBrainzId() {
        return musicBrainzId;
    }

    public void setMusicBrainzId(String musicBrainzId) {
        this.musicBrainzId = musicBrainzId;
    }

    public int getTrackNumber() {
        return trackNumber;
    }

    public void setTrackNumber(int trackNumber) {
        this.trackNumber = trackNumber;
    }

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }

    public boolean isChosenByUser() {
        return chosenByUser;
    }

    public void setChosenByUser(boolean chosenByUser) {
        this.chosenByUser = chosenByUser;
    }

    public String getPkgName() {
        return pkgName;
    }

    public void setPkgName(String pkgName) {
        this.pkgName = pkgName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ScrobbleData that = (ScrobbleData) o;

        if (getTimestamp() != that.getTimestamp()) return false;
        if (getDuration() != that.getDuration()) return false;
        if (getTrackNumber() != that.getTrackNumber()) return false;
        if (isChosenByUser() != that.isChosenByUser()) return false;
        if (getArtist() != null ? !getArtist().equals(that.getArtist()) : that.getArtist() != null)
            return false;
        if (getTrack() != null ? !getTrack().equals(that.getTrack()) : that.getTrack() != null)
            return false;
        if (getAlbum() != null ? !getAlbum().equals(that.getAlbum()) : that.getAlbum() != null)
            return false;
        if (getAlbumArtist() != null ? !getAlbumArtist().equals(that.getAlbumArtist()) : that.getAlbumArtist() != null)
            return false;
        if (getMusicBrainzId() != null ? !getMusicBrainzId().equals(that.getMusicBrainzId()) : that.getMusicBrainzId() != null)
            return false;
        return getStreamId() != null ? getStreamId().equals(that.getStreamId()) : that.getStreamId() == null;
    }

    @Override
    public int hashCode() {
        int result = getArtist() != null ? getArtist().hashCode() : 0;
        result = 31 * result + (getTrack() != null ? getTrack().hashCode() : 0);
        result = 31 * result + getTimestamp();
        result = 31 * result + getDuration();
        result = 31 * result + (getAlbum() != null ? getAlbum().hashCode() : 0);
        result = 31 * result + (getAlbumArtist() != null ? getAlbumArtist().hashCode() : 0);
        result = 31 * result + (getMusicBrainzId() != null ? getMusicBrainzId().hashCode() : 0);
        result = 31 * result + getTrackNumber();
        result = 31 * result + (getStreamId() != null ? getStreamId().hashCode() : 0);
        result = 31 * result + (isChosenByUser() ? 1 : 0);
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "ScrobbleData [track=" + track + ", artist=" + artist + ", album=" + album + ", albumArtist=" + albumArtist + ", duration="
                + duration + ", musicBrainzId=" + musicBrainzId + ", timestamp=" + timestamp + ", trackNumber=" + trackNumber
                + ", streamId=" + streamId + ", pkgName=redacted, chosenByUser=" + chosenByUser + "]";
    }

}
