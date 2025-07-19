package com.arn.scrobble.charts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.utils.PanoTimeFormatter
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff.toInverseMap
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlin.math.max


open class ChartsPeriodVM : ViewModel() {

    protected val mainPrefs = PlatformStuff.mainPrefs

    private val _periodType = MutableStateFlow<TimePeriodType?>(null)

    val periodType = _periodType
        .combine(PlatformStuff.mainPrefs.data.mapLatest { it.currentAccountType }) { currentType, prefsType ->
            if (prefsType == AccountType.LISTENBRAINZ) TimePeriodType.LISTENBRAINZ
            else currentType
        }

    private val _customPeriodInput =
        MutableStateFlow(TimePeriod(1577836800000L, 1609459200000L)) // 2020

    private val _user = MutableStateFlow<UserCached?>(null)
    val timePeriods: StateFlow<Map<TimePeriod, Int>> = periodType
        .filterNotNull()
        .combine(
            _customPeriodInput
        ) { periodType, customPeriod ->
            periodType to customPeriod
        }
        .combine(
            _user.filterNotNull().take(1)
        ) { (periodType, customPeriod), user ->

            val timePeriodsGenerator =
                TimePeriodsGenerator(
                    user.registeredTime,
                    System.currentTimeMillis(),
                    generateFormattedStrings = true
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
            if (selectedPeriod !in timePeriods) {
                timePeriods.firstNotNullOf { (k, v) -> k } // just select the first
            } else
                selectedPeriod
        }
        .onEach { period ->
            if (_periodType.value != TimePeriodType.LISTENBRAINZ && _periodType.value != null && period != null) {
                GlobalScope.launch {
                    mainPrefs.updateData {
                        it.copy(
                            lastChartsPeriodType = _periodType.value!!,
                            lastChartsLastfmPeriodSelected = period,
                            lastChartsCustomPeriod = _customPeriodInput.value
                        )
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    init {
        viewModelScope.launch {
            if (Scrobblables.currentAccount.value?.type == AccountType.LISTENBRAINZ) {
                val (type, selected, custom) = PlatformStuff.mainPrefs.data.mapLatest {
                    Triple(
                        it.lastChartsPeriodType,
                        it.lastChartsLastfmPeriodSelected,
                        it.lastChartsCustomPeriod
                    )
                }.first()
                _periodType.emit(type)
                _selectedPeriod.value = selected
                _customPeriodInput.value = custom
            }
        }
    }

    fun setUser(user: UserCached) {
        _user.value = user
    }

    fun setPeriodType(type: TimePeriodType) {
        viewModelScope.launch {
            _periodType.emit(type)
        }
    }

    fun setSelectedPeriod(period: TimePeriod) {
        _selectedPeriod.value = period
    }

    fun setCustomPeriodInput(period: TimePeriod) {
        _customPeriodInput.value = period
    }
}