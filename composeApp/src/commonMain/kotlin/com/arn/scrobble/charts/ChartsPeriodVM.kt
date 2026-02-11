package com.arn.scrobble.charts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.LastfmPeriod
import com.arn.scrobble.api.listenbrainz.ListenBrainzRange
import com.arn.scrobble.utils.PanoTimeFormatter
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff.toInverseMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlin.math.max


class ChartsPeriodVM(
    user: UserCached,
) : ViewModel() {
    private var digestPeriod: LastfmPeriod? = null
    private val _periodType = MutableStateFlow<TimePeriodType?>(null)

    val periodType = _periodType.asStateFlow()
    private val _refreshCount = MutableStateFlow(0)
    val refreshCount = _refreshCount.asStateFlow()

    private val _customPeriodInput =
        MutableStateFlow(TimePeriod(1577836800000L, 1609459200000L)) // 2020

    val timePeriods: StateFlow<Map<TimePeriod, Int>> = periodType
        .filterNotNull()
        .combine(
            _customPeriodInput
        ) { periodType, customPeriod ->
            periodType to customPeriod
        }
        .combine(
            PlatformStuff.mainPrefs.data.map { it.firstDayOfWeek },
        )
        { (periodType, customPeriod), firstDayOfWeek ->

            val timePeriodsGenerator =
                TimePeriodsGenerator(
                    user.registeredTime,
                    System.currentTimeMillis(),
                    firstDayOfWeek,
                    generateFormattedStrings = true,
                )

            val timePeriods = when (periodType) {
                TimePeriodType.CONTINUOUS -> TimePeriodsGenerator.getContinuousPeriods()
                    .toInverseMap()

                TimePeriodType.CUSTOM -> {
                    val selPeriod = customPeriod
                    val start = max(selPeriod.start, user.registeredTime)
                    val end = selPeriod.end
                    listOf(
                        TimePeriod(
                            start, end,
                            name = PanoTimeFormatter.dateRange(start, end)
                        )
                    ).toInverseMap()
                }

                TimePeriodType.WEEK -> timePeriodsGenerator.weeks().toInverseMap()

                TimePeriodType.MONTH -> timePeriodsGenerator.months().toInverseMap()

                TimePeriodType.YEAR -> timePeriodsGenerator.years().toInverseMap()
                TimePeriodType.LISTENBRAINZ -> timePeriodsGenerator.listenBrainz().toInverseMap()
                else -> throw IllegalArgumentException("Unknown period type: $periodType")
            }

            timePeriods
        }
        .stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            emptyMap()
        )

    private val _selectedPeriod = MutableStateFlow<TimePeriod?>(null)

    val selectedPeriod = _selectedPeriod
        .combine(timePeriods.filterNot { it.isEmpty() }) { selectedPeriod, timePeriods ->
            val prevDigestPeriod = digestPeriod

            if (prevDigestPeriod != null && periodType.value == TimePeriodType.CONTINUOUS) {
                digestPeriod = null
                timePeriods.firstNotNullOfOrNull { (k, v) ->
                    if (k.lastfmPeriod == prevDigestPeriod) k else null
                }
            } else if (prevDigestPeriod != null && periodType.value == TimePeriodType.LISTENBRAINZ) {
                digestPeriod = null
                timePeriods.firstNotNullOfOrNull { (k, v) ->
                    val listenBrainzPeriod = when (prevDigestPeriod) {
                        LastfmPeriod.WEEK -> ListenBrainzRange.week
                        LastfmPeriod.MONTH -> ListenBrainzRange.month
                        LastfmPeriod.YEAR -> ListenBrainzRange.year
                        else -> null
                    }

                    if (k.listenBrainzRange == listenBrainzPeriod) k else null
                }
            } else if (selectedPeriod !in timePeriods) {
                timePeriods.firstNotNullOf { (k, v) -> k } // just select the first
            } else
                selectedPeriod
        }
        .onEach { period ->
            // mark as non-refresh request
            _refreshCount.value = 0

            if (_periodType.value != TimePeriodType.LISTENBRAINZ && _periodType.value != null && period != null) {
                PlatformStuff.mainPrefs.updateData {
                    it.copy(
                        lastChartsPeriodType = _periodType.value!!,
                        lastChartsLastfmPeriod = period,
                        lastChartsCustomPeriod = _customPeriodInput.value
                    )
                }
            } else if (_periodType.value == TimePeriodType.LISTENBRAINZ && period != null) {
                PlatformStuff.mainPrefs.updateData {
                    it.copy(
                        lastChartsListenBrainzPeriod = period
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun setPeriodType(type: TimePeriodType) {
        _periodType.value = type
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