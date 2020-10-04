package com.arn.scrobble.charts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.arn.scrobble.LFMRequester
import de.umass.lastfm.*


class ChartsVM(application: Application) : AndroidViewModel(application) {
    val chartsData by lazy { mutableListOf<MusicEntry>() }
    val imgMap by lazy { mutableMapOf<Int,String>() }
    val chartsReceiver by lazy { MutableLiveData<Pair<Int,Collection<MusicEntry>>>() }
    val weeklyListReceiver by lazy { MutableLiveData<List<Chart<MusicEntry>>>() }
    private var lastChartsAsyncTask: LFMRequester.MyAsyncTask? = null
    private val infoAsyncTasks by lazy { mutableMapOf<Int, LFMRequester.MyAsyncTask>() }
    val info by lazy { MutableLiveData<Pair<Int, MusicEntry?>>() }
    var weeklyChart: Chart<MusicEntry>? = null
    var periodIdx = 1
    var page = 1
    var reachedEnd = false
//    private var loadedCached = false

    fun loadCharts(type: Int, page: Int, username: String){
        if (periodIdx == 0)
            return
        this.page = page
        lastChartsAsyncTask?.cancel(true)
//        val command = if (loadedCached) Stuff.GET_RECENTS else Stuff.GET_RECENTS_CACHED
        lastChartsAsyncTask = LFMRequester(getApplication()).getCharts(type, Period.values()[periodIdx - 1], page, username)
                .asAsyncTask(chartsReceiver)
//        loadedCached = true
    }

    fun loadWeeklyChartsList(username: String){
        LFMRequester(getApplication()).getWeeklyChartsList(username).asAsyncTask(weeklyListReceiver)
    }

    fun loadWeeklyCharts(type: Int, username: String) {
        page = 1
        reachedEnd = true
        lastChartsAsyncTask?.cancel(true)
        lastChartsAsyncTask = LFMRequester(getApplication()).getWeeklyCharts(type, weeklyChart!!.from.time/1000, weeklyChart!!.to.time/1000, username)
                .asAsyncTask(chartsReceiver)
    }

    fun loadTrackInfo(track: Track, pos: Int) {
        infoAsyncTasks[pos]?.cancel(true)
        infoAsyncTasks[pos] = LFMRequester(getApplication()).getTrackInfo(track, pos).asAsyncTask(info)
    }

    fun loadArtistInfo(artist: Artist, pos: Int) {
        infoAsyncTasks[pos]?.cancel(true)
        infoAsyncTasks[pos] = LFMRequester(getApplication()).getArtistInfo(artist, pos).asAsyncTask(info)
    }

    fun loadAlbumInfo(album: Album, pos: Int) {
        infoAsyncTasks[pos]?.cancel(true)
        infoAsyncTasks[pos] = LFMRequester(getApplication()).getAlbumInfo(album, pos).asAsyncTask(info)
    }

    fun removeInfoTask(pos: Int) {
        val task = infoAsyncTasks.remove(pos)
        task?.cancel(true)
    }

    fun removeAllInfoTasks() {
        infoAsyncTasks.keys.toList().forEach {
            removeInfoTask(it)
        }
    }
}