package com.arn.scrobble.navigation

import com.arn.scrobble.pref.AppItem


data class TimePickerResult(val hour: Int, val minute: Int)
data class DatePickerResult(val dateMillis: Long)
data class DateRangePickerResult(val startUtc: Long, val endUtc: Long)
data class SelectedPackagesResult(val checked: List<AppItem>, val unchecked: List<AppItem>)