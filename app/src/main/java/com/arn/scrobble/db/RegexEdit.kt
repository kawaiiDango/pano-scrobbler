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

    var order: Int = -1,
    var preset: String? = null,
    var name: String? = null,
    var pattern: String? = null,
    var replacement: String = "",

    @Embedded
    var extractionPatterns: ExtractionPatterns? = null,

    @field:TypeConverters(Converters::class)
    var fields: Set<String>? = null,

    @field:TypeConverters(Converters::class)
    var packages: Set<String>? = null,
    var replaceAll: Boolean = false,
    var caseSensitive: Boolean = false,
    var continueMatching: Boolean = false,
) : Parcelable {
    @SerialName("field")
    @IgnoredOnParcel
    @Ignore
    var fieldCompat: String? = null
}

@Parcelize
@Serializable
data class ExtractionPatterns(
    val extractionTrack: String,
    val extractionAlbum: String,
    val extractionArtist: String,
    val extractionAlbumArtist: String,
) : Parcelable