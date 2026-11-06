package com.arn.scrobble.db

import androidx.room3.ColumnTypeConverter
import kotlinx.serialization.json.Json

object StringMapTypeConverter {
    @ColumnTypeConverter
    fun fromMap(map: Map<String, String>): String =
        if (map.isEmpty()) ""
        else Json.encodeToString<Map<String, String>>(map)

    @ColumnTypeConverter
    fun toMap(json: String): Map<String, String> =
        if (json.isEmpty()) emptyMap()
        else Json.decodeFromString<Map<String, String>>(json)
}
