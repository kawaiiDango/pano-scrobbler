package com.arn.scrobble.db

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Entity(
    tableName = RegexEditsDao.tableName,
    indices = [
        Index(value = ["preset"], unique = true),
        Index(value = ["order"]),
    ]
)
@Parcelize
@Serializable
data class RegexEdit(
    @PrimaryKey(autoGenerate = true)
    @Transient
    val _id: Int = 0,

    val order: Int = -1,
    val preset: String? = null,
    val name: String? = null,
    val pattern: String? = null,
    val replacement: String = "",

    @Embedded
    val extractionPatterns: ExtractionPatterns? = null,

    @field:TypeConverters(Converters::class)
    val fields: Set<String>? = null,

    @field:TypeConverters(Converters::class)
    val packages: Set<String>? = null,
    val replaceAll: Boolean = false,
    val caseSensitive: Boolean = false,
    val continueMatching: Boolean = false,
) : Parcelable {
    @SerialName("field")
    @IgnoredOnParcel
    @Ignore
    val fieldCompat: String? = null
}

@Parcelize
@Serializable
data class ExtractionPatterns(
    val extractionTrack: String,
    val extractionAlbum: String,
    val extractionArtist: String,
    val extractionAlbumArtist: String,
) : Parcelable