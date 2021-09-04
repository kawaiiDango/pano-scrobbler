package com.arn.scrobble.charts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.LFMRequester
import com.arn.scrobble.Stuff
import de.umass.lastfm.*


class ChartsVM(application: Application) : AndroidViewModel(application) {
    val chartsData by lazy { mutableListOf<MusicEntry>() }
    val imgMap by lazy { mutableMapOf<Int,String>() }
    val chartsReceiver by lazy { MutableLiveData<PaginatedResult<MusicEntry>>() }
    val listReceiver by lazy { MutableLiveData<List<MusicEntry>>() }
    val periodCountReceiver by lazy { MutableLiveData<List<ChartsOverviewFragment.ScrobbleCount>>() }
    var periodCountRequested = false
    var periodCountHeader: String? = null
    val weeklyListReceiver by lazy { MutableLiveData<List<Chart<MusicEntry>>>() }
    private var lastChartsTasks = mutableMapOf<Int,LFMRequester>()
    private val infoTasks by lazy { mutableMapOf<Int, LFMRequester>() }
    val info by lazy { MutableLiveData<Pair<Int, MusicEntry?>>() }
    var weeklyChart: Chart<MusicEntry>? = null
    var weeklyChartIdx: Int = -1
    var username: String? = null
    var periodIdx = 1
    var page = 1
    var totalCount = 0
    var reachedEnd = false
    var type = 1
//    private var loadedCached = false

    fun loadCharts(page: Int){
        if (periodIdx == 0)
            return
        this.page = page
        lastChartsTasks[type]?.cancel()
//        val command = if (loadedCached) Stuff.GET_RECENTS else Stuff.GET_RECENTS_CACHED
        lastChartsTasks[type] = LFMRequester(getApplication(), viewModelScope, chartsReceiver).apply {
            getCharts(type, Period.values()[periodIdx - 1], page, username)
        }
//        loadedCached = true
    }

    fun loadWeeklyChartsList(scrobblingSince: Long){
        LFMRequester(getApplication(), viewModelScope, weeklyListReceiver)
            .getWeeklyChartsList(username, scrobblingSince)
    }

    fun loadWeeklyCharts() {
        page = 1
        reachedEnd = true
        lastChartsTasks[type]?.cancel()
        lastChartsTasks[type] = LFMRequester(getApplication(), viewModelScope, chartsReceiver).apply {
            getWeeklyCharts(
            type,
            weeklyChart!!.from.time / 1000,
            weeklyChart!!.to.time / 1000,
            username
        )
        }
    }

    fun loadScrobbleCounts(periods: List<ChartsOverviewFragment.ScrobbleCount>) {
        lastChartsTasks[Stuff.TYPE_SC]?.cancel()
        lastChartsTasks[Stuff.TYPE_SC] = LFMRequester(getApplication(), viewModelScope, periodCountReceiver).apply {
            getScrobbleCounts(periods, username)
        }
    }

    fun loadTrackInfo(track: Track, pos: Int) {
        infoTasks[pos]?.cancel()
        infoTasks[pos] = LFMRequester(getApplication(), viewModelScope, info).apply {
            getTrackInfo(track, pos)
        }
    }

    fun loadArtistInfo(artist: Artist, pos: Int) {
        infoTasks[pos]?.cancel()
        infoTasks[pos] = LFMRequester(getApplication(), viewModelScope, info).apply {
            getArtistInfo(artist, pos)
        }
    }

    fun loadAlbumInfo(album: Album, pos: Int) {
        infoTasks[pos]?.cancel()
        infoTasks[pos] = LFMRequester(getApplication(), viewModelScope, info).apply {
            getAlbumInfo(album, pos)
        }
    }

    fun removeInfoTask(pos: Int) {
        val task = infoTasks.remove(pos)
        task?.cancel()
    }

    fun removeAllInfoTasks() {
        infoTasks.keys.toList().forEach {
            removeInfoTask(it)
        }
    }

    fun resetRequestedState() {
        periodCountRequested = false
    }
}