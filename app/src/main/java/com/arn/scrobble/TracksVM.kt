package com.arn.scrobble

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.arn.scrobble.pending.PendingScrJob
import com.arn.scrobble.pending.PendingScrService
import com.arn.scrobble.pending.db.PendingScrobblesDb
import de.umass.lastfm.PaginatedResult
import de.umass.lastfm.Track
import java.util.concurrent.Executors


class TracksVM(application: Application) : AndroidViewModel(application) {
    private val recents by lazy { MutableLiveData<PaginatedResult<Track>>() }
    val deletedTracksStringSet by lazy { mutableSetOf<String>() }
    private val loves by lazy { MutableLiveData<PaginatedResult<Track>>() }
    private val heroInfo by lazy { MutableLiveData<MutableList<String>>() }
    private var lastHeroInfoAsyncTask: LFMRequester.MyAsyncTask? = null
    private val similar by lazy { MutableLiveData<List<Track>>() }
    val trackInfo by lazy { MutableLiveData<Pair<Int,Track>>() }
    private val pendingTracks by lazy { MutableLiveData<PendingListData>() }
    private val executor by lazy { Executors.newSingleThreadExecutor() }
    //for room's built in livedata to work, data must be inserted, deleted from the same dao object
    var page = 1
    private var loadedCachedRecents = false
    private var loadedCachedLoves = false

    fun loadRecentsList(page: Int, reload: Boolean): MutableLiveData<PaginatedResult<Track>> {
        this.page = page
        val command = if (loadedCachedRecents) Stuff.GET_RECENTS else Stuff.GET_RECENTS_CACHED
        if (reload) {
            LFMRequester(command, page.toString()).asAsyncTask(getApplication(), recents)
            loadedCachedRecents = true
        }
        return recents
    }
    fun loadLovesList(page: Int, reload: Boolean): MutableLiveData<PaginatedResult<Track>> {
        this.page = page
        val command = if (loadedCachedLoves) Stuff.GET_LOVES else Stuff.GET_LOVES_CACHED
        if (reload) {
            LFMRequester(command, page.toString()).asAsyncTask(getApplication(), loves)
            loadedCachedLoves = true
        }
        return loves
    }

    fun loadHero(url: String?): MutableLiveData<MutableList<String>> {
        lastHeroInfoAsyncTask?.cancel(true)
        if (url != null) {
            lastHeroInfoAsyncTask =
                    LFMRequester(Stuff.GET_HERO_INFO, url, "").asAsyncTask(getApplication(), heroInfo)
        }
        return heroInfo
    }

    fun loadSimilar(artist: String, track: String, limit: Int): MutableLiveData<List<Track>> {
        LFMRequester(Stuff.GET_SIMILAR, artist, track, limit.toString()).asAsyncTask(getApplication(), similar)
        return similar
    }

    fun loadInfo(artist: String, track: String, pos:Int): MutableLiveData<Pair<Int,Track>> {
        LFMRequester(Stuff.GET_INFO, artist, track, pos.toString()).asSerialAsyncTask(getApplication(), trackInfo)
        return trackInfo
    }

    fun loadPending(limit: Int, submit: Boolean): MutableLiveData<PendingListData> {
        executor.execute{
            val dao = PendingScrobblesDb.getDb(getApplication()).getScrobblesDao()
            val lovesDao = PendingScrobblesDb.getDb(getApplication()).getLovesDao()
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
                    && Main.isOnline && !PendingScrService.mightBeRunning && !PendingScrJob.mightBeRunning) {
                val intent = Intent(getApplication<Application>().applicationContext, PendingScrService::class.java)
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
//                    getApplication<Application>().startForegroundService(intent) //sometimes causes ANR
//                else
                getApplication<Application>().startService(intent)
            }
        }
        return pendingTracks
    }

}