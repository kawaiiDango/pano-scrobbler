package com.arn.scrobble.charts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.PageResult
import com.arn.scrobble.api.listenbrainz.ListenBrainz
import com.arn.scrobble.ui.MusicEntryLoaderInput
import com.arn.scrobble.utils.Stuff.doOnSuccessLoggingFaliure
import com.arn.scrobble.utils.Stuff.toInverseMap
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.max


open class ChartsPeriodVM : ViewModel() {

    protected val mainPrefs = PlatformStuff.mainPrefs

    private val _periodType = if (Scrobblables.current.value is ListenBrainz) {
        MutableStateFlow(TimePeriodType.LISTENBRAINZ)
    } else {
        MutableStateFlow(runBlocking {
            mainPrefs.data.map { it.lastChartsPeriodType }.first()
        }
        )
//        _periodType.stateIn(
//            viewModelScope,
//            SharingStarted.Lazily,
//            if (Scrobblables.current.value is ListenBrainz)
//                TimePeriodType.LISTENBRAINZ
//            else
//                TimePeriodType.CONTINUOUS
//        )
    }

    val periodType = _periodType.asStateFlow()

    private val _input = MutableStateFlow<MusicEntryLoaderInput?>(null)
    val input = _input.asStateFlow()
    val timePeriods: StateFlow<Map<TimePeriod, Int>> = periodType
        .filterNotNull()
        .combine(
            _input.filterNotNull().take(1)
        ) { periodType, input ->

            val timePeriodsGenerator =
                TimePeriodsGenerator(
                    input.user.registeredTime,
                    System.currentTimeMillis(),
                    PlatformStuff.application
                )

            val timePeriods = when (periodType) {
                TimePeriodType.CONTINUOUS -> TimePeriodsGenerator.getContinuousPeriods()
                    .toInverseMap()

                TimePeriodType.CUSTOM -> {
                    val selPeriod = selectedPeriod.value ?: return@combine emptyMap()
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

    // TODO make async
    private val _selectedPeriod = MutableStateFlow(
        runBlocking {
            mainPrefs.data.map { it.lastChartsPeriodSelected }.first()
        }
    )
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
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)


    init {
        selectedPeriod
            .filterNotNull()
            .debounce(100)
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
        _periodType.value = type
//            mainPrefs.updateData { it.copy(lastChartsPeriodType = type) }
    }

    fun setSelectedPeriod(period: TimePeriod) {
        _selectedPeriod.value = period
        viewModelScope.launch {
//            mainPrefs.updateData { it.copy(lastChartsPeriodSelected = period) }
            _input.value = _input.value?.copy(page = 1)
        }
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

    override fun onCleared() {
        // viewModelScope is cancelled
        val periodType = periodType.value
        val selectedPeriod = selectedPeriod.value

        if (periodType != TimePeriodType.LISTENBRAINZ && selectedPeriod != null) {
            GlobalScope.launch {
                mainPrefs.updateData {
                    it.copy(
                        lastChartsPeriodType = periodType,
                        lastChartsPeriodSelected = selectedPeriod
                    )
                }
            }
        }
    }
}