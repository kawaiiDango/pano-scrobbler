package com.arn.scrobble.charts

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.LFMRequester
import com.arn.scrobble.Stuff.setMidnight
import com.arn.scrobble.Stuff.setUserFirstDayOfWeek
import com.arn.scrobble.charts.TimePeriodsGenerator.Companion.toDuration
import com.arn.scrobble.charts.TimePeriodsGenerator.Companion.toTimePeriod
import com.arn.scrobble.scrobbleable.ListenBrainz
import com.arn.scrobble.scrobbleable.Scrobblables
import com.hadilq.liveevent.LiveEvent
import de.umass.lastfm.MusicEntry
import de.umass.lastfm.PaginatedResult
import de.umass.lastfm.Period
import io.michaelrocks.bimap.BiMap
import java.util.Calendar


class ChartsVM : ViewModel() {
    val chartsData by lazy { mutableListOf<MusicEntry>() }
    val chartsReceiver by lazy { LiveEvent<PaginatedResult<MusicEntry>>() }
    val listReceiver by lazy { LiveEvent<List<MusicEntry>>() }
    private var lastChartsTask: LFMRequester? = null
    val periodType = MutableLiveData<TimePeriodType>()
    val timePeriods = MutableLiveData<BiMap<Int, TimePeriod>>()
    val selectedPeriod = MutableLiveData<TimePeriod>()
    var username: String? = null
    var page = 1
    var totalCount = 0
    var reachedEnd = false
    var chartsType = 1

    fun loadCharts(page: Int = 1, networkOnly: Boolean = false) {
        val selectedPeriodValue = selectedPeriod.value ?: return
        var prevPeriod: TimePeriod? = null
        this.page = page

        if (periodType.value != TimePeriodType.CONTINUOUS) {
            if (Scrobblables.current !is ListenBrainz) {
                this.page = 1
                reachedEnd = true
            }
            timePeriods.value?.inverse?.get(selectedPeriodValue)?.let { idx ->
                prevPeriod = timePeriods.value?.get(idx + 1)
            }
        } else {
            if (selectedPeriodValue.period != null && selectedPeriodValue.period != Period.OVERALL) {
                val cal = Calendar.getInstance()
                cal.setUserFirstDayOfWeek()
                cal.setMidnight()
                val duration = selectedPeriodValue.period.toDuration(endTime = cal.timeInMillis)
                prevPeriod =
                    selectedPeriodValue.period.toTimePeriod(endTime = cal.timeInMillis - duration)
            }
        }
        lastChartsTask?.cancel()
        lastChartsTask =
            LFMRequester(viewModelScope, chartsReceiver).apply {
                getChartsWithStonks(
                    type = chartsType,
                    timePeriod = selectedPeriodValue,
                    prevTimePeriod = prevPeriod,
                    page = page,
                    usernamep = username,
                    networkOnly = networkOnly
                )
            }
    }

    fun hasLoaded() = lastChartsTask?.isCompleted == true
}