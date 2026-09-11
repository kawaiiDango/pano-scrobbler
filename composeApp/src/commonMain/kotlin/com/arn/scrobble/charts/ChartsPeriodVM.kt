package com.arn.scrobble.charts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.api.lastfm.LastfmPeriod
import com.arn.scrobble.api.listenbrainz.ListenBrainzRange
import com.arn.scrobble.charts.TimePeriodsGenerator.Companion.toTimePeriod
import com.arn.scrobble.utils.PanoTimeFormatter
import com.arn.scrobble.utils.PlatformStuff
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlin.math.max


class ChartsPeriodVM : ViewModel() {
    private var digestPeriod: LastfmPeriod? = null
    private val _periodTypeToRegisteredTime = MutableStateFlow<Pair<TimePeriodType, Long>?>(null)

    val periodTypeToRegisteredTime = _periodTypeToRegisteredTime.asStateFlow()
    private val _refreshCount = MutableStateFlow(0)
    val refreshCount = _refreshCount.asStateFlow()

    private val _customPeriodInput =
        MutableStateFlow(TimePeriod(1577836800000L, 1609459200000L)) // 2020

    val timePeriods: StateFlow<List<TimePeriod>> = combine(
        _periodTypeToRegisteredTime
            .filterNotNull(),
        _customPeriodInput,
        PlatformStuff.mainPrefs.data.map { it.firstDayOfWeek }.distinctUntilChanged(),
    ) { periodTypeToRegisteredTime, customPeriod, firstDayOfWeek ->

        val (periodType, registeredTime) = periodTypeToRegisteredTime

        val timePeriodsGenerator =
            TimePeriodsGenerator(
                registeredTime,
                System.currentTimeMillis(),
                firstDayOfWeek,
                generateFormattedStrings = true,
            )

        val timePeriods = when (periodType) {
            TimePeriodType.CONTINUOUS -> TimePeriodsGenerator.getContinuousPeriods()

            TimePeriodType.CUSTOM -> {
                val selPeriod = customPeriod
                val start = max(selPeriod.start, registeredTime)
                val end = selPeriod.end
                listOf(
                    TimePeriod(
                        start, end,
                        name = PanoTimeFormatter.dateRange(start, end)
                    )
                )
            }

            TimePeriodType.WEEK -> timePeriodsGenerator.weeks()

            TimePeriodType.MONTH -> timePeriodsGenerator.months()

            TimePeriodType.YEAR -> timePeriodsGenerator.years()
            TimePeriodType.LISTENBRAINZ -> timePeriodsGenerator.listenBrainz()
            else -> throw IllegalArgumentException("Unknown period type: $periodType")
        }

        timePeriods
    }
        .stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            emptyList()
        )

    private val _selectedPeriod = MutableStateFlow<TimePeriod?>(null)

    val selectedPeriod = _selectedPeriod
        .combine(timePeriods.filterNot { it.isEmpty() }) { selectedPeriod, timePeriods ->
            val prevDigestPeriod = digestPeriod

            val periodType = _periodTypeToRegisteredTime.value?.first

            if (prevDigestPeriod != null && periodType == TimePeriodType.CONTINUOUS) {
                digestPeriod = null
                timePeriods.firstNotNullOfOrNull {
                    if (it.lastfmPeriod == prevDigestPeriod) it else null
                }
            } else if (prevDigestPeriod != null && periodType == TimePeriodType.LISTENBRAINZ) {
                digestPeriod = null
                timePeriods.firstNotNullOfOrNull {
                    val listenBrainzPeriod = when (prevDigestPeriod) {
                        LastfmPeriod.WEEK -> ListenBrainzRange.week
                        LastfmPeriod.MONTH -> ListenBrainzRange.month
                        LastfmPeriod.YEAR -> ListenBrainzRange.year
                        else -> null
                    }

                    if (it.listenBrainzRange == listenBrainzPeriod) it else null
                }
            } else if (
                selectedPeriod != null &&
                (periodType == TimePeriodType.WEEK ||
                        periodType == TimePeriodType.MONTH ||
                        periodType == TimePeriodType.YEAR ||
                        periodType == TimePeriodType.CONTINUOUS && selectedPeriod.lastfmPeriod == null)
            ) {
                val selectedPeriodStart =
                    selectedPeriod.lastfmPeriod?.toTimePeriod()?.start ?: selectedPeriod.start
                val insertionIdx = timePeriods.binarySearch { period ->
                    // list is descending, compare in reverse
                    val periodStart = period.lastfmPeriod?.toTimePeriod()?.start ?: period.start
                    selectedPeriodStart.compareTo(periodStart)
                }

                if (insertionIdx >= 0) {
                    timePeriods[insertionIdx]
                } else {
                    val hi = -insertionIdx - 1
                    val lo = hi - 1

                    timePeriods.getOrNull(hi)
                        ?: timePeriods.getOrNull(lo)
                        ?: timePeriods.firstOrNull()
                }
            } else if (selectedPeriod !in timePeriods) {
                timePeriods.firstOrNull() // just select the first
            } else {
                selectedPeriod
            }
        }
        .onEach { period ->
            // mark as non-refresh request
            _refreshCount.value = 0
            val periodType = _periodTypeToRegisteredTime.value?.first


            if (periodType != TimePeriodType.LISTENBRAINZ && periodType != null && period != null) {
                PlatformStuff.mainPrefs.updateData {
                    it.copy(
                        lastChartsPeriodType = periodType,
                        lastChartsLastfmPeriod = period,
                        lastChartsCustomPeriod = _customPeriodInput.value
                    )
                }
            } else if (periodType == TimePeriodType.LISTENBRAINZ && period != null) {
                PlatformStuff.mainPrefs.updateData {
                    it.copy(
                        lastChartsListenBrainzPeriod = period
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun setPeriodTypeAndRegisteredTime(type: TimePeriodType, registeredTime: Long) {
        _periodTypeToRegisteredTime.value = type to registeredTime
    }

    fun setSelectedPeriod(period: TimePeriod) {
        _selectedPeriod.value = period
    }

    fun setCustomPeriodInput(period: TimePeriod) {
        _customPeriodInput.value = period
    }

    fun setDigestPeriod(period: LastfmPeriod) {
        digestPeriod = period
    }

    fun refresh() {
        _refreshCount.value += 1
    }
}