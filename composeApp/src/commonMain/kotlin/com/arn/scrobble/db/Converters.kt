package com.arn.scrobble.db

import androidx.room.TypeConverter

object Converters {
    @TypeConverter
    fun fromCommaSeperatedString(value: String?) =
        value?.split(", ")
            ?.filter { it.isNotEmpty() }
            // hotfix for old data
            ?.map { if (it == "albumartist") RegexEditFields.ALBUM_ARTIST else it }
            ?.toSet()

    @TypeConverter
    fun toCommaSeperatedString(value: Set<String>?) =
        value?.joinToString()?.ifEmpty { null }
}
