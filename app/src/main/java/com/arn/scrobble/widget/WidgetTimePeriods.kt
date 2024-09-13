package com.arn.scrobble.widget

import android.content.Context
import com.arn.scrobble.api.lastfm.Period
import com.arn.scrobble.api.listenbrainz.ListenbrainzRanges
import com.arn.scrobble.charts.AllPeriods
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.charts.TimePeriodsGenerator

class WidgetTimePeriods(private val context: Context) {

    private val timePeriodsGenerator
        get() =
            TimePeriodsGenerator(
                System.currentTimeMillis() - 1,
                System.currentTimeMillis(),
                context
            )

    fun toTimePeriod(period: AllPeriods): TimePeriod {
        return when (period) {
            AllPeriods.WEEK -> TimePeriod(Period.WEEK).apply {
                tag = ListenbrainzRanges.this_week.name
            }

            AllPeriods.MONTH -> TimePeriod(Period.MONTH).apply {
                tag = ListenbrainzRanges.this_month.name
            }

            AllPeriods.QUARTER -> TimePeriod(Period.QUARTER).apply {
                tag = ListenbrainzRanges.quarter.name
            }

            AllPeriods.HALF_YEAR -> TimePeriod(Period.HALF_YEAR).apply {
                tag = ListenbrainzRanges.half_yearly.name
            }

            AllPeriods.YEAR -> TimePeriod(Period.YEAR).apply {
                tag = ListenbrainzRanges.this_year.name
            }

            AllPeriods.THIS_WEEK -> timePeriodsGenerator.weeks[0].apply {
                tag = ListenbrainzRanges.this_week.name
            }

            AllPeriods.THIS_MONTH -> timePeriodsGenerator.months[0].apply {
                tag = ListenbrainzRanges.this_month.name
            }

            AllPeriods.THIS_YEAR -> timePeriodsGenerator.years[0].apply {
                tag = ListenbrainzRanges.this_year.name
            }

            AllPeriods.ALL_TIME -> TimePeriod(Period.OVERALL).apply {
                tag = ListenbrainzRanges.all_time.name
            }
        }
    }
}