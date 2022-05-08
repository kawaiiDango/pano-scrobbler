package com.arn.scrobble.charts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.LFMRequester
import com.arn.scrobble.pref.HistoryPref
import de.umass.lastfm.MusicEntry
import io.michaelrocks.bimap.BiMap


class MonthPickerVM(app: Application) : AndroidViewModel(app) {
    lateinit var timePeriods: BiMap<Int, TimePeriod>
    var selectedMonth = 0
    var selectedYear = 0
    lateinit var callback: (TimePeriod) -> Unit
}