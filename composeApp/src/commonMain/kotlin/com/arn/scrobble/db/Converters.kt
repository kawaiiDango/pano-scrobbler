package com.arn.scrobble.db

import androidx.room.TypeConverter

object Converters {
    @TypeConverter
    fun fromCommaSeperatedString(value: String) =
        value.split(", ")
            .filter { it.isNotEmpty() }
            .toSet()

    @TypeConverter
    fun toCommaSeperatedString(value: Set<String>) = value.joinToString()

    @TypeConverter
    fun regexEditFieldsToCommaSeperatedString(value: Set<RegexEdit.Field>) =
        value.joinToString { it.name }

    @TypeConverter
    fun commaSeperatedStringToRegexEditFields(value: String) =
        value.split(", ")
            .filter { it.isNotEmpty() }
            .map {
                // hotfix for old data
                val v = if (it == "albumartist") RegexEdit.Field.albumArtist.name else it
                RegexEdit.Field.valueOf(v)
            }
            .toSet()
}
