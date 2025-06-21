package com.arn.scrobble.db

import androidx.annotation.Keep
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import kotlinx.serialization.Serializable

@Entity(
    tableName = RegexEditsDao.tableName,
    indices = [
        Index(value = ["order"]),
    ]
)
@Serializable
data class RegexEdit(
    @PrimaryKey(autoGenerate = true)
    val _id: Int = 0,

    val order: Int = -1,
    val name: String,

    @Embedded
    val search: SearchPatterns,
    @Embedded
    val replacement: ReplacementPatterns? = null,

    @field:TypeConverters(Converters::class)
    val appIds: Set<String> = emptySet(),
    val caseSensitive: Boolean = false,
    val blockPlayerAction: BlockPlayerAction? = null,
) {
    @Serializable
    data class SearchPatterns(
        val searchTrack: String,
        val searchAlbum: String,
        val searchArtist: String,
        val searchAlbumArtist: String,
    )

    @Serializable
    data class ReplacementPatterns(
        val replacementTrack: String,
        val replacementAlbum: String,
        val replacementArtist: String,
        val replacementAlbumArtist: String,
        val replaceAll: Boolean = false,
    )

    @Keep
    enum class Field {
        track, albumArtist, artist, album
    }
}