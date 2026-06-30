package com.arn.scrobble.db

import androidx.room3.ColumnTypeConverter

object Converters {
    @ColumnTypeConverter
    fun fromCommaSeperatedString(value: String) =
        value.split(", ")
            .filter { it.isNotEmpty() }
            .toSet()

    @ColumnTypeConverter
    fun toCommaSeperatedString(value: Set<String>) = value.joinToString()

    @ColumnTypeConverter
    fun regexEditFieldsToCommaSeperatedString(value: Set<RegexEdit.Field>) =
        value.joinToString { it.name }

    @ColumnTypeConverter
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
