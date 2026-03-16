package com.arn.scrobble.db

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
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

    val track: String? = null,
    val album: String? = null,
    val artist: String? = null,
    val albumArtist: String? = null,

    @ColumnInfo(defaultValue = "1")
    val continueMatching: Boolean = true,
)
