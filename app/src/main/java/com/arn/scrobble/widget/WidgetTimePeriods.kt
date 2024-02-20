package com.arn.scrobble.widget

import android.content.Context
import com.arn.scrobble.api.lastfm.Period
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.charts.TimePeriodType
import com.arn.scrobble.charts.TimePeriodsGenerator
import com.arn.scrobble.api.listenbrainz.ListenbrainzRanges

class WidgetTimePeriods(private val context: Context) {

    private val timePeriodsGenerator
        get() =
            TimePeriodsGenerator(
                System.currentTimeMillis() - 1,
                System.currentTimeMillis(),
                context
            )

    val periodsMap = mapOf(
        Period.WEEK.value to TimePeriod(Period.WEEK).apply {
            tag = ListenbrainzRanges.this_week.name
        },
        Period.MONTH.value to TimePeriod(Period.MONTH).apply {
            tag = ListenbrainzRanges.this_month.name
        },
        Period.QUARTER.value to TimePeriod(Period.QUARTER).apply {
            tag = ListenbrainzRanges.quarter.name
        },
        Period.HALF_YEAR.value to TimePeriod(Period.HALF_YEAR).apply {
            tag = ListenbrainzRanges.half_yearly.name
        },
        Period.YEAR.value to TimePeriod(Period.YEAR).apply {
            tag = ListenbrainzRanges.this_year.name
        },
        TimePeriodType.WEEK.toString() to timePeriodsGenerator.weeks[0].apply {
            tag = ListenbrainzRanges.this_week.name
        },
        TimePeriodType.MONTH.toString() to timePeriodsGenerator.months[0].apply {
            tag = ListenbrainzRanges.this_month.name
        },
        TimePeriodType.YEAR.toString() to timePeriodsGenerator.years[0].apply {
            tag = ListenbrainzRanges.this_year.name
        },
        Period.OVERALL.value to TimePeriod(Period.OVERALL).apply {
            tag = ListenbrainzRanges.all_time.name
        }
    )
}