package com.arn.scrobble.charts

import androidx.lifecycle.viewModelScope
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Period
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.charts.TimePeriodsGenerator.Companion.toTimePeriod
import com.arn.scrobble.ui.MusicEntryLoaderInput
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch


class RandomVMOld : ChartsPeriodVMOld() {
    private val _musicEntry = MutableSharedFlow<MusicEntry?>(replay = 1)
    val musicEntry = _musicEntry.asSharedFlow()
    private val _error = MutableStateFlow<Throwable?>(null)
    val error = _error.asStateFlow()
    private val _hasLoaded = MutableStateFlow(false)
    val hasLoaded = _hasLoaded.asStateFlow()
    private var totalScrobbles = -1
    private var totalLoves = -1
    private var totalArtists = -1
    private var totalAlbums = -1

    init {
        viewModelScope.launch {
            input.filterNotNull()
                .combine(selectedPeriod) { inp, period ->
                    if (inp.timePeriod != period) {
                        resetTotals()
                    }

                    inp.copy(timePeriod = period)
                }
                .mapLatest { input ->
                    _hasLoaded.emit(false)
                    mainPrefs.updateData { it.copy(lastRandomType = input.type) }
                    loadRandom(input)
                }
                .catch { exception ->
                    emit(null to input.value!!.type)
                }
                .collectLatest { (entry, total) ->
                    _hasLoaded.emit(true)
                    if (entry != null) {
                        _musicEntry.emit(entry)
                        _error.emit(null)
                    } else {
                        _musicEntry.emit(null)
                        _error.emit(IllegalStateException(PlatformStuff.application.getString(R.string.charts_no_data)))
                    }
                    input.value?.type?.let { type ->
                        setTotal(type, total)
                    }
                }
        }
    }

    private fun getTotal(type: Int = input.value?.type ?: -1): Int {
        return when (type) {
            Stuff.TYPE_TRACKS -> totalScrobbles
            Stuff.TYPE_LOVES -> totalLoves
            Stuff.TYPE_ARTISTS -> totalArtists
            Stuff.TYPE_ALBUMS -> totalAlbums
            else -> throw IllegalArgumentException("Unknown type $type")
        }
    }

    private fun setTotal(type: Int, total: Int) {
        when (type) {
            Stuff.TYPE_TRACKS -> totalScrobbles = total
            Stuff.TYPE_LOVES -> totalLoves = total
            Stuff.TYPE_ARTISTS -> totalArtists = total
            Stuff.TYPE_ALBUMS -> totalAlbums = total
            else -> throw IllegalArgumentException("Unknown type $type")
        }
    }

    private suspend fun loadRandom(input: MusicEntryLoaderInput): Pair<MusicEntry?, Int> {
        input.timePeriod!!
        var isCharts = false

        suspend fun getOne(page: Int): Pair<MusicEntry?, Int> {
            val _entry: MusicEntry?
            val _total: Int
            when {
                input.type == Stuff.TYPE_TRACKS &&
                        Scrobblables.current.value!!.userAccount.type == AccountType.LASTFM -> {
                    isCharts = false
                    var to = -1L
                    var from = -1L

                    if (input.timePeriod.period == null) {
                        from = input.timePeriod.start
                        to = input.timePeriod.end
                    } else if (input.timePeriod.period != Period.OVERALL) {
                        val approxTimePeriod = input.timePeriod.period.toTimePeriod()
                        from = approxTimePeriod.start
                        to = approxTimePeriod.end
                    }

                    Scrobblables.current.value!!.getRecents(
                        page,
                        input.user.name,
                        from = from,
                        to = to,
                        limit = 1,
                    ).getOrThrow().let {
                        _entry = it.entries.firstOrNull()
                        _total = it.attr.totalPages
                    }
                }

                input.type == Stuff.TYPE_LOVES -> {
                    isCharts = false
                    Scrobblables.current.value!!.getLoves(
                        page,
                        input.user.name,
                        limit = 1,
                    ).getOrThrow().let {
                        _entry = it.entries.firstOrNull()
                        _total = it.attr.totalPages
                    }
                }

                else -> {
                    isCharts = true
                    Scrobblables.current.value!!.getCharts(
                        input.type,
                        input.timePeriod,
                        page,
                        input.user.name,
                        limit = if (input.timePeriod.period == null && Scrobblables.current.value?.userAccount?.type == AccountType.LASTFM)
                            -1
                        else
                            1
                    ).getOrThrow().let {
                        if (input.timePeriod.period == null && Scrobblables.current.value?.userAccount?.type == AccountType.LASTFM) {
                            _entry = it.entries.randomOrNull()
                            _total = it.entries.size
                        } else {
                            _entry = it.entries.firstOrNull()
                            _total = it.attr.totalPages
                        }
                    }
                }
            }

            return Pair(_entry, _total)
        }

        var total = getTotal(input.type)
        var result: Pair<MusicEntry?, Int>

        if (total == -1) {
            result = getOne(1)
            total = result.second

            if (total > 0 && isCharts && input.timePeriod.period == null && Scrobblables.current.value?.userAccount?.type == AccountType.LASTFM) {
                // lastfm weekly charts. Already randomised
                return result
            }
        }

        if (total > 0) {
            val page = (1..total).random()
            result = getOne(page)

            if (result.first != null && !isCharts &&
                Scrobblables.current.value!!.userAccount.type == AccountType.LASTFM
            ) {
                val track = result.first as Track

                Requesters.lastfmUnauthedRequester
                    .getInfo(track, username = input.user.name)
                    .onSuccess {
                        val t = track.copy(
                            userplaycount = it.userplaycount,
                            album = if (input.type == Stuff.TYPE_LOVES)
                                it.album?.copy(image = it.image)
                            else
                                track.album,
                        )

                        result = t to result.second
                    }
            }
        } else {
            result = null to 0
        }

        return result
    }

    private fun resetTotals() {
        totalScrobbles = -1
        totalArtists = -1
        totalAlbums = -1
    }

}