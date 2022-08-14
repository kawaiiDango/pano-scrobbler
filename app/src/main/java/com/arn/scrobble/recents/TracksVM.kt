package com.arn.scrobble.recents

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.LFMRequester
import com.arn.scrobble.Stuff
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.pending.PendingListData
import com.arn.scrobble.pending.PendingScrJob
import com.arn.scrobble.pending.PendingScrService
import com.hadilq.liveevent.LiveEvent
import de.umass.lastfm.PaginatedResult
import de.umass.lastfm.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*


class TracksVM(application: Application) : AndroidViewModel(application) {
    val tracksReceiver by lazy { LiveEvent<PaginatedResult<Track>>() }
    val firstScrobbledDate by lazy { MutableLiveData<Date>() }
    val tracks by lazy { mutableListOf<Track>() }
    val deletedTracksStringSet by lazy { mutableSetOf<String>() }
    val listenerTrendReceiver by lazy { LiveEvent<List<Int>>() }
    private var lastHeroInfoTask: Job? = null
    val pkgMap = mutableMapOf<Long, String>()
    private val urlToListenerTrendMap = mutableMapOf<String, List<Int>?>()
    val paletteColors by lazy { MutableLiveData<PaletteColors>() }

    private val pendingTracks by lazy { MutableLiveData<PendingListData>() }
    private val mutex = Mutex()

    var username: String? = null
    var page = 1
    var totalPages = 1
    private var loadedCached = false
    var selectedPos = 1
    var toTime: Long? = null


    fun loadRecents(page: Int) {
        this.page = page
        LFMRequester(getApplication(), viewModelScope, tracksReceiver)
            .getRecents(
                page,
                username,
                cached = !loadedCached,
                to = toTime ?: -1L,
                includeNowPlaying = toTime == null,
                doDeltaIndex = page == 1 && toTime == null && username == null // todo: && theres no unsubmitted pending scrobbles
            )
        loadedCached = true
    }

    fun loadLoves(page: Int) {
        this.page = page
        LFMRequester(getApplication(), viewModelScope, tracksReceiver)
            .getLoves(page, username, cached = !loadedCached)
        loadedCached = true
    }

    fun loadTrackScrobbles(track: Track, page: Int) {
        this.page = page
        LFMRequester(getApplication(), viewModelScope, tracksReceiver).getTrackScrobbles(
            track,
            page,
            username
        )
    }

    fun loadFirstScrobbleDate(pr: PaginatedResult<Track>) {
        LFMRequester(getApplication(), viewModelScope, firstScrobbledDate).getTrackFirstScrobble(
            pr,
            username
        )
    }

    fun loadListenerTrend(url: String?) {
        lastHeroInfoTask?.cancel()
        if (url != null) {
            if (url in urlToListenerTrendMap) {
                listenerTrendReceiver.value = urlToListenerTrendMap[url]
                return
            }

            lastHeroInfoTask = viewModelScope.launch(Dispatchers.IO) {
                val trend = LFMRequester(getApplication(), viewModelScope)
                    .execHere<List<Int>> {
                        getListenerTrend(url)
                    }
                urlToListenerTrendMap[url] = trend
                listenerTrendReceiver.postValue(trend)
            }
        }
    }

    fun loadPending(limit: Int, submit: Boolean): MutableLiveData<PendingListData> {
        viewModelScope.launch(Dispatchers.IO) {
            mutex.withLock {
                val dao = PanoDb.getDb(getApplication()).getScrobblesDao()
                val lovesDao = PanoDb.getDb(getApplication()).getLovesDao()
                val data = PendingListData()
                data.plCount = lovesDao.count
                data.psCount = dao.count
                var limit2: Int
                if (data.plCount > 0) {
                    limit2 = limit
                    if (data.psCount > 0)
                        limit2--
                    data.plList = lovesDao.all(limit2)
                }
                if (data.psCount > 0) {
                    limit2 = limit
                    if (data.plCount > 0)
                        limit2--
                    data.psList = dao.all(limit2)
                }

                pendingTracks.postValue(data)
                if (submit && (data.plCount > 0 || data.psCount > 0)
                    && Stuff.isOnline && !PendingScrService.mightBeRunning && !PendingScrJob.mightBeRunning
                ) {
                    val intent = Intent(
                        getApplication<Application>().applicationContext,
                        PendingScrService::class.java
                    )
                    ContextCompat.startForegroundService(getApplication<Application>(), intent)
                }
            }
        }
        return pendingTracks
    }

    fun reemitColors() {
        paletteColors.value = paletteColors.value
    }

}