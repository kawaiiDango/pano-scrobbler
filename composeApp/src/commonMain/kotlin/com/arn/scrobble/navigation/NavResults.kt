package com.arn.scrobble.navigation

import androidx.compose.ui.unit.IntOffset
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.charts.TimePeriodType
import com.arn.scrobble.pref.AppItem


data class TimePickerResult(val hour: Int, val minute: Int)
data class DatePickerResult(val timeUtc: Long)
data class DateRangePickerResult(val startUtc: Long, val endUtc: Long)
data class SelectedPackagesResult(val checked: List<AppItem>, val unchecked: List<AppItem>)
data object FabClickedResult
data class SubTabClickedResult(val id: Int)
data class PullToRefreshResult(val tab: PanoTab)
data object TimePeriodTypeClickedResult
data class TimePeriodClickedResult(val timePeriod: TimePeriod, val selectedPeriodOffset: IntOffset)
data class TimePeriodDataResult(
    val typeSelectorShown: Boolean?,
    val periodType: TimePeriodType,
    val timePeriodsList: List<TimePeriod>,
    val selectedPeriod: TimePeriod?,
    val enabled: Boolean,
)