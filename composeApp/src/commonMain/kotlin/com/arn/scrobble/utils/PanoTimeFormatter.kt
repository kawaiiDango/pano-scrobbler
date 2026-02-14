package com.arn.scrobble.utils

expect object PanoTimeFormatter {

    fun relative(millis: Long, justNowString: String?, withPreposition: Boolean = false): String

    fun full(millis: Long): String

    fun short(millis: Long): String

    fun day(millis: Long): String

    fun month(millis: Long): String

    fun dateRange(startMillis: Long, endMillis: Long): String

    fun monthRange(startMillis: Long, endMillis: Long): String

    fun year(millis: Long): String
}