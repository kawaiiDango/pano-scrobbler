package com.arn.scrobble.recents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.insertSeparators
import androidx.paging.map
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.ScrobbleEverywhere
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.LastFm
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.listenbrainz.ListenBrainz
import com.arn.scrobble.db.CachedTracksDao
import com.arn.scrobble.db.DirtyUpdate
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.ui.PanoSnackbarVisuals
import com.arn.scrobble.ui.generateKey
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.work.CommonWorkState
import com.arn.scrobble.work.PendingScrobblesWork
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.lastfm_reauth
import java.util.Calendar


class ScrobblesVM(
    val user: UserCached,
    track: Track?, // for track-specific scrobbles view
) : ViewModel() {
    val pendingScrobbles = PanoDb.db.getPendingScrobblesDao().allFlow(10000)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    private val _nlsEnabled = MutableStateFlow(PlatformStuff.isNotificationListenerEnabled())
    val nlsEnabled = _nlsEnabled.asStateFlow()
    private val _scrobblerServiceRunning = MutableStateFlow<Boolean?>(null)
    val scrobblerServiceRunning = _scrobblerServiceRunning.asStateFlow()

    private val editsAndDeletes = MutableStateFlow<Map<String, Track?>>(emptyMap())
    val deletedTracksCount = editsAndDeletes
        .mapLatest { it.count { (k, v) -> v == null } }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)
    private val _pkgMap = MutableStateFlow<Map<Long, String>>(emptyMap())
    val pkgMap = _pkgMap.asStateFlow()
    private val _input = MutableStateFlow<ScrobblesInput?>(null)
    private val _firstScrobbleTime = MutableStateFlow<Long?>(null)
    val firstScrobbleTime = _firstScrobbleTime.asStateFlow()
    private val _total = MutableStateFlow(track?.userplaycount)
    val total = _total.asStateFlow()

    var totalPages = 1
        private set

    private val _loadedCachedVersion = MutableStateFlow(false)
    private val _firstPageLoadedTime = MutableStateFlow<Long?>(null)
    val firstPageLoadedTime = _firstPageLoadedTime.asStateFlow()

    private val pagingConfig = PagingConfig(
        pageSize = Stuff.DEFAULT_PAGE_SIZE,
        enablePlaceholders = true,
        initialLoadSize = Stuff.DEFAULT_PAGE_SIZE,
        prefetchDistance = 4
    )

    val tracks = _input
        .filterNotNull()
        .combine(_loadedCachedVersion) { input, loadedCachedVersion ->
            val loadedCachedVersion =
                if (user.isSelf && track == null)
                    loadedCachedVersion
                else
                    true
            input to loadedCachedVersion
        }
        .flatMapLatest { (input, loadedCachedVersion) ->
            Pager(
                config = pagingConfig,
                pagingSourceFactory = {
                    ScrobblesPagingSource(
                        username = user.name,
                        loadLovedTracks = input.loadLoved,
                        timeJumpMillis = input.timeJumpMillis,
                        track = track,
                        cachedOnly = !loadedCachedVersion,
                        scrobbleSources = input.showScrobbleSources,
                        addToPkgMap = { time, pkg -> _pkgMap.value += time to pkg },
                        onSetFirstScrobbleTime = { _firstScrobbleTime.value = it },
                        onSetFirstPageLoadedTime = {
                            _firstPageLoadedTime.value = it
                        },
                        onSetTotal = {
                            // show total only if loading loved tracks or track-specific scrobbles
                            _total.value = if (input.loadLoved || track != null)
                                it
                            else
                                null
                        },
                        onClearOverrides = ::clearOverrides
                    )
                }
            )
                .flow
                .cachedIn(viewModelScope)
                .combine(editsAndDeletes) { pagingData, editsAndDeletesMap ->
                    if (!_loadedCachedVersion.value) {
                        viewModelScope.launch {
                            delay(50)
                            _loadedCachedVersion.value = true
                        }
                    }

                    val keysTillNow = mutableSetOf<String>()
                    val cal = Calendar.getInstance()!!

                    // filter duplicates to prevent a crash in LazyColumn
                    pagingData.map { track ->
                        val key = track.generateKey()
                        val editedTrack = editsAndDeletesMap[key]
                        TrackWrapper.TrackItem(editedTrack ?: track, key)
                    }
                        .filter {
                            val keep =
                                it.key !in keysTillNow &&
                                        (editsAndDeletesMap[it.key] != null || it.key !in editsAndDeletesMap)
                            keysTillNow += it.key
                            keep
                        }
                        .let {
                            if (track == null && !input.loadLoved) {
                                it.insertSeparators { before, after ->
                                    if (before?.track?.date == null ||
                                        after?.track?.date == null
                                    )
                                        return@insertSeparators null

                                    cal.timeInMillis = before.track.date
                                    val beforeDay = cal[Calendar.DAY_OF_YEAR]
                                    cal.timeInMillis = after.track.date
                                    val afterDay = cal[Calendar.DAY_OF_YEAR]

                                    if (beforeDay != afterDay)
                                        TrackWrapper.SeparatorItem(
                                            after.track.date,
                                            "sep_${after.track.date}"
                                        )
                                    else
                                        null
                                }
                            } else
                                it as PagingData<TrackWrapper>
                        }
                }
        }.cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            val hasPending = PanoDb.db.getPendingScrobblesDao().count() > 0
            val isRunning = PendingScrobblesWork.state().first() == CommonWorkState.RUNNING

            if (hasPending && !isRunning)
                PendingScrobblesWork.schedule(force = true)
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

        if (user.isSelf)
            updateScrobblerServiceStatus()
    }

    fun updateScrobblerServiceStatus() {
        viewModelScope.launch {
            _nlsEnabled.value = PlatformStuff.isNotificationListenerEnabled()

            if (_nlsEnabled.value &&
                PlatformStuff.mainPrefs.data.map { it.scrobblerEnabled }.first() &&
                scrobblerServiceRunning.value == null
            ) // do only once
                _scrobblerServiceRunning.value = PlatformStuff.isScrobblerRunning()
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
        editsAndDeletes.value = emptyMap()
        _pkgMap.value = emptyMap()
    }

    fun removeTrack(item: TrackWrapper.TrackItem) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                Scrobblables.current?.delete(item.track)
                    ?.onFailure {
                        it.printStackTrace()
                        if (it is LastFm.CookiesInvalidatedException) {
                            Stuff.globalSnackbarFlow.emit(
                                PanoSnackbarVisuals(
                                    getString(Res.string.lastfm_reauth),
                                    isError = true
                                )
                            )
                        } else
                            Stuff.globalExceptionFlow.emit(it)
                    }
                    ?.onSuccess {
                        CachedTracksDao.deltaUpdateAll(
                            item.track,
                            -1,
                            DirtyUpdate.BOTH
                        )
                    }
            }
        }
        editsAndDeletes.value += item.key to null
    }

    fun loveOrUnlove(item: TrackWrapper.TrackItem, love: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            ScrobbleEverywhere.loveOrUnlove(
                item.track,
                love
            )
        }

        editTrack(
            item.key,
            item.track.copy(userloved = love)
        )
    }

    fun hateOrUnhate(item: TrackWrapper.TrackItem, hate: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (hate)
                (Scrobblables.current as? ListenBrainz)?.hate(item.track)
            else
                ScrobbleEverywhere.loveOrUnlove(item.track, false)
        }
        editTrack(
            item.key,
            item.track.copy(userHated = hate)
        )
    }

    fun editTrack(key: String, editedTrack: Track) {
        editsAndDeletes.value += key to editedTrack
    }

}

expect fun ScrobblesVM.shareTrack(track: Track, shareSig: String?)