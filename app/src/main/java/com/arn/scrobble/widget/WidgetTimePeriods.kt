package com.arn.scrobble.widget

import android.content.Context
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.charts.TimePeriodType
import com.arn.scrobble.charts.TimePeriodsGenerator
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
        Period.WEEK.string to TimePeriod(context, Period.WEEK),
        Period.ONE_MONTH.string to TimePeriod(context, Period.ONE_MONTH),
        Period.THREE_MONTHS.string to TimePeriod(context, Period.THREE_MONTHS),
        Period.SIX_MONTHS.string to TimePeriod(context, Period.SIX_MONTHS),
        Period.TWELVE_MONTHS.string to TimePeriod(context, Period.TWELVE_MONTHS),
        TimePeriodType.WEEK.toString() to timePeriodsGenerator.weeks[0],
        TimePeriodType.MONTH.toString() to timePeriodsGenerator.months[0],
        TimePeriodType.YEAR.toString() to timePeriodsGenerator.years[0],
        Period.OVERALL.string to TimePeriod(context, Period.OVERALL)
    )
}