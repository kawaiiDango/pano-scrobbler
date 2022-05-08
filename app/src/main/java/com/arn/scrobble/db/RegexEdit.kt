package com.arn.scrobble.db

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import kotlinx.parcelize.Parcelize

@Entity(
    tableName = RegexEditsDao.tableName,
    indices = [
        Index(value = ["preset"], unique = true),
        Index(value = ["order"]),
    ]
)
@Parcelize
data class RegexEdit(
    @PrimaryKey(autoGenerate = true)
    var _id: Int = 0,

    var order: Int = -1,
    var preset: String? = null,
    var name: String? = null,
    var pattern: String? = null,
    var replacement: String = "",

    @field:TypeConverters(Converters::class)
    var fields: Set<String>? = null,
    var replaceAll: Boolean = false,
    var caseSensitive: Boolean = false,
    var continueMatching: Boolean = false,
) : Parcelable