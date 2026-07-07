package com.arn.scrobble.utils

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

actual object PanoTimeFormatter {


    actual fun relative(
        millis: Long,
        justNowString: String?,
        withPreposition: Boolean,
    ): String {
        val millis = millis
        val diff = System.currentTimeMillis() - millis
        val seconds = diff / 1000

        if (justNowString != null && (millis == 0L || seconds <= 60)) {
            return justNowString
        }

        val currentDay = LocalDateTime.now().dayOfYear
        val currentYear = LocalDateTime.now().year


        val dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(millis),
            ZoneId.systemDefault()
        )
        val formatter = when {
            currentDay == dateTime.dayOfYear && currentYear == dateTime.year -> {
                DateTimeFormatter.ofLocalizedTime(
                    FormatStyle.SHORT,
                )
            }

            else -> {
                DateTimeFormatter.ofLocalizedDateTime(
                    FormatStyle.MEDIUM,
                    FormatStyle.SHORT
                )
            }
        }

        return dateTime.format(formatter)
    }


    actual fun full(millis: Long): String {
        val dateTime = ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(millis),
            ZoneId.systemDefault()
        )
        val formatter = DateTimeFormatterBuilder()
            .appendLocalized(FormatStyle.LONG, FormatStyle.MEDIUM) // MEDIUM time has no zone letter
            .appendLiteral(' ')
            .appendOffset("+HH:MM", "Z")   // numeric offset, no resource bundle needed
            .toFormatter(Locale.getDefault())

        return dateTime.format(formatter)
    }

    actual fun short(millis: Long): String {
        val dateTime = Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .truncatedTo(ChronoUnit.SECONDS)

        val formatter = DateTimeFormatter.ISO_DATE_TIME

        return dateTime.format(formatter)
    }

    actual fun day(millis: Long): String {
        val dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(millis),
            ZoneId.systemDefault()
        )
        val currentYear = LocalDateTime.now().year
        val dateFormatter = DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
            .apply {
                if (dateTime.year != currentYear) {
                    appendPattern(" yyyy")
                }
            }
            .toFormatter(Locale.getDefault())
        return dateTime.format(dateFormatter)
    }

    actual fun month(millis: Long, short: Boolean): String {
        val monthPattern = if (short) "MMM" else "MMMM"

        val currentYear = LocalDateTime.now().year

        val dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(millis),
            ZoneId.systemDefault()
        )
        val dateFormatter = DateTimeFormatterBuilder()
            .appendPattern(monthPattern)
            .apply {
                if (dateTime.year != currentYear) {
                    appendPattern(" yyyy")
                }
            }
            .toFormatter(Locale.getDefault())
        return dateTime.format(dateFormatter)
    }

    actual fun monthRange(startMillis: Long, endMillis: Long): String {
        val startDateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(startMillis),
            ZoneId.systemDefault()
        )
        val endDateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(endMillis - 1),
            ZoneId.systemDefault()
        )
        val currentYear = LocalDateTime.now().year
        val formatterBuilder = DateTimeFormatterBuilder()
            .appendPattern("MMM")
            .apply {
                if (startDateTime.year != currentYear || endDateTime.year != currentYear) {
                    appendPattern(" yyyy")
                }
            }
        val formatter = formatterBuilder.toFormatter(Locale.getDefault())
        return Stuff.formatBigHyphen(
            startDateTime.format(formatter),
            endDateTime.format(formatter)
        )
    }

    actual fun dateRange(startMillis: Long, endMillis: Long): String {
        val startDateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(startMillis),
            ZoneId.systemDefault()
        )
        val endDateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(endMillis - 1),
            ZoneId.systemDefault()
        )
        val currentYear = LocalDateTime.now().year
        val formatterBuilder = DateTimeFormatterBuilder()
            .appendPattern("MMM dd")
            .apply {
                if (startDateTime.year != currentYear || endDateTime.year != currentYear) {
                    appendPattern(" yyyy")
                }
            }
        val formatter = formatterBuilder.toFormatter(Locale.getDefault())
        return Stuff.formatBigHyphen(
            startDateTime.format(formatter),
            endDateTime.format(formatter)
        )
    }

    actual fun year(millis: Long): String {
        val dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(millis),
            ZoneId.systemDefault()
        )
        val formatter = DateTimeFormatter.ofPattern("yyyy")
        return dateTime.format(formatter)
    }
}