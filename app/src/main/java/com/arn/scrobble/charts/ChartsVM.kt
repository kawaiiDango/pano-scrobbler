package com.arn.scrobble.charts

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.App
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.LFMRequester
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.setMidnight
import com.arn.scrobble.charts.TimePeriodsGenerator.Companion.toDuration
import com.arn.scrobble.charts.TimePeriodsGenerator.Companion.toTimePeriod
import com.arn.scrobble.friends.UserSerializable
import com.arn.scrobble.scrobbleable.ListenBrainz
import com.arn.scrobble.scrobbleable.Scrobblables
import com.hadilq.liveevent.LiveEvent
import de.umass.lastfm.MusicEntry
import de.umass.lastfm.PaginatedResult
import de.umass.lastfm.Period
import io.michaelrocks.bimap.BiMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Calendar


class ChartsVM : ViewModel() {
    val chartsData by lazy { mutableListOf<MusicEntry>() }
    val chartsReceiver by lazy { LiveEvent<PaginatedResult<MusicEntry>>() }
    val listReceiver by lazy { LiveEvent<List<MusicEntry>>() }
    val listeningActivity by lazy { MutableLiveData<Map<TimePeriod, Int>>() }
    val tagCloud by lazy { MutableLiveData<Map<String, Float>>() }
    val tagCloudError by lazy { LiveEvent<Throwable>() }
    val tagCloudRefresh by lazy { LiveEvent<Unit>() }
    var periodCountRequested = false
    var tagCloudRequested = false
    val tagCloudProgressLd by lazy { MutableLiveData<Double>() }
    val scrobbleCountHeader = MutableLiveData(App.context.getString(R.string.listening_activity))
    private var lastListeningActivityJob: Job? = null
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
                cal.setMidnight()
                val duration = selectedPeriodValue.period.toDuration(endTime = cal.timeInMillis)
                prevPeriod =
                    selectedPeriodValue.period.toTimePeriod(endTime = cal.timeInMillis - duration)
            }
        }
        lastChartsTasks[chartsType]?.cancel()
        lastChartsTasks[chartsType] =
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

    fun loadListeningActivity(user: UserSerializable) {
        lastListeningActivityJob?.cancel()
        lastListeningActivityJob =
            viewModelScope.launch(Dispatchers.IO + LFMRequester.ExceptionNotifier(timberLog = false)) {
                listeningActivity.postValue(
                    Scrobblables.current?.getListeningActivity(
                        selectedPeriod.value ?: return@launch, user
                    )
                )
            }
    }

    fun loadTagCloud(musicEntries: List<MusicEntry>) {
        if (!BuildConfig.DEBUG)
            return
        tagCloudRequested = true
        lastChartsTasks[Stuff.TYPE_TAG_CLOUD]?.cancel()
        lastChartsTasks[Stuff.TYPE_TAG_CLOUD] =
            LFMRequester(
                viewModelScope, liveData = tagCloud,
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