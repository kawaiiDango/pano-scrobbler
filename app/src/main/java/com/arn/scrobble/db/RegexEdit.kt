package com.arn.scrobble.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "regexEdits",
    indices = [
        Index(value = ["preset"], unique = true),
        Index(value = ["order"]),
    ]
)
data class RegexEdit(
    @PrimaryKey(autoGenerate = true)
    var _id: Int = 0,

    var order: Int = -1,
    var preset: String? = null,
    var name: String? = null,
    var pattern: String? = null,
    var replacement: String = "",
    var field: String? = null,
    var replaceAll: Boolean = false,
    var caseSensitive: Boolean = false,
    var continueMatching: Boolean = false,
)