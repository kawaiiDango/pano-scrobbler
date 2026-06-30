package com.arn.scrobble.utils

import android.os.Build
import android.text.format.DateUtils
import com.arn.scrobble.utils.AndroidStuff.applicationContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.TimeZone

actual object PanoTimeFormatter {

    actual fun relative(
        millis: Long,
        justNowString: String?,
        withPreposition: Boolean,
    ): String {
        val context = applicationContext
        val millis = millis
        val diff = System.currentTimeMillis() - millis
        return when {
            justNowString != null && (millis == 0L || diff <= DateUtils.MINUTE_IN_MILLIS) -> {
                justNowString
            }

            withPreposition -> {
                DateUtils.getRelativeTimeSpanString(
                    context,
                    millis,
                    true
                ).toString()
            }

            else -> {
                DateUtils.getRelativeDateTimeString(
                    context,
                    millis,
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.DAY_IN_MILLIS * 2,
                    DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_SHOW_TIME
                ).toString()
            }
        }
    }

    actual fun full(millis: Long): String {
        return DateUtils.formatDateTime(
            applicationContext,
            millis,
            DateUtils.FORMAT_SHOW_DATE or
                    DateUtils.FORMAT_SHOW_TIME or
                    DateUtils.FORMAT_SHOW_YEAR
        ) + " " + TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT)
    }

    actual fun short(millis: Long): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return full(millis)
        }

        val dateTime = Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .truncatedTo(ChronoUnit.SECONDS)

        val formatter = DateTimeFormatter.ISO_DATE_TIME

        return dateTime.format(formatter)
    }

    actual fun day(millis: Long): String {
        return DateUtils.formatDateTime(
            applicationContext,
            millis,
            DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_SHOW_DATE
        )
    }

    actual fun month(millis: Long, short: Boolean): String {
        val shortFlag = if (short) DateUtils.FORMAT_ABBREV_MONTH else 0

        return DateUtils.formatDateTime(
            applicationContext,
            millis,
            shortFlag or DateUtils.FORMAT_NO_MONTH_DAY
        )
    }

    actual fun monthRange(startMillis: Long, endMillis: Long): String {
        return DateUtils.formatDateRange(
            applicationContext,
            startMillis,
            endMillis - 1,
            DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_NO_MONTH_DAY
        )
    }

    actual fun dateRange(startMillis: Long, endMillis: Long): String {
        return DateUtils.formatDateRange(
            applicationContext,
            startMillis,
            endMillis - 1,
            DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_SHOW_DATE
        )
    }

    actual fun year(millis: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        return calendar.get(Calendar.YEAR).toString()
    }
}