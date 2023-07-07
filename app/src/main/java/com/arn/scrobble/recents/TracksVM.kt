package com.arn.scrobble.recents

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.LFMRequester
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.scrobbleable.ListenBrainz
import com.arn.scrobble.scrobbleable.Scrobblables
import com.arn.scrobble.ui.SectionedVirtualList
import com.hadilq.liveevent.LiveEvent
import de.umass.lastfm.PaginatedResult
import de.umass.lastfm.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date


class TracksVM : ViewModel() {
    val tracksReceiver by lazy { LiveEvent<PaginatedResult<Track>>() }
    val firstScrobbledDate by lazy { MutableLiveData<Date>() }
    val tracks by lazy { mutableListOf<Track>() }
    val virtualList = SectionedVirtualList()
    val pendingScrobblesLd by lazy { PanoDb.db.getPendingScrobblesDao().allLd(10000) }
    val pendingLovesLd by lazy { PanoDb.db.getPendingLovesDao().allLd(10000) }
    var isShowingLoves = false
    var pendingSubmitAttempted = false

    val deletedTracksStringSet by lazy { mutableSetOf<String>() }
    val pkgMap = mutableMapOf<Long, String>()
    val paletteColors by lazy { MutableLiveData<PaletteColors>() }

    private val errorNotifier = LFMRequester.ExceptionNotifier()

    var username: String? = null
    var page = 1
    var totalPages = 1
    var loadedCached = false
    var selectedPos = 0
    var toTime: Long? = null
    private var lastLoadJob: Job? = null


    fun loadRecents(page: Int) {
        this.page = page

        val isListenbrainz = Scrobblables.current is ListenBrainz
        val _to = if (isListenbrainz && page > 1)
            tracks.lastOrNull()?.playedWhen?.time ?: -1
        else
            toTime ?: -1L

        val limit = 100
        lastLoadJob?.cancel()
        lastLoadJob = viewModelScope.launch(errorNotifier) {
            val pr = withContext(Dispatchers.IO) {
                Scrobblables.current!!.getRecents(
                    page,
                    username,
                    cached = !loadedCached,
                    to = _to,
                    limit = limit,
                    includeNowPlaying = toTime == null,
                )
            }
            val _loadedCached = loadedCached
            if (!loadedCached) {
                selectedPos = 0
                loadedCached = true
            }

            // time jump pg 1 always resets selection
            if (toTime != null && page == 1)
                selectedPos = 0

            tracksReceiver.value = pr

            if (pr.isStale && !_loadedCached) {
                loadRecents(page)
            }
        }
    }

    fun loadLoves(page: Int) {
        this.page = page

        lastLoadJob?.cancel()
        lastLoadJob = viewModelScope.launch(errorNotifier) {
            val pr = withContext(Dispatchers.IO) {
                Scrobblables.current!!.getLoves(
                    page,
                    username,
                    cached = !loadedCached,
                )
            }

            val _loadedCached = loadedCached

            if (!loadedCached) {
                selectedPos = 0
                loadedCached = true
            }
            tracksReceiver.value = pr
            if (pr.isStale && !_loadedCached) {
                loadLoves(page)
            }
        }
    }

    fun loadTrackScrobbles(track: Track, page: Int) {
        this.page = page
        LFMRequester(viewModelScope, tracksReceiver).getTrackScrobbles(
            track,
            page,
            username
        )
    }

    fun loadFirstScrobbleDate(pr: PaginatedResult<Track>) {
        LFMRequester(viewModelScope, firstScrobbledDate).getTrackFirstScrobble(
            pr,
            username
        )
    }

    fun reemitColors() {
        paletteColors.value = paletteColors.value
    }

}