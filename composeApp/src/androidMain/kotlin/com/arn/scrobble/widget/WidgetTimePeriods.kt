package com.arn.scrobble.widget

import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.LastfmPeriod
import com.arn.scrobble.api.listenbrainz.ListenbrainzRanges
import com.arn.scrobble.charts.AllPeriods
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.charts.TimePeriodsGenerator
import com.arn.scrobble.utils.AndroidStuff

class WidgetTimePeriods {

    private val timePeriodsGenerator
        get() =
            TimePeriodsGenerator(
                System.currentTimeMillis() - 1,
                System.currentTimeMillis(),
                generateFormattedStrings = true
            )

    val context = AndroidStuff.application

    fun toTimePeriod(period: AllPeriods): TimePeriod {
        return when (period) {
            AllPeriods.WEEK -> TimePeriod(
                LastfmPeriod.WEEK,
                name = context.resources.getQuantityString(R.plurals.num_weeks, 1, 1)
            ).apply {
                tag = ListenbrainzRanges.this_week.name
            }

            AllPeriods.MONTH -> TimePeriod(
                LastfmPeriod.MONTH,
                name = context.resources.getQuantityString(R.plurals.num_months, 1, 1)
            ).apply {
                tag = ListenbrainzRanges.this_month.name
            }

            AllPeriods.QUARTER -> TimePeriod(
                LastfmPeriod.QUARTER,
                name = context.resources.getQuantityString(R.plurals.num_months, 3, 3)
            ).apply {
                tag = ListenbrainzRanges.quarter.name
            }

            AllPeriods.HALF_YEAR -> TimePeriod(
                LastfmPeriod.HALF_YEAR,
                name = context.resources.getQuantityString(R.plurals.num_months, 6, 6)
            ).apply {
                tag = ListenbrainzRanges.half_yearly.name
            }

            AllPeriods.YEAR -> TimePeriod(
                LastfmPeriod.YEAR,
                name = context.resources.getQuantityString(R.plurals.num_years, 1, 1)
            ).apply {
                tag = ListenbrainzRanges.this_year.name
            }

            AllPeriods.THIS_WEEK -> timePeriodsGenerator.weeks()[0].apply {
                tag = ListenbrainzRanges.this_week.name
            }

            AllPeriods.THIS_MONTH -> timePeriodsGenerator.months()[0].apply {
                tag = ListenbrainzRanges.this_month.name
            }

            AllPeriods.THIS_YEAR -> timePeriodsGenerator.years()[0].apply {
                tag = ListenbrainzRanges.this_year.name
            }

            AllPeriods.ALL_TIME -> TimePeriod(
                LastfmPeriod.OVERALL,
                name = context.getString(R.string.charts_overall)
            ).apply {
                tag = ListenbrainzRanges.all_time.name
            }
        }
    }
}