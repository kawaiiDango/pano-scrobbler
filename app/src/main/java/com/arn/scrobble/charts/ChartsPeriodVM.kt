package com.arn.scrobble.charts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.PageResult
import com.arn.scrobble.api.listenbrainz.ListenBrainz
import com.arn.scrobble.main.App
import com.arn.scrobble.ui.MusicEntryLoaderInput
import com.arn.scrobble.utils.Stuff.doOnSuccessLoggingFaliure
import com.arn.scrobble.utils.Stuff.toInverseMap
import kotlinx.coroutines.flow.MutableSharedFlow
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

    val prefs = App.prefs

    private val initialPeriodType by lazy {
        if (Scrobblables.current is ListenBrainz)
            TimePeriodType.LISTENBRAINZ
        else runCatching {
            TimePeriodType.valueOf(prefs.lastChartsPeriodType)
        }.getOrDefault(TimePeriodType.CONTINUOUS)
    }

    private val _periodType = MutableSharedFlow<TimePeriodType>()
    val periodType = _periodType.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        initialPeriodType
    )
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
                .toInverseMap()

            TimePeriodType.CUSTOM -> {
                val selPeriod = _selectedPeriod.value
                val start =
                    max(selPeriod.start, input.user.registeredTime)
                val end = selPeriod.end
                listOf(TimePeriod(start, end)).toInverseMap()
            }

            TimePeriodType.WEEK -> timePeriodsGenerator.weeks.toInverseMap()

            TimePeriodType.MONTH -> timePeriodsGenerator.months.toInverseMap()

            TimePeriodType.YEAR -> timePeriodsGenerator.years.toInverseMap()
            TimePeriodType.LISTENBRAINZ -> timePeriodsGenerator.listenBrainzPeriods.toInverseMap()
            else -> throw IllegalArgumentException("Unknown period type: $periodType")
        }

        timePeriods
    }
//        .onEach { it.firstOrNull()?.let { _selectedPeriod.emit(it) } }
        .stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            emptyMap()
        )


    private val _selectedPeriod = MutableStateFlow(prefs.lastChartsPeriodSelectedJson)
    val selectedPeriod = _selectedPeriod
        .combine(timePeriods.filterNot { it.isEmpty() }) { selectedPeriod, timePeriods ->
            if (selectedPeriod !in timePeriods) {
                timePeriods.firstNotNullOf { (k, v) -> k } // just select the first

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

        viewModelScope.launch {
            _periodType.emit(initialPeriodType)
        }

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
        _selectedPeriod.value = period
    }

    protected suspend fun emitEntries(
        flow: MutableStateFlow<List<MusicEntry>?>,
        result: Result<PageResult<out MusicEntry>>,
        concat: Boolean
    ) {
        result.doOnSuccessLoggingFaliure {
            if (concat)
                flow.emit((flow.value ?: emptyList()) + it.entries)
            else
                flow.emit(it.entries)
        }
    }
}