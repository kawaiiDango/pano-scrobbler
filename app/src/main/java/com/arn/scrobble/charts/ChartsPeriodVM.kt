package com.arn.scrobble.charts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.App
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.PageResult
import com.arn.scrobble.api.listenbrainz.ListenBrainz
import com.arn.scrobble.ui.MusicEntryLoaderInput
import com.arn.scrobble.utils.Stuff.firstOrNull
import com.arn.scrobble.utils.Stuff.toBimap
import io.michaelrocks.bimap.HashBiMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlin.math.max


open class ChartsPeriodVM : ViewModel() {

    private val prefs = App.prefs

    private val _periodType = MutableStateFlow(
        if (Scrobblables.current is ListenBrainz)
            TimePeriodType.LISTENBRAINZ
        else runCatching {
            TimePeriodType.valueOf(prefs.lastChartsPeriodType)
        }.getOrDefault(TimePeriodType.CONTINUOUS)
    )
    val periodType = _periodType.asStateFlow()
    private val _input = MutableStateFlow<MusicEntryLoaderInput?>(null)
    val input = _input.asStateFlow()
    val timePeriods = _periodType.combine(
        _input.filterNotNull().take(1)
    ) { periodType, input ->

        if (periodType != TimePeriodType.LISTENBRAINZ)
            prefs.lastChartsPeriodType = periodType.name

        val timePeriodsGenerator =
            TimePeriodsGenerator(
                input.user.registeredTime,
                System.currentTimeMillis(),
                App.context
            )

        val timePeriods = when (periodType) {
            TimePeriodType.CONTINUOUS -> TimePeriodsGenerator.getContinuousPeriods()
                .toBimap()

            TimePeriodType.CUSTOM -> {
                val selPeriod = _selectedPeriod.value
                val start =
                    max(selPeriod.start, input.user.registeredTime)
                val end = selPeriod.end
                listOf(TimePeriod(start, end)).toBimap()
            }

            TimePeriodType.WEEK -> timePeriodsGenerator.weeks.toBimap()
            TimePeriodType.MONTH -> timePeriodsGenerator.months.toBimap()
            TimePeriodType.YEAR -> timePeriodsGenerator.years.toBimap()
            TimePeriodType.LISTENBRAINZ -> timePeriodsGenerator.listenBrainzPeriods.toBimap()
            else -> throw IllegalArgumentException("Unknown period type: $periodType")
        }

        timePeriods
    }
//        .onEach { it.firstOrNull()?.let { _selectedPeriod.emit(it) } }
        .stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            HashBiMap.create<Int, TimePeriod>(emptyMap())
        )


    private val _selectedPeriod = MutableStateFlow(prefs.lastChartsPeriodSelectedJson)
    val selectedPeriod = _selectedPeriod
        .combine(timePeriods.filterNot { it.isEmpty() }) { selectedPeriod, timePeriods ->
            if (selectedPeriod !in timePeriods.inverse) {
                timePeriods.firstOrNull()!! // just select the first

//                val firstPeriod = timePeriods.firstOrNull()!!
//                val lastPeriod = timePeriods.lastOrNull()!!

                // clamp or select first
//                when {
//                    selectedPeriod.start > firstPeriod.start -> firstPeriod
//                    selectedPeriod.start < lastPeriod.start -> lastPeriod
//                    else -> firstPeriod
//                }
            } else
                selectedPeriod
        }.mapLatest {
            prefs.lastChartsPeriodSelectedJson = it
            it
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, prefs.lastChartsPeriodSelectedJson)


    init {
        selectedPeriod
            .combine(_input.filterNotNull()) { period, input ->
                loadCharts(
                    type = input.type,
                    page = input.page,
                    timePeriod = period,
                    username = input.user.name,
                )
            }.launchIn(viewModelScope)
    }

    fun setInput(input: MusicEntryLoaderInput, initial: Boolean = false) {
        if (initial && _input.value == null || !initial)
            _input.value = input
    }

    protected open suspend fun loadCharts(
        type: Int,
        page: Int,
        timePeriod: TimePeriod,
        username: String,
        networkOnly: Boolean = false
    ) {
    }

    fun setPeriodType(type: TimePeriodType) {
        viewModelScope.launch {
            _periodType.emit(type)
        }
    }

    fun setSelectedPeriod(period: TimePeriod) {
        viewModelScope.launch {
            _selectedPeriod.emit(period)
        }
    }

    protected suspend fun emitEntries(
        flow: MutableStateFlow<List<MusicEntry>?>,
        result: Result<PageResult<out MusicEntry>>,
        concat: Boolean
    ) {
        result.onFailure {
            App.globalExceptionFlow.emit(it)
        }

        result.onSuccess {
            if (concat)
                flow.emit((flow.value ?: emptyList()) + it.entries)
            else
                flow.emit(it.entries)
        }
    }
}