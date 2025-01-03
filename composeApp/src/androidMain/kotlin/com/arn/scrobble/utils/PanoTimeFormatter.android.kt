package com.arn.scrobble.utils

import android.text.format.DateUtils
import com.arn.scrobble.utils.AndroidStuff.application
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.time_just_now
import java.util.Calendar
import java.util.TimeZone

actual object PanoTimeFormatter {

    actual fun relative(
        millis: Long,
        withPreposition: Boolean,
    ): String {
        val context = application
        val millis = millis
        val diff = System.currentTimeMillis() - millis
        return when {
            millis == 0L || diff <= DateUtils.MINUTE_IN_MILLIS -> {
                runBlocking { // todo remove runBlocking
                    getString(Res.string.time_just_now)
                }
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
            application,
            millis,
            DateUtils.FORMAT_SHOW_DATE or
                    DateUtils.FORMAT_SHOW_TIME or
                    DateUtils.FORMAT_SHOW_YEAR
        ) + " " + TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT)
    }

    actual fun day(millis: Long): String {
        return DateUtils.formatDateTime(
            application,
            millis,
            DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_SHOW_DATE
        )
    }

    actual fun month(millis: Long): String {
        return DateUtils.formatDateTime(
            application,
            millis,
            DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_NO_MONTH_DAY
        )
    }

    actual fun monthRange(startMillis: Long, endMillis: Long): String {
        return DateUtils.formatDateRange(
            application,
            startMillis,
            endMillis - 1,
            DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_NO_MONTH_DAY
        )
    }

    actual fun dateRange(startMillis: Long, endMillis: Long): String {
        return DateUtils.formatDateRange(
            application,
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