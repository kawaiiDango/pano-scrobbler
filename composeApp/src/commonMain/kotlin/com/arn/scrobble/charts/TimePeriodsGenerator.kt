package com.arn.scrobble.charts

import com.arn.scrobble.api.lastfm.LastfmPeriod
import com.arn.scrobble.api.listenbrainz.ListenBrainzRange
import com.arn.scrobble.utils.PanoTimeFormatter
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff.setMidnight
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.charts_overall
import pano_scrobbler.composeapp.generated.resources.num_months
import pano_scrobbler.composeapp.generated.resources.num_weeks
import pano_scrobbler.composeapp.generated.resources.num_years
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.min


class TimePeriodsGenerator(
    private val beginTime: Long,
    private val anchorTime: Long,
    firstDayOfWeek: Int,
    private val generateFormattedStrings: Boolean = false,
) {
    private val cal by lazy {
        Calendar.getInstance().apply {
            if (firstDayOfWeek in Calendar.SUNDAY..Calendar.SATURDAY)
                this.firstDayOfWeek = firstDayOfWeek
        }
    }

    fun days(): List<TimePeriod> {
        val days = mutableListOf<TimePeriod>()

        cal.timeInMillis = beginTime
        cal.setMidnight()

        while (cal.timeInMillis < anchorTime) {
            val start = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val end = cal.timeInMillis
            val name = if (generateFormattedStrings)
                PanoTimeFormatter.day(start)
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

    fun weeks(): List<TimePeriod> {
        val weeks = mutableListOf<TimePeriod>()

        cal.timeInMillis = beginTime
        cal.setMidnight()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)

        while (cal.timeInMillis < anchorTime) {
            val start = cal.timeInMillis
            cal.add(Calendar.WEEK_OF_YEAR, 1)
            val end = cal.timeInMillis

            weeks += TimePeriod(
                start,
                end,
                name = if (generateFormattedStrings)
                    PanoTimeFormatter.dateRange(start, end)
                else ""
            )
        }
        return weeks.asReversed()
    }

    fun months(): List<TimePeriod> {
        val months = mutableListOf<TimePeriod>()
        cal.timeInMillis = beginTime
        cal.setMidnight()
        cal.set(Calendar.DAY_OF_MONTH, 1)

        while (cal.timeInMillis < anchorTime) {
            val start = cal.timeInMillis
            cal.add(Calendar.MONTH, 1)
            val end = cal.timeInMillis
            val name = if (generateFormattedStrings)
                PanoTimeFormatter.month(start)
            else ""
            months += TimePeriod(start, end, name = name)
        }
        return months.asReversed()
    }

    fun years(): List<TimePeriod> {
        val years = mutableListOf<TimePeriod>()
        val format = SimpleDateFormat("yyyy", Locale.getDefault())
        cal.timeInMillis = beginTime
        cal.setMidnight()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.MONTH, Calendar.JANUARY)

        while (cal.timeInMillis < anchorTime) {
            val start = cal.timeInMillis
            val name = if (generateFormattedStrings)
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

    suspend fun listenBrainz(): List<TimePeriod> {
        val timePeriods = mutableListOf<TimePeriod>()

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
        timePeriods += TimePeriod(
            start, end,
            name = PanoTimeFormatter.dateRange(start, end),
            listenBrainzRange = ListenBrainzRange.this_week
        )

        // previous week
        resetCal()
        cal[Calendar.DAY_OF_WEEK] = Calendar.MONDAY
        cal.add(Calendar.WEEK_OF_YEAR, -1)
        timePeriods += TimePeriod(
            cal.timeInMillis, start,
            name = PanoTimeFormatter.dateRange(cal.timeInMillis, start),
            listenBrainzRange = ListenBrainzRange.week
        )

        // this month
        resetCal()
        cal[Calendar.DAY_OF_MONTH] = 1
        start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        end = cal.timeInMillis

        timePeriods += TimePeriod(
            start, end,
            name = PanoTimeFormatter.month(start),
            listenBrainzRange = ListenBrainzRange.this_month
        )

        // previous month
        resetCal()
        cal[Calendar.DAY_OF_MONTH] = 1
        cal.add(Calendar.MONTH, -1)
        timePeriods += TimePeriod(
            cal.timeInMillis, start,
            name = PanoTimeFormatter.month(cal.timeInMillis),
            listenBrainzRange = ListenBrainzRange.month
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
            name = PanoTimeFormatter.monthRange(start, end),
            listenBrainzRange = ListenBrainzRange.quarter
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
            name = PanoTimeFormatter.monthRange(start, end),
            listenBrainzRange = ListenBrainzRange.half_yearly
        )

        // this year
        resetCal()
        cal[Calendar.DAY_OF_YEAR] = 1
        start = cal.timeInMillis
        cal.add(Calendar.YEAR, 1)
        end = cal.timeInMillis
        timePeriods += TimePeriod(
            start, end,
            name = PanoTimeFormatter.year(start),
            listenBrainzRange = ListenBrainzRange.this_year
        )

        // previous year
        resetCal()
        cal[Calendar.DAY_OF_YEAR] = 1
        cal.add(Calendar.YEAR, -1)
        timePeriods += TimePeriod(
            cal.timeInMillis, start,
            name = PanoTimeFormatter.year(cal.timeInMillis),
            listenBrainzRange = ListenBrainzRange.year
        )

        // overall
        timePeriods += TimePeriod(
            LastfmPeriod.OVERALL,
            name = getString(Res.string.charts_overall),
        )

        return timePeriods
    }

    companion object {

        private suspend fun getContinuousPeriodString(lastfmPeriod: LastfmPeriod) =
            when (lastfmPeriod) {
                LastfmPeriod.WEEK -> getPluralString(Res.plurals.num_weeks, 1, 1)
                LastfmPeriod.MONTH -> getPluralString(Res.plurals.num_months, 1, 1)
                LastfmPeriod.QUARTER -> getPluralString(Res.plurals.num_months, 3, 3)
                LastfmPeriod.HALF_YEAR -> getPluralString(Res.plurals.num_months, 6, 6)
                LastfmPeriod.YEAR -> getPluralString(Res.plurals.num_years, 1, 1)
                LastfmPeriod.OVERALL -> getString(Res.string.charts_overall)
            }

        suspend fun getContinuousPeriods() =
            LastfmPeriod.entries.map { TimePeriod(it, name = getContinuousPeriodString(it)) }

        fun LastfmPeriod.toDuration(
            registeredTime: Long = 0,
            endTime: Long = System.currentTimeMillis(),
        ) = when (this) {
            LastfmPeriod.WEEK -> TimeUnit.DAYS.toMillis(7)
            LastfmPeriod.MONTH -> TimeUnit.DAYS.toMillis(30)
            LastfmPeriod.QUARTER -> TimeUnit.DAYS.toMillis(90)
            LastfmPeriod.HALF_YEAR -> TimeUnit.DAYS.toMillis(180)
            LastfmPeriod.YEAR -> TimeUnit.DAYS.toMillis(365)
            LastfmPeriod.OVERALL -> min(
                TimeUnit.DAYS.toMillis(365 * 10),
                endTime - registeredTime
            )
        }

        fun LastfmPeriod.toTimePeriod(
            registeredTime: Long = 0,
            endTime: Long = System.currentTimeMillis(),
            name: String = ""
        ) = TimePeriod(endTime - toDuration(registeredTime, endTime), endTime, name = name)

        suspend fun forListeningActivity(
            timePeriodp: TimePeriod,
            registeredTime: Long,
        ): ListeningActivity {

            val timePeriod = if (timePeriodp.lastfmPeriod != null) {
                val cal = Calendar.getInstance()
                cal.timeInMillis += TimeUnit.DAYS.toMillis(1) // include today
                cal.setMidnight()
                timePeriodp.lastfmPeriod
                    .toTimePeriod(registeredTime, cal.timeInMillis, timePeriodp.name)
            } else {
                timePeriodp
            }

            val type = when (TimeUnit.MILLISECONDS.toDays(timePeriod.end - timePeriod.start)) {
                in 367 until Long.MAX_VALUE -> TimePeriodType.YEAR
                in 90 until 367 -> TimePeriodType.MONTH
                in 10 until 90 -> TimePeriodType.WEEK
                else -> TimePeriodType.DAY
            }

            val firstDayOfWeek = PlatformStuff.mainPrefs.data.map { it.firstDayOfWeek }.first()

            val generator = TimePeriodsGenerator(
                timePeriod.start,
                timePeriod.end - 1,
                firstDayOfWeek,
            )

            val timePeriods = when (type) {
                TimePeriodType.YEAR -> generator.years()
                TimePeriodType.MONTH -> generator.months().take(12)
                TimePeriodType.WEEK -> generator.weeks()
                TimePeriodType.DAY -> generator.days().take(7)
                else -> throw IllegalArgumentException("Invalid time period type")
            }

            val formatter: (TimePeriod) -> String = when (type) {
                TimePeriodType.YEAR -> {
                    { SimpleDateFormat("''yy", Locale.getDefault()).format(it.start) }
                }

                TimePeriodType.MONTH -> {
                    {
                        if (timePeriods.size <= 7)
                            SimpleDateFormat("MMM", Locale.getDefault()).format(it.start)
                        else
                            SimpleDateFormat("MM", Locale.getDefault()).format(it.start)
                    }
                }

                TimePeriodType.WEEK -> {
                    {
                        // https://stackoverflow.com/a/9721171/1067596
                        val df = DateFormat.getDateInstance(DateFormat.SHORT) as SimpleDateFormat
                        val pattern = df.toLocalizedPattern().replace(".?[Yy].?".toRegex(), "")
                        val f = SimpleDateFormat(pattern, Locale.getDefault())

                        "${f.format(it.start)}-\n${f.format(it.end)}"
                    }
                }

                TimePeriodType.DAY -> {
                    { SimpleDateFormat("EEE", Locale.getDefault()).format(it.start) }
                }
            }

            val timePeriodsToCounts = timePeriods.asReversed()
                .associate { it.copy(name = formatter(it)) to 0 }

            return ListeningActivity(
                timePeriodsToCounts = timePeriodsToCounts,
                type = type
            )
        }
    }

}

@Serializable
data class TimePeriod(
    val start: Long,
    val end: Long,
    val lastfmPeriod: LastfmPeriod? = null,
    val listenBrainzRange: ListenBrainzRange? = when (lastfmPeriod) {
        LastfmPeriod.WEEK -> ListenBrainzRange.week
        LastfmPeriod.MONTH -> ListenBrainzRange.month
        LastfmPeriod.QUARTER -> ListenBrainzRange.quarter
        LastfmPeriod.HALF_YEAR -> ListenBrainzRange.half_yearly
        LastfmPeriod.YEAR -> ListenBrainzRange.year
        LastfmPeriod.OVERALL -> ListenBrainzRange.all_time
        null -> null
    },
    val name: String = "",
) {

    constructor(lastfmPeriod: LastfmPeriod, name: String = "") : this(
        -1,
        -1,
        lastfmPeriod,
        name = name
    )

    override fun toString() =
        "TimePeriod(start=${Date(start)}, end=${Date(end)}, period=$lastfmPeriod, listenBrainzPeriod=$listenBrainzRange, name=\"$name\")"
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

data class ListeningActivity(
    val timePeriodsToCounts: Map<TimePeriod, Int> = emptyMap(),
    val type: TimePeriodType = TimePeriodType.CUSTOM,
)