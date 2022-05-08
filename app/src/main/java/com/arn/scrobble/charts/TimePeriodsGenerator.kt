package com.arn.scrobble.charts

import android.content.Context
import android.text.format.DateUtils
import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.annotation.PluralsRes
import com.arn.scrobble.R
import com.arn.scrobble.Stuff.setMidnight
import de.umass.lastfm.Period
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.min

class TimePeriodsGenerator(
    private val beginTime: Long,
    private val anchorTime: Long,
    private val contextForFormatter: Context?
) {
    private val cal by lazy { Calendar.getInstance() }

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
                    name
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
                    stringBuilder.toString()
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
                months += TimePeriod(start, end, name)
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
                years += TimePeriod(start, end, name)
            }
            return years.asReversed()
        }

    val recentsTimeJumps
        get(): List<Pair<TimePeriod, Int>> {

            val timePeriods = mutableListOf<Pair<TimePeriod, Int>>()
            val endTime = System.currentTimeMillis()

            fun addTimePeriod(
                calendarField: Int,
                valueToAdd: Int,
                @PluralsRes pluralsRes: Int,
                @DrawableRes iconRes: Int
            ) {
                cal.timeInMillis = anchorTime
                cal.add(calendarField, valueToAdd)
                if (cal.timeInMillis in beginTime..endTime) {

                    val name = contextForFormatter?.resources?.getQuantityString(
                        pluralsRes,
                        abs(valueToAdd),
                        (if (valueToAdd > 0) "+" else "") + valueToAdd
                    )
                        ?: ""

                    timePeriods += TimePeriod(
                        beginTime,
                        cal.timeInMillis,
                        name
                    ) to iconRes
                }
            }

            addTimePeriod(Calendar.WEEK_OF_YEAR, -1, R.plurals.num_weeks, R.drawable.vd_week)
            addTimePeriod(Calendar.WEEK_OF_YEAR, 1, R.plurals.num_weeks, R.drawable.vd_week)
            addTimePeriod(Calendar.MONTH, -1, R.plurals.num_months, R.drawable.vd_month)
            addTimePeriod(Calendar.MONTH, 1, R.plurals.num_months, R.drawable.vd_month)
            addTimePeriod(Calendar.YEAR, -1, R.plurals.num_years, R.drawable.vd_calendar)
            addTimePeriod(Calendar.YEAR, 1, R.plurals.num_years, R.drawable.vd_calendar)

            return timePeriods
        }

    companion object {

        fun getContinuousPeriods(context: Context) =
            Period.values().map {
                TimePeriod(context, it)
            }

        fun Period.toDuration(
            registeredTime: Long = 0,
            endTime: Long = System.currentTimeMillis()
        ) = when (this) {
            Period.WEEK -> TimeUnit.DAYS.toMillis(7)
            Period.ONE_MONTH -> TimeUnit.DAYS.toMillis(30)
            Period.THREE_MONTHS -> TimeUnit.DAYS.toMillis(90)
            Period.SIX_MONTHS -> TimeUnit.DAYS.toMillis(180)
            Period.TWELVE_MONTHS -> TimeUnit.DAYS.toMillis(365)
            Period.OVERALL -> min(
                TimeUnit.DAYS.toMillis(365 * 10),
                endTime - registeredTime
            )
        }

        fun Period.toTimePeriod(
            registeredTime: Long = 0,
            endTime: Long = System.currentTimeMillis()
        ) = TimePeriod(endTime - toDuration(registeredTime, endTime), endTime)

        fun getSparklinePeriods(
            timePeriodp: TimePeriod,
            registeredTime: Long
        ): Pair<List<TimePeriod>, TimePeriodType> {

            var timePeriod = timePeriodp.copy()
            if (timePeriodp.period != null) {
                val cal = Calendar.getInstance()
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
                TimePeriodType.MONTH -> generator.months
                TimePeriodType.WEEK -> generator.weeks
                TimePeriodType.DAY -> generator.days
                else -> throw IllegalArgumentException("Invalid time period type")
            }

            val maxWeekCountToShowEndDate = 5

            val formatter: (TimePeriod) -> String = when (type) {
                TimePeriodType.YEAR -> {
                    { SimpleDateFormat("''yy", Locale.getDefault()).format(it.start) }
                }
                TimePeriodType.MONTH -> {
                    {
                        SimpleDateFormat("MMM", Locale.ENGLISH).format(it.start).take(1)
                            .uppercase()
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
                    { SimpleDateFormat("dd", Locale.getDefault()).format(it.start) }
                }
                else -> throw IllegalArgumentException("Invalid time period type")
            }

            timePeriods.forEach { it.name = formatter(it) }

            return timePeriods.asReversed() to type
        }
    }

}

@kotlinx.serialization.Serializable
data class TimePeriod(
    val start: Long,
    val end: Long,
    val period: Period? = null,
) {

    var name: String = ""

    constructor(context: Context, period: Period) : this(
        -1,
        -1,
        period
    ) {
        name = when (period) {
            Period.WEEK -> context.resources.getQuantityString(R.plurals.num_weeks, 1, 1)
            Period.ONE_MONTH -> context.resources.getQuantityString(R.plurals.num_months, 1, 1)
            Period.THREE_MONTHS -> context.resources.getQuantityString(R.plurals.num_months, 3, 3)
            Period.SIX_MONTHS -> context.resources.getQuantityString(R.plurals.num_months, 6, 6)
            Period.TWELVE_MONTHS -> context.resources.getQuantityString(R.plurals.num_years, 1, 1)
            Period.OVERALL -> context.getString(R.string.charts_overall)
        }
    }

    constructor(context: Context, start: Long, end: Long) : this(start, end) {
        name = DateUtils.formatDateRange(
            context,
            start,
            end - 1,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH
        )
    }

    constructor(start: Long, end: Long, name: String) : this(start, end) {
        this.name = name
    }

    override fun toString() =
        "TimePeriod(start=${Date(start)}, end=${Date(end)}, period=$period, name=\"$name\")"
}

enum class TimePeriodType {
    DAY,
    WEEK,
    MONTH,
    YEAR,
    CONTINUOUS,
    CUSTOM,
}