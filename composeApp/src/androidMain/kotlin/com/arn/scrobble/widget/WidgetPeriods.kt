package com.arn.scrobble.widget

import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.LastfmPeriod
import com.arn.scrobble.api.listenbrainz.ListenBrainzRange
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.charts.TimePeriodsGenerator
import com.arn.scrobble.utils.AndroidStuff


enum class WidgetPeriods {
    THIS_WEEK,
    WEEK,
    THIS_MONTH,
    MONTH,
    QUARTER,
    HALF_YEAR,
    THIS_YEAR,
    YEAR,
    ALL_TIME;

    fun toTimePeriod(firstDayOfWeek: Int): TimePeriod {
        val context = AndroidStuff.applicationContext
        val timePeriodsGenerator by lazy {
            TimePeriodsGenerator(
                System.currentTimeMillis() - 1,
                System.currentTimeMillis(),
                firstDayOfWeek,
                generateFormattedStrings = true
            )
        }

        return when (this) {
            WEEK -> TimePeriod(
                LastfmPeriod.WEEK,
                name = context.resources.getQuantityString(R.plurals.num_weeks, 1, 1)
            )

            MONTH -> TimePeriod(
                LastfmPeriod.MONTH,
                name = context.resources.getQuantityString(R.plurals.num_months, 1, 1)
            )

            QUARTER -> TimePeriod(
                LastfmPeriod.QUARTER,
                name = context.resources.getQuantityString(R.plurals.num_months, 3, 3)
            )

            HALF_YEAR -> TimePeriod(
                LastfmPeriod.HALF_YEAR,
                name = context.resources.getQuantityString(R.plurals.num_months, 6, 6)
            )

            YEAR -> TimePeriod(
                LastfmPeriod.YEAR,
                name = context.resources.getQuantityString(R.plurals.num_years, 1, 1)
            )

            THIS_WEEK -> timePeriodsGenerator.weeks()[0].copy(
                listenBrainzRange = ListenBrainzRange.this_week
            )

            THIS_MONTH -> timePeriodsGenerator.months()[0].copy(
                listenBrainzRange = ListenBrainzRange.this_month
            )

            THIS_YEAR -> timePeriodsGenerator.years()[0].copy(
                listenBrainzRange = ListenBrainzRange.this_year
            )

            ALL_TIME -> TimePeriod(
                LastfmPeriod.OVERALL,
                name = context.getString(R.string.charts_overall)
            )
        }
    }
}