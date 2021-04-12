package com.arn.scrobble.db

import androidx.room.Entity
import androidx.room.Index


/**
 * Created by arn on 11/09/2017.
 */

@Entity(tableName = "edits",
        primaryKeys = ["origArtist", "origAlbum", "origTrack"],
        indices = [Index(name = "legacyIdx", value = ["legacyHash"])])
class Edit {
    var legacyHash: String? = null

    var origTrack: String = ""
    var origAlbum: String = ""
    var origArtist: String = ""
    var track: String = ""
    var album: String = ""
    var albumArtist: String = ""
    var artist: String = ""

    override fun toString(): String {
        return "Edit [hash=$legacyHash track=$track, album=$album, albumArtist=$albumArtist, artist=$artist]"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Edit

        if (legacyHash != other.legacyHash) return false
        if (origTrack != other.origTrack) return false
        if (origAlbum != other.origAlbum) return false
        if (origArtist != other.origArtist) return false
        if (track != other.track) return false
        if (album != other.album) return false
        if (albumArtist != other.albumArtist) return false
        if (artist != other.artist) return false

        return true
    }

    override fun hashCode(): Int {
        var result = legacyHash?.hashCode() ?: 0
        result = 31 * result + origTrack.hashCode()
        result = 31 * result + origAlbum.hashCode()
        result = 31 * result + origArtist.hashCode()
        result = 31 * result + track.hashCode()
        result = 31 * result + album.hashCode()
        result = 31 * result + albumArtist.hashCode()
        result = 31 * result + artist.hashCode()
        return result
    }

}
