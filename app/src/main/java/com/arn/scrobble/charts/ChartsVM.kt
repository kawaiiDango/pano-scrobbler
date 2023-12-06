package com.arn.scrobble.charts

import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Period
import com.arn.scrobble.api.listenbrainz.ListenBrainz
import com.arn.scrobble.charts.TimePeriodsGenerator.Companion.toDuration
import com.arn.scrobble.charts.TimePeriodsGenerator.Companion.toTimePeriod
import com.arn.scrobble.utils.Stuff.setMidnight
import com.arn.scrobble.utils.Stuff.setUserFirstDayOfWeek
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar


class ChartsVM : ChartsPeriodVM() {
    private val _entries = MutableStateFlow<List<MusicEntry>?>(null)
    val entries = _entries.asStateFlow()
    var reachedEnd = false
    private val _hasLoaded = MutableStateFlow(false)
    val hasLoaded = _hasLoaded.asStateFlow()
    var totalCount = 0
        private set

    override suspend fun loadCharts(
        type: Int,
        page: Int,
        timePeriod: TimePeriod,
        username: String,
        networkOnly: Boolean
    ) {
        var prevPeriod: TimePeriod? = null

        if (periodType.value != TimePeriodType.CONTINUOUS) {
            if (Scrobblables.current !is ListenBrainz) {
                reachedEnd = true
            }
            timePeriods.value.inverse[timePeriod]?.let { idx ->
                prevPeriod = timePeriods.value[idx + 1]
            }
        } else {
            if (timePeriod.period != null && timePeriod.period != Period.OVERALL) {
                val cal = Calendar.getInstance()
                cal.setUserFirstDayOfWeek()
                cal.setMidnight()
                val duration = timePeriod.period.toDuration(endTime = cal.timeInMillis)
                prevPeriod =
                    timePeriod.period.toTimePeriod(endTime = cal.timeInMillis - duration)
            }
        }

        _hasLoaded.emit(false)

        val result = Scrobblables.current!!
            .getChartsWithStonks(
                type = type,
                timePeriod = timePeriod,
                prevTimePeriod = prevPeriod,
                page = page,
                username = username,
                networkOnly = networkOnly
            )

        _hasLoaded.emit(true)

        result.onSuccess {
            reachedEnd = it.attr.page >= it.attr.totalPages
            it.attr.total?.let { totalCount = it }
        }

        emitEntries(_entries, result, page > 1)
    }
}