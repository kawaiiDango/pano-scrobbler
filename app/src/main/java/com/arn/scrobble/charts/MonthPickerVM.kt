package com.arn.scrobble.charts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.michaelrocks.bimap.BiMap


class MonthPickerVM(app: Application) : AndroidViewModel(app) {
    lateinit var timePeriods: BiMap<Int, TimePeriod>
    var selectedMonth = 0
    var selectedYear = 0
    lateinit var callback: (TimePeriod) -> Unit
}