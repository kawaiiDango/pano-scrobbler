package com.arn.scrobble.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey


/**
 * Created by arn on 11/09/2017.
 */

@Entity(
    tableName = SimpleEditsDao.tableName,
    indices = [
        Index(value = ["legacyHash"]),
        Index(value = ["origArtist", "origAlbum", "origTrack"], unique = true),
    ]
)
data class SimpleEdit(
    @PrimaryKey(autoGenerate = true)
    var _id: Int = 0,

    var legacyHash: String? = null,

    var origTrack: String = "",
    var origAlbum: String = "",
    var origArtist: String = "",
    var track: String = "",
    var album: String = "",
    var albumArtist: String = "",
    var artist: String = "",
)
