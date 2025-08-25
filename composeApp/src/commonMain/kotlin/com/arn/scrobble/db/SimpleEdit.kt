package com.arn.scrobble.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable


/**
 * Created by arn on 11/09/2017.
 */

@Entity(
    tableName = SimpleEditsDao.tableName,
    indices = [
        Index(
            value = [
                "hasOrigTrack",
                "origTrack",

                "hasOrigArtist",
                "origArtist",

                "hasOrigAlbum",
                "origAlbum",

                "hasOrigAlbumArtist",
                "origAlbumArtist",
            ], unique = true
        ),
    ]
)
@Serializable
data class SimpleEdit(
    @PrimaryKey(autoGenerate = true)
    val _id: Int = 0,

    @ColumnInfo(defaultValue = "1")
    val hasOrigTrack: Boolean = true,
    val origTrack: String = "",

    @ColumnInfo(defaultValue = "1")
    val hasOrigArtist: Boolean = true,
    val origArtist: String = "",

    @ColumnInfo(defaultValue = "1")
    val hasOrigAlbum: Boolean = true,
    val origAlbum: String = "",

    @ColumnInfo(defaultValue = "0")
    val hasOrigAlbumArtist: Boolean = false,
    @ColumnInfo(defaultValue = "")
    val origAlbumArtist: String = "",

    val track: String? = "",
    val album: String? = "",
    val albumArtist: String? = "",
    val artist: String? = "",
)
