package com.arn.scrobble.charts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.LastfmPeriod
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.charts.TimePeriodsGenerator.Companion.toTimePeriod
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.charts_no_data


class RandomVM : ViewModel() {
    private val _musicEntry = MutableStateFlow<MusicEntry?>(null)
    private val _input = MutableStateFlow<RandomLoaderInput?>(null)
    private val _refreshTrigger = MutableSharedFlow<Unit>()
    val musicEntry = _musicEntry.asStateFlow()
    private val _error = MutableStateFlow<Throwable?>(null)
    val error = _error.asStateFlow()
    private val _hasLoaded = MutableStateFlow(false)
    val hasLoaded = _hasLoaded.asStateFlow()
    private var totalScrobbles = -1
    private var totalLoves = -1
    private var totalArtists = -1
    private var totalAlbums = -1
    private var totalsLoadedForTimePeriod: TimePeriod? = null
    private val mainPrefs = PlatformStuff.mainPrefs

    init {
        viewModelScope.launch {
            _input
                .filterNotNull()
                .debounce(300)
                .combine(_refreshTrigger.onStart { emit(Unit) }
                ) { input, _ -> input }
                .mapLatest { input ->
                    _hasLoaded.emit(false)
                    mainPrefs.updateData { it.copy(randomType = input.type) }
                    val result = loadRandom(input)
                    setTotal(input.type, result.second)
                    result
                }
                .catch { exception ->
                    emit(null to -1)
                }
                .collectLatest { (entry, total) ->
                    _hasLoaded.emit(true)
                    if (entry != null) {
                        _musicEntry.emit(entry)
                        _error.emit(null)
                    } else {
                        _musicEntry.emit(null)
                        _error.emit(IllegalStateException(getString(Res.string.charts_no_data)))
                    }
                }
        }
    }

    fun setRandomInput(input: RandomLoaderInput, refresh: Boolean) {
        viewModelScope.launch {
            _input.emit(input)
            if (refresh) {
                _refreshTrigger.emit(Unit)
            }
        }
    }

    private fun getTotal(type: Int): Int {
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

    private suspend fun loadRandom(input: RandomLoaderInput): Pair<MusicEntry?, Int> {
        var isCharts = false
        val currentScrobblable = Scrobblables.current!!

        suspend fun getOne(page: Int): Pair<MusicEntry?, Int> {
            val _entry: MusicEntry?
            val _total: Int
            when {
                input.type == Stuff.TYPE_TRACKS &&
                        currentScrobblable.userAccount.type == AccountType.LASTFM -> {
                    isCharts = false
                    var to = -1L
                    var from = -1L

                    if (input.timePeriod.lastfmPeriod == null) {
                        from = input.timePeriod.start
                        to = input.timePeriod.end
                    } else if (input.timePeriod.lastfmPeriod != LastfmPeriod.OVERALL) {
                        val approxTimePeriod = input.timePeriod.lastfmPeriod.toTimePeriod()
                        from = approxTimePeriod.start
                        to = approxTimePeriod.end
                    }

                    currentScrobblable.getRecents(
                        page,
                        input.username,
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
                    currentScrobblable.getLoves(
                        page,
                        input.username,
                        limit = 1,
                    ).getOrThrow().let {
                        _entry = it.entries.firstOrNull()
                        _total = it.attr.totalPages
                    }
                }

                else -> {
                    isCharts = true
                    currentScrobblable.getCharts(
                        input.type,
                        input.timePeriod,
                        page,
                        input.username,
                        limit = if (input.timePeriod.lastfmPeriod == null && currentScrobblable.userAccount.type == AccountType.LASTFM)
                            -1
                        else
                            1
                    ).getOrThrow().let {
                        if (input.timePeriod.lastfmPeriod == null && currentScrobblable.userAccount.type == AccountType.LASTFM) {
                            _entry = it.entries.randomOrNull()
                            _total = it.entries.size
                        } else {
                            _entry = it.entries.firstOrNull()
                            _total = it.attr.totalPages
                        }
                    }
                }
            }

            totalsLoadedForTimePeriod = input.timePeriod

            return Pair(_entry, _total)
        }

        if (totalsLoadedForTimePeriod != input.timePeriod) {
            resetTotals()
        }

        var total = getTotal(input.type)
        var result: Pair<MusicEntry?, Int>

        if (total == -1) {
            result = getOne(1)
            total = result.second

            if (total > 0 && isCharts && input.timePeriod.lastfmPeriod == null && currentScrobblable.userAccount.type == AccountType.LASTFM) {
                // lastfm weekly charts. Already randomised
                return result
            }
        }

        if (total > 0) {
            val page = (1..total).random()
            result = getOne(page)

            if (result.first != null && !isCharts &&
                currentScrobblable.userAccount.type == AccountType.LASTFM
            ) {
                val track = result.first as Track

                Requesters.lastfmUnauthedRequester
                    .getInfo(track, username = input.username)
                    .onSuccess {
                        val t = track.copy(
                            userplaycount = it.userplaycount,
                            album = if (input.type == Stuff.TYPE_LOVES)
                                it.album?.copy(image = it.album.image)
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