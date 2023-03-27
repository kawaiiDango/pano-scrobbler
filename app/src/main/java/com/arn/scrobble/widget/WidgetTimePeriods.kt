package com.arn.scrobble.widget

import android.content.Context
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.charts.TimePeriodType
import com.arn.scrobble.charts.TimePeriodsGenerator
import com.arn.scrobble.scrobbleable.ListenbrainzRanges
import de.umass.lastfm.Period

class WidgetTimePeriods(private val context: Context) {

    private val timePeriodsGenerator
        get() =
            TimePeriodsGenerator(
                System.currentTimeMillis() - 1,
                System.currentTimeMillis(),
                context
            )

    val periodsMap = mapOf(
        Period.WEEK.string to TimePeriod(Period.WEEK).apply { tag = ListenbrainzRanges.this_week.name },
        Period.ONE_MONTH.string to TimePeriod(Period.ONE_MONTH).apply { tag = ListenbrainzRanges.this_month.name },
        Period.THREE_MONTHS.string to TimePeriod(Period.THREE_MONTHS).apply { tag = ListenbrainzRanges.quarter.name },
        Period.SIX_MONTHS.string to TimePeriod(Period.SIX_MONTHS).apply { tag = ListenbrainzRanges.half_yearly.name },
        Period.TWELVE_MONTHS.string to TimePeriod(Period.TWELVE_MONTHS).apply { tag = ListenbrainzRanges.this_year.name },
        TimePeriodType.WEEK.toString() to timePeriodsGenerator.weeks[0].apply { tag = ListenbrainzRanges.this_week.name },
        TimePeriodType.MONTH.toString() to timePeriodsGenerator.months[0].apply { tag = ListenbrainzRanges.this_month.name },
        TimePeriodType.YEAR.toString() to timePeriodsGenerator.years[0].apply { tag = ListenbrainzRanges.this_year.name },
        Period.OVERALL.string to TimePeriod(Period.OVERALL).apply { tag = ListenbrainzRanges.all_time.name }
    )
}