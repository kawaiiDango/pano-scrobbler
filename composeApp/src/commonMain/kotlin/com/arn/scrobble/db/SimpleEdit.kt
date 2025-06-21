package com.arn.scrobble.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


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
@Serializable
data class SimpleEdit(
    @PrimaryKey(autoGenerate = true)
    val _id: Int = 0,

    @SerialName("hash")
    val legacyHash: String? = null,

    val origTrack: String = "",
    val origAlbum: String = "",
    val origArtist: String = "",
    val track: String = "",
    val album: String = "",
    val albumArtist: String = "",
    val artist: String = "",
)
