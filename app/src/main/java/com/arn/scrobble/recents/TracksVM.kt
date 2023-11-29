package com.arn.scrobble.recents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.api.Requesters.toFlow
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.LastFm
import com.arn.scrobble.api.lastfm.PageResult
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.listenbrainz.ListenBrainz
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.ui.MusicEntryLoaderInput
import com.arn.scrobble.ui.SectionedVirtualList
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.max


class TracksVM : ViewModel() {
    private val _tracks = MutableStateFlow<List<Track>?>(null)
    val tracks = _tracks.asStateFlow()
    private val _firstScrobbledTime = MutableStateFlow<Int?>(null)
    val firstScrobbledTime = _firstScrobbledTime.asStateFlow()
    val virtualList = SectionedVirtualList()
    val pendingScrobbles = PanoDb.db.getPendingScrobblesDao().allFlow(10000)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val pendingLoves = PanoDb.db.getPendingLovesDao().allFlow(10000)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    var pendingSubmitAttempted = false

    private val deletedTracksSet = mutableSetOf<Track>()
    val pkgMap = mutableMapOf<Int, String>() // play time -> pkg
    val lastScrobbleOfTheDaySet = mutableSetOf<Int>() // set of play times
    private val _paletteColors = MutableStateFlow<PaletteColors?>(null)
    val paletteColors = _paletteColors.asStateFlow()
    private val _input = MutableStateFlow<MusicEntryLoaderInput?>(null)
    val input = _input.asStateFlow()
    private val _hasLoaded = MutableStateFlow(false)
    val hasLoaded = _hasLoaded.asStateFlow()
    val isShowingLoves get() = input.value?.type == Stuff.TYPE_LOVES
    private val cal by lazy { Calendar.getInstance() }

    var totalPages = 1
        private set

    private var loadedInitialCachedVersion = false
    var selectedPos = 0
    private val limit = 100
    var lastRecentsLoadTime = 0L
        private set

    init {
        viewModelScope.launch {
            _input.filterNotNull()
                .collectLatest {
                    _hasLoaded.emit(false)
                    when (it.type) {
                        Stuff.TYPE_TRACKS -> {
                            if (it.entry is Track) {
                                loadTrackScrobbles(it.entry, it.page, it.user.name)
                            } else {
                                loadRecents(it.page, it.user.name, it.timePeriod)
                            }
                            lastRecentsLoadTime = System.currentTimeMillis()
                        }

                        Stuff.TYPE_LOVES -> {
                            loadLoves(it.page, it.user.name)
                        }

                        else -> throw IllegalArgumentException("Unknown type")
                    }
                    _hasLoaded.emit(true)
                }
        }
    }

    fun setInput(input: MusicEntryLoaderInput) {
        viewModelScope.launch {
            _input.emit(input)
        }
    }

    fun setPaletteColors(colors: PaletteColors) {
        viewModelScope.launch {
            _paletteColors.emit(colors)
        }
    }

    private suspend fun loadRecents(page: Int, username: String, timePeriod: TimePeriod?) {
        val isListenbrainz = Scrobblables.current is ListenBrainz
        val _to = if (isListenbrainz && page > 1)
            tracks.value?.lastOrNull()?.date ?: -1
        else
            timePeriod?.end?.div(1000)?.toInt() ?: -1

        val pr = Scrobblables.current!!.getRecents(
            page,
            username,
            cached = !loadedInitialCachedVersion,
            to = _to,
            limit = limit,
            includeNowPlaying = _input.value?.timePeriod == null,
        )

        if (!loadedInitialCachedVersion) {
            selectedPos = 0
            loadedInitialCachedVersion = true

            viewModelScope.launch {
                loadRecents(page, username, timePeriod)
            }
        }

        // time jump pg 1 always resets selection
        if (_to != -1 && page == 1)
            selectedPos = 0

        pr.onSuccess {
            totalPages = max(1, it.attr.totalPages) //dont let totalpages be 0
        }

        emitTracks(pr, page > 1)
    }

    private suspend fun loadLoves(page: Int, username: String) {
        val pr = Scrobblables.current!!.getLoves(
            page,
            username,
            cached = !loadedInitialCachedVersion,
        )

        if (!loadedInitialCachedVersion) {
            selectedPos = 0
            loadedInitialCachedVersion = true

            viewModelScope.launch {
                loadLoves(page, username)
            }
        }

        pr.onSuccess {
            totalPages = max(1, it.attr.totalPages) //dont let totalpages be 0

            if (isShowingLoves) return@onSuccess

            val t = when {
                it.attr.page == 1 -> it.entries.firstOrNull()
                it.entries.isNotEmpty() -> it.entries.last()
                else -> null
            }

            cal.timeInMillis = t?.date?.let { it * 1000L } ?: System.currentTimeMillis()

            // mark first scrobble of the day

            var prevDate = cal[Calendar.DAY_OF_YEAR]
            it.entries.forEach { track ->
                if (track in deletedTracksSet || track.date == null)
                    return@forEach


                cal.timeInMillis = track.date * 1000L
                val currentDate = cal[Calendar.DAY_OF_YEAR]
                if (prevDate != currentDate)
                    lastScrobbleOfTheDaySet += track.date
                prevDate = currentDate
            }
        }
        emitTracks(pr, page > 1)
    }

    private suspend fun loadTrackScrobbles(track: Track, page: Int, username: String) {
        (Scrobblables.current as? LastFm)
            ?.userGetTrackScrobbles(track, page, username, limit)
            ?.let {
                it.onSuccess { pr ->
                    totalPages = max(1, pr.attr.totalPages) //dont let totalpages be 0
                    if (page == 1 && pr.entries.size > 1)
                        viewModelScope.launch {
                            loadFirstScrobbleDate(pr, username)
                        }
                }
                emitTracks(it, page > 1)
            }
    }

    private suspend fun emitTracks(result: Result<PageResult<Track>>, concat: Boolean) {
        if (concat)
            _tracks.emitAll(result.map { (_tracks.value ?: emptyList()) + it.entries }.toFlow())
        else
            _tracks.emitAll(result.map { it.entries }.toFlow())
    }

    private suspend fun loadFirstScrobbleDate(pr: PageResult<Track>, username: String) {
        val track = pr.entries.firstOrNull() ?: return
        pr.attr.total ?: return
        val firstScrobbleDate = if (pr.attr.total > limit)
            (Scrobblables.current as? LastFm)
                ?.userGetTrackScrobbles(
                    track = track,
                    username = username,
                    page = pr.attr.total,
                    limit = 1,
                )
                ?.getOrNull()
                ?.entries
                ?.firstOrNull()
                ?.date
        else
            pr.entries.lastOrNull()?.date

        _firstScrobbledTime.emit(firstScrobbleDate)
    }

    fun removeTrack(track: Track) {
        val idx = tracks.value?.indexOf(track) ?: -1
        if (idx == -1) return

        deletedTracksSet += track

        val newTracksList = tracks.value?.filterIndexed { index, it -> index != idx }

        viewModelScope.launch {
            _tracks.emit(newTracksList)
        }
    }

    fun editTrack(track: Track) {
        val idx = virtualList.indexOfFirst { it is Track && it.date == track.date }
        val idxTracks = tracks.value?.indexOfFirst { it.date == track.date }

        if (idx == -1 || idxTracks == -1) return
        val prevTrack = virtualList[idx] as Track

        if (prevTrack == track)
            return

        deletedTracksSet += prevTrack

        val newTracksList = tracks.value?.mapIndexed { index, it ->
            if (index == idxTracks)
                track
            else
                it
        }

        viewModelScope.launch {
            _tracks.emit(newTracksList)
        }
    }

    fun reEmitColors() {
        viewModelScope.launch {
            val oldValue = _paletteColors.value
            _paletteColors.emit(null)
            _paletteColors.emit(oldValue)
        }
    }

}