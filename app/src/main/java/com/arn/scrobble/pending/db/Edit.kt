package com.arn.scrobble.pending.db

import androidx.room.Entity
import androidx.room.PrimaryKey


/**
 * Created by arn on 11/09/2017.
 */

@Entity(tableName = "edits")
class Edit {
    @PrimaryKey
    var hash: String = ""

    var track: String = ""
    var album: String = ""
    var albumArtist: String = ""
    var artist: String = ""

    override fun toString(): String {
        return "Edit [track=$track, album=$album, albumArtist=$albumArtist, artist=$artist]"
    }
}
