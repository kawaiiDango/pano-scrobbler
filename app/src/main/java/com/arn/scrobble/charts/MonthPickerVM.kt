package com.arn.scrobble.charts

import androidx.lifecycle.ViewModel
import io.michaelrocks.bimap.BiMap


class MonthPickerVM : ViewModel() {
    lateinit var timePeriods: BiMap<Int, TimePeriod>
    var selectedMonth = 0
    var selectedYear = 0
    lateinit var callback: (TimePeriod) -> Unit
}