package com.arn.scrobble.recents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.ui.PanoSnackbarVisuals
import com.arn.scrobble.ui.generateKey
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.work.CommonWorkState
import com.arn.scrobble.work.PendingScrobblesWork
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class ScrobblesVM : ViewModel() {
    val pendingScrobbles = PanoDb.db.getPendingScrobblesDao().allFlow(10000)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    private val _nlsEnabled = MutableStateFlow(PlatformStuff.isNotificationListenerEnabled())
    val nlsEnabled = _nlsEnabled.asStateFlow()
    private val _scrobblerServiceRunning = MutableStateFlow<Boolean?>(null)
    val scrobblerServiceRunning = _scrobblerServiceRunning.asStateFlow()

    private val _editedTracksMap = MutableStateFlow<Map<String, Track>>(emptyMap())
    val editedTracksMap = _editedTracksMap.asStateFlow()
    private val _deletedTracksSet = MutableStateFlow<Set<Track>>(emptySet())
    val deletedTracksSet = _deletedTracksSet.asStateFlow()

    private val _pkgMap = MutableStateFlow<Map<Long, String>>(emptyMap())
    val pkgMap = _pkgMap.asStateFlow()
    val lastScrobbleOfTheDaySet = mutableSetOf<Long>() // set of play times
    private val _input = MutableStateFlow<ScrobblesInput?>(null)
    val input = _input.asStateFlow()
    private val _firstScrobbleTime = MutableStateFlow<Long?>(null)
    val firstScrobbleTime = _firstScrobbleTime.asStateFlow()

    var totalPages = 1
        private set

    private val _loadedCachedVersion = MutableStateFlow(false)
    private val _lastRecentsRefreshTime = MutableStateFlow(System.currentTimeMillis())
    val lastRecentsRefreshTime = _lastRecentsRefreshTime.asStateFlow()

    private val pagingConfig = PagingConfig(
        pageSize = Stuff.DEFAULT_PAGE_SIZE,
        enablePlaceholders = true,
        initialLoadSize = Stuff.DEFAULT_PAGE_SIZE,
        prefetchDistance = 4
    )

    val tracks = _input.filterNotNull()
        .combine(_loadedCachedVersion) { input, loadedCachedVersion ->
            val loadedCachedVersion =
                if (input.user.isSelf && input.track == null) loadedCachedVersion else true
            input to loadedCachedVersion
        }
        .flatMapLatest { (input, loadedCachedVersion) ->
            Pager(
                config = pagingConfig,
                pagingSourceFactory = {
                    ScrobblesPagingSource(
                        username = input.user.name,
                        loadLovedTracks = input.loadLoved,
                        timeJumpMillis = input.timeJumpMillis,
                        track = input.track,
                        cachedOnly = !loadedCachedVersion,
                        addLastScrobbleOfTheDay = { lastScrobbleOfTheDaySet += it },
                        addToPkgMap = { time, pkg -> _pkgMap.value += time to pkg },
                        onSetFirstScrobbleTime = { _firstScrobbleTime.value = it },
                        onSetLastRecentsRefreshTime = {
                            _lastRecentsRefreshTime.value = it
                        },
                        onClearOverrides = ::clearOverrides
                    )
                }
            ).flow
        }
        .cachedIn(viewModelScope)
        .onEach {
            if (!_loadedCachedVersion.value) {
                viewModelScope.launch {
                    delay(50)
                    _loadedCachedVersion.value = true
                }
            }
        }

    init {
        viewModelScope.launch {
            val hasPending = PanoDb.db.getPendingScrobblesDao().count() > 0

            if (hasPending && Stuff.isOnline)
                PendingScrobblesWork.checkAndSchedule()
        }

        viewModelScope.launch {
            PendingScrobblesWork.getProgress()
                .collectLatest {
                    if (it.state == CommonWorkState.RUNNING || it.state == CommonWorkState.FAILED) {
                        val snackbarData = PanoSnackbarVisuals(
                            message = it.message,
                            isError = it.isError
                        )
                        Stuff.globalSnackbarFlow.emit(snackbarData)
                    }
                }
        }

        updateScrobblerServiceStatus()
    }

    fun updateScrobblerServiceStatus() {
        viewModelScope.launch {
            if (input.value?.user?.isSelf == true) {
                _nlsEnabled.value = PlatformStuff.isNotificationListenerEnabled()

                if (_nlsEnabled.value &&
                    PlatformStuff.mainPrefs.data.map { it.scrobblerEnabled }.first() &&
                    scrobblerServiceRunning.value == null
                ) // do only once
                    _scrobblerServiceRunning.value = PlatformStuff.isScrobblerRunning()
            }
        }
    }

    fun setScrobblesInput(input: ScrobblesInput) {
        _input.value = input
    }


    /*
    private suspend fun loadRecents(
        page: Int,
        username: String,
        timePeriod: TimePeriod?,
    ) {
        val isListenbrainz = Scrobblables.current is ListenBrainz
        val _to = if (isListenbrainz && page > 1)
            tracks.value?.lastOrNull()?.date ?: -1L
        else
            timePeriod?.end ?: -1L

        _hasLoaded.emit(false)

        val includeNowPlaying = _input.value?.timePeriod == null && page == 1

        val pr = Scrobblables.current!!.getRecents(
            page,
            username,
            cached = !loadedInitialCachedVersion,
            to = _to,
            limit = limit,
            includeNowPlaying = includeNowPlaying,
        )

        if (!loadedInitialCachedVersion) {
            selectedPos = 0
            loadedInitialCachedVersion = true
            viewModelScope.launch {
                loadRecents(page, username, timePeriod)
            }
        }

        // time jump pg 1 always resets selection
        if (_to != -1L && page == 1)
            selectedPos = 0

        pr.onSuccess {
            totalPages = max(1, it.attr.totalPages) //dont let totalpages be 0
            _fileException.emit(null)

            // mark first scrobble of the day
            val t = when {
                it.attr.page == 1 -> it.entries.firstOrNull()
                it.entries.isNotEmpty() -> it.entries.last()
                else -> null
            }

            cal.timeInMillis = t?.date ?: System.currentTimeMillis()

            var prevDate = cal[Calendar.DAY_OF_YEAR]
            it.entries.forEach { track ->
                if (track in deletedTracksSet || track.date == null)
                    return@forEach


                cal.timeInMillis = track.date
                val currentDate = cal[Calendar.DAY_OF_YEAR]
                if (prevDate != currentDate)
                    _lastScrobbleOfTheDaySet += track.date
                prevDate = currentDate
            }
        }

        pr.onFailure {
            if (it is FileScrobblable.FException)
                _fileException.emit(it)
            else if ((it as? ApiException)?.code == 504)
                return
        }

        emitTracks(pr, page > 1)

//        if (setLoading) {
        yield()
        _hasLoaded.emit(true)
//        }
    }
*/

    private fun clearOverrides() {
        _deletedTracksSet.value = emptySet()
        _editedTracksMap.value = emptyMap()
        _pkgMap.value = emptyMap()
        lastScrobbleOfTheDaySet.clear()
    }

    fun removeTrack(track: Track) {
        _deletedTracksSet.value += track
    }

    fun editTrack(origTrack: Track, editedTrack: Track) {
        _editedTracksMap.value += origTrack.generateKey() to editedTrack
    }

}