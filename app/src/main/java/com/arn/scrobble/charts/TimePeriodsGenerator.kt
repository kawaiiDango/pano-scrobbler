package com.arn.scrobble.charts

import android.content.Context
import android.os.Parcelable
import android.text.format.DateUtils
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.Period
import com.arn.scrobble.api.listenbrainz.ListenbrainzRanges
import com.arn.scrobble.utils.Stuff.setMidnight
import com.arn.scrobble.utils.Stuff.setUserFirstDayOfWeek
import kotlinx.coroutines.runBlocking
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Formatter
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.min

class TimePeriodsGenerator(
    private val beginTime: Long,
    private val anchorTime: Long,
    private val contextForFormatter: Context?,
) {
    private val cal by lazy { runBlocking { Calendar.getInstance().setUserFirstDayOfWeek() } }

    val days
        get(): List<TimePeriod> {
            val days = mutableListOf<TimePeriod>()

            cal.timeInMillis = beginTime
            cal.setMidnight()

            while (cal.timeInMillis < anchorTime) {
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, 1)
                val end = cal.timeInMillis
                val name = if (contextForFormatter != null)
                    DateUtils.formatDateTime(
                        contextForFormatter,
                        start,
                        DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_SHOW_DATE
                    )
                else
                    ""
                days += TimePeriod(
                    start,
                    end,
                    name = name
                )
            }
            return days.asReversed()
        }

    val weeks
        get(): List<TimePeriod> {
            val weeks = mutableListOf<TimePeriod>()
            val stringBuilder = StringBuilder()
            var formatter = Formatter(stringBuilder, Locale.getDefault())

            cal.timeInMillis = beginTime
            cal.setMidnight()
            cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)

            while (cal.timeInMillis < anchorTime) {
                val start = cal.timeInMillis
                cal.add(Calendar.WEEK_OF_YEAR, 1)
                val end = cal.timeInMillis

                if (contextForFormatter != null)
                    formatter = DateUtils.formatDateRange(
                        contextForFormatter,
                        formatter,
                        start,
                        end - 1,
                        DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_SHOW_DATE
                    )

                weeks += TimePeriod(
                    start,
                    end,
                    name = stringBuilder.toString()
                )
                stringBuilder.setLength(0)
            }
            return weeks.asReversed()
        }

    val months
        get(): List<TimePeriod> {
            val months = mutableListOf<TimePeriod>()
            cal.timeInMillis = beginTime
            cal.setMidnight()
            cal.set(Calendar.DAY_OF_MONTH, 1)

            while (cal.timeInMillis < anchorTime) {
                val start = cal.timeInMillis
                cal.add(Calendar.MONTH, 1)
                val end = cal.timeInMillis
                val name = if (contextForFormatter != null)
                    DateUtils.formatDateTime(
                        contextForFormatter,
                        start,
                        DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_NO_MONTH_DAY
                    )
                else ""
                months += TimePeriod(start, end, name = name)
            }
            return months.asReversed()
        }

    val years
        get(): List<TimePeriod> {
            val years = mutableListOf<TimePeriod>()
            val format = SimpleDateFormat("yyyy", Locale.getDefault())
            cal.timeInMillis = beginTime
            cal.setMidnight()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.MONTH, Calendar.JANUARY)

            while (cal.timeInMillis < anchorTime) {
                val start = cal.timeInMillis
                val name = if (contextForFormatter != null)
                    format.format(cal.time)
                else ""
                cal.add(Calendar.YEAR, 1)
                val end = cal.timeInMillis
                years += TimePeriod(start, end, name = name)
            }
            return years.asReversed()
        }

    val recentsTimeJumps
        get(): List<TimeJumpEntry> {

            val timeJumpEntries = mutableListOf<TimeJumpEntry>()
            val endTime = System.currentTimeMillis()

            fun addTimePeriod(
                calendarField: Int,
                addsTime: Boolean,
                type: TimePeriodType,
            ) {
                cal.timeInMillis = anchorTime
                cal.add(calendarField, if (addsTime) 1 else -1)
                if (cal.timeInMillis in beginTime..endTime) {
                    timeJumpEntries += TimeJumpEntry(
                        timeMillis = cal.timeInMillis,
                        type = type,
                        addsTime = addsTime
                    )
                }
            }

            addTimePeriod(Calendar.WEEK_OF_YEAR, false, TimePeriodType.WEEK)
            addTimePeriod(Calendar.WEEK_OF_YEAR, true, TimePeriodType.WEEK)
            addTimePeriod(Calendar.MONTH, false, TimePeriodType.MONTH)
            addTimePeriod(Calendar.MONTH, true, TimePeriodType.MONTH)
            addTimePeriod(Calendar.YEAR, false, TimePeriodType.YEAR)
            addTimePeriod(Calendar.YEAR, true, TimePeriodType.YEAR)

            return timeJumpEntries
        }

    val listenBrainzPeriods
        get(): List<TimePeriod> {
            val timePeriods = mutableListOf<TimePeriod>()
            val format: SimpleDateFormat

            var start: Long
            var end: Long

            fun resetCal() {
                cal.timeInMillis = anchorTime
                cal.setMidnight()
                cal.firstDayOfWeek = Calendar.MONDAY
            }

            // this week
            resetCal()
            cal[Calendar.DAY_OF_WEEK] = Calendar.MONDAY
            start = cal.timeInMillis
            cal.add(Calendar.WEEK_OF_YEAR, 1)
            end = cal.timeInMillis
            timePeriods += TimePeriod(start, end, tag = ListenbrainzRanges.this_week.name)

            // previous week
            resetCal()
            cal[Calendar.DAY_OF_WEEK] = Calendar.MONDAY
            cal.add(Calendar.WEEK_OF_YEAR, -1)
            timePeriods += TimePeriod(cal.timeInMillis, start, tag = ListenbrainzRanges.week.name)

            // this month
            resetCal()
            cal[Calendar.DAY_OF_MONTH] = 1
            start = cal.timeInMillis
            cal.add(Calendar.MONTH, 1)
            end = cal.timeInMillis

            timePeriods += TimePeriod(
                start, end,
                name = DateUtils.formatDateRange(
                    PlatformStuff.application,
                    start,
                    end - 1,
                    DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_NO_MONTH_DAY
                ), tag = ListenbrainzRanges.this_month.name
            )

            // previous month
            resetCal()
            cal[Calendar.DAY_OF_MONTH] = 1
            cal.add(Calendar.MONTH, -1)
            timePeriods += TimePeriod(
                cal.timeInMillis, start,
                name = DateUtils.formatDateRange(
                    PlatformStuff.application,
                    cal.timeInMillis,
                    start - 1,
                    DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_NO_MONTH_DAY
                ), tag = ListenbrainzRanges.month.name
            )


            // prev quarter
            resetCal()
            cal[Calendar.DAY_OF_MONTH] = 1
            val endMonth1 = if (cal[Calendar.MONTH] < Calendar.APRIL)
                Calendar.JANUARY
            else if (cal[Calendar.MONTH] < Calendar.JULY)
                Calendar.APRIL
            else if (cal[Calendar.MONTH] < Calendar.OCTOBER)
                Calendar.JULY
            else
                Calendar.OCTOBER

            cal[Calendar.MONTH] = endMonth1
            end = cal.timeInMillis
            cal.add(Calendar.MONTH, -3)
            start = cal.timeInMillis
            timePeriods += TimePeriod(
                start, end,
                name = DateUtils.formatDateRange(
                    PlatformStuff.application,
                    start,
                    end - 1,
                    DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_NO_MONTH_DAY
                ),
                tag = ListenbrainzRanges.quarter.name
            )

            // prev half year
            resetCal()
            cal[Calendar.DAY_OF_MONTH] = 1
            val endMonth2 = if (cal[Calendar.MONTH] < Calendar.JULY)
                Calendar.JANUARY
            else
                Calendar.JULY
            cal[Calendar.MONTH] = endMonth2
            end = cal.timeInMillis
            cal.add(Calendar.MONTH, -6)
            start = cal.timeInMillis
            timePeriods += TimePeriod(
                start, end,
                name = DateUtils.formatDateRange(
                    PlatformStuff.application,
                    start,
                    end - 1,
                    DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_NO_MONTH_DAY
                ),
                tag = ListenbrainzRanges.half_yearly.name
            )

            // this year
            format = SimpleDateFormat("yyyy", Locale.getDefault())
            resetCal()
            cal[Calendar.DAY_OF_YEAR] = 1
            start = cal.timeInMillis
            cal.add(Calendar.YEAR, 1)
            end = cal.timeInMillis
            timePeriods += TimePeriod(
                start, end,
                name = format.format(start),
                tag = ListenbrainzRanges.this_year.name
            )

            // previous year
            resetCal()
            cal[Calendar.DAY_OF_YEAR] = 1
            cal.add(Calendar.YEAR, -1)
            timePeriods += TimePeriod(
                cal.timeInMillis, start,
                name = format.format(cal.timeInMillis),
                tag = ListenbrainzRanges.year.name
            )

            // overall
            timePeriods += TimePeriod(Period.OVERALL).apply {
                tag = ListenbrainzRanges.all_time.name
            }

            return timePeriods
        }

    companion object {

        fun getContinuousPeriods() =
            Period.entries.map { TimePeriod(it) }

        fun Period.toDuration(
            registeredTime: Long = 0,
            endTime: Long = System.currentTimeMillis(),
        ) = when (this) {
            Period.WEEK -> TimeUnit.DAYS.toMillis(7)
            Period.MONTH -> TimeUnit.DAYS.toMillis(30)
            Period.QUARTER -> TimeUnit.DAYS.toMillis(90)
            Period.HALF_YEAR -> TimeUnit.DAYS.toMillis(180)
            Period.YEAR -> TimeUnit.DAYS.toMillis(365)
            Period.OVERALL -> min(
                TimeUnit.DAYS.toMillis(365 * 10),
                endTime - registeredTime
            )
        }

        fun Period.toTimePeriod(
            registeredTime: Long = 0,
            endTime: Long = System.currentTimeMillis(),
        ) = TimePeriod(endTime - toDuration(registeredTime, endTime), endTime, name = "")

        fun getScrobblingActivityPeriods(
            timePeriodp: TimePeriod,
            registeredTime: Long,
        ): List<TimePeriod> {

            var timePeriod = timePeriodp.copy()
            if (timePeriodp.period != null) {
                val cal = Calendar.getInstance()
                cal.timeInMillis += TimeUnit.DAYS.toMillis(1) // include today
                cal.setMidnight()
                timePeriod = timePeriodp.period.toTimePeriod(registeredTime, cal.timeInMillis)
                timePeriod.name = timePeriodp.name
            }

            val type = when (TimeUnit.MILLISECONDS.toDays(timePeriod.end - timePeriod.start)) {
                in 367 until Long.MAX_VALUE -> TimePeriodType.YEAR
                in 90 until 367 -> TimePeriodType.MONTH
                in 10 until 90 -> TimePeriodType.WEEK
                else -> TimePeriodType.DAY
            }

            val generator = TimePeriodsGenerator(timePeriod.start, timePeriod.end - 1, null)

            val timePeriods = when (type) {
                TimePeriodType.YEAR -> generator.years
                TimePeriodType.MONTH -> generator.months.take(12)
                TimePeriodType.WEEK -> generator.weeks
                TimePeriodType.DAY -> generator.days.take(7)
                else -> throw IllegalArgumentException("Invalid time period type")
            }

            val maxWeekCountToShowEndDate = 5

            val formatter: (TimePeriod) -> String = when (type) {
                TimePeriodType.YEAR -> {
                    { SimpleDateFormat("''yy", Locale.getDefault()).format(it.start) }
                }

                TimePeriodType.MONTH -> {
                    {
                        SimpleDateFormat("MMM", Locale.ENGLISH).format(it.start)
                        //.take(1).uppercase()
                    }
                }

                TimePeriodType.WEEK -> {
                    {
                        val f = SimpleDateFormat("dd", Locale.getDefault())
                        if (timePeriods.size > maxWeekCountToShowEndDate)
                            f.format(it.start)
                        else
                            "${f.format(it.start)}-${f.format(it.end)}"
                    }
                }

                TimePeriodType.DAY -> {
                    { SimpleDateFormat("EEE", Locale.getDefault()).format(it.start) }
                }

                else -> throw IllegalArgumentException("Invalid time period type")
            }

            timePeriods.forEach { it.name = formatter(it) }

            return timePeriods.asReversed()
        }
    }

}

@Parcelize
@Serializable
data class TimePeriod(
    val start: Long,
    val end: Long,
    val period: Period? = null,
    var name: String = DateUtils.formatDateRange(
        PlatformStuff.application,
        start,
        end - 1,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH
    ),
    var tag: String? = null,
) : Parcelable {

    constructor(period: Period) : this(
        -1,
        -1,
        period,
        name = PlatformStuff.application.resources!!.let {
            when (period) {
                Period.WEEK -> it.getQuantityString(R.plurals.num_weeks, 1, 1)
                Period.MONTH -> it.getQuantityString(R.plurals.num_months, 1, 1)
                Period.QUARTER -> it.getQuantityString(R.plurals.num_months, 3, 3)
                Period.HALF_YEAR -> it.getQuantityString(R.plurals.num_months, 6, 6)
                Period.YEAR -> it.getQuantityString(R.plurals.num_years, 1, 1)
                Period.OVERALL -> it.getString(R.string.charts_overall)
            }
        }
    )

    override fun toString() =
        "TimePeriod(start=${Date(start)}, end=${Date(end)}, period=$period, name=\"$name\")"
}

data class TimeJumpEntry(
    val timeMillis: Long,
    val type: TimePeriodType,
    val addsTime: Boolean,
)

enum class TimePeriodType {
    DAY,
    WEEK,
    MONTH,
    YEAR,
    CONTINUOUS,
    CUSTOM,
    LISTENBRAINZ,
}

enum class AllPeriods {
    THIS_WEEK,
    WEEK,
    THIS_MONTH,
    MONTH,
    QUARTER,
    HALF_YEAR,
    THIS_YEAR,
    YEAR,
    ALL_TIME
}