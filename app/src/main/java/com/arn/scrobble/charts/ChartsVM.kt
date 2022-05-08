package com.arn.scrobble.charts

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.LFMRequester
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.setMidnight
import com.arn.scrobble.charts.TimePeriodsGenerator.Companion.toDuration
import com.arn.scrobble.charts.TimePeriodsGenerator.Companion.toTimePeriod
import com.hadilq.liveevent.LiveEvent
import de.umass.lastfm.MusicEntry
import de.umass.lastfm.PaginatedResult
import de.umass.lastfm.Period
import io.michaelrocks.bimap.BiMap
import java.util.*


class ChartsVM(application: Application) : AndroidViewModel(application) {
    val chartsData by lazy { mutableListOf<MusicEntry>() }
    val chartsReceiver by lazy { LiveEvent<PaginatedResult<MusicEntry>>() }
    val listReceiver by lazy { LiveEvent<List<MusicEntry>>() }
    val periodCountReceiver by lazy { MutableLiveData<Map<TimePeriod, Int>>() }
    val tagCloudReceiver by lazy { MutableLiveData<Map<String, Float>>() }
    val tagCloudError by lazy { LiveEvent<Throwable>() }
    val tagCloudRefresh by lazy { LiveEvent<Unit>() }
    var periodCountRequested = false
    var tagCloudRequested = false
    val tagCloudProgressLd by lazy { MutableLiveData<Double>() }
    val scrobbleCountHeader = MutableLiveData(application.getString(R.string.charts))
    private val lastChartsTasks = mutableMapOf<Int, LFMRequester>()
    val periodType = MutableLiveData<TimePeriodType>()
    val timePeriods = MutableLiveData<BiMap<Int, TimePeriod>>()
    val selectedPeriod = MutableLiveData<TimePeriod>()
    var tagCloudBitmap: Pair<Int, Bitmap?>? = null
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
            this.page = 1
            reachedEnd = true
            timePeriods.value?.inverse?.get(selectedPeriodValue)?.let { idx ->
                prevPeriod = timePeriods.value?.get(idx + 1)
            }
        } else {
            if (selectedPeriodValue.period != null && selectedPeriodValue.period != Period.OVERALL) {
                val cal = Calendar.getInstance()
                cal.setMidnight()
                val duration = selectedPeriodValue.period.toDuration(endTime = cal.timeInMillis)
                prevPeriod =
                    selectedPeriodValue.period.toTimePeriod(endTime = cal.timeInMillis - duration)
            }
        }
        lastChartsTasks[chartsType]?.cancel()
        lastChartsTasks[chartsType] =
            LFMRequester(getApplication(), viewModelScope, chartsReceiver).apply {
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

    fun loadScrobbleCounts(periods: List<TimePeriod>) {
        lastChartsTasks[Stuff.TYPE_SC]?.cancel()
        lastChartsTasks[Stuff.TYPE_SC] =
            LFMRequester(getApplication(), viewModelScope, periodCountReceiver).apply {
                getScrobbleCounts(periods, username)
            }
    }

    fun loadTagCloud(musicEntries: List<MusicEntry>) {
        if (!BuildConfig.DEBUG)
            return
        tagCloudRequested = true
        lastChartsTasks[Stuff.TYPE_TAG_CLOUD]?.cancel()
        lastChartsTasks[Stuff.TYPE_TAG_CLOUD] =
            LFMRequester(
                getApplication(), viewModelScope, liveData = tagCloudReceiver,
                errorLiveData = tagCloudError
            ).apply {
                getTagCloud(musicEntries, tagCloudProgressLd)
            }
    }

    fun hasLoaded(type: Int) = lastChartsTasks[type]?.isCompleted == true

    fun resetRequestedState() {
        periodCountRequested = false
        tagCloudRequested = false
        lastChartsTasks[Stuff.TYPE_TAG_CLOUD]?.cancel()
        lastChartsTasks[Stuff.TYPE_SC]?.cancel()
    }
}