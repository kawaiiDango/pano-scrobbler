package com.arn.scrobble

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.arn.scrobble.pending.PendingScrJob
import com.arn.scrobble.pending.PendingScrService
import com.arn.scrobble.pending.db.PendingScrobblesDb
import de.umass.lastfm.ImageSize
import de.umass.lastfm.PaginatedResult
import de.umass.lastfm.Track
import java.util.concurrent.Executors


class TracksVM(application: Application) : AndroidViewModel(application) {
    val tracksReceiver by lazy { MutableLiveData<PaginatedResult<Track>>() }
    val tracks by lazy { mutableListOf<Track>() }
    val deletedTracksStringSet by lazy { mutableSetOf<String>() }
    val heroInfo by lazy { MutableLiveData<MutableList<String>>() }
    private var lastHeroInfoAsyncTask: LFMRequester.MyAsyncTask? = null
    val similarReceiver by lazy { MutableLiveData<List<Track>>() }
    val trackInfo by lazy { MutableLiveData<Pair<Int,Track?>>() }
    val imgMap = mutableMapOf<Int, Map<ImageSize, String>>()
    private val pendingTracks by lazy { MutableLiveData<PendingListData>() }
    private val executor by lazy { Executors.newSingleThreadExecutor() }
    //for room's built in livedata to work, data must be inserted, deleted from the same dao object
    var username: String? = null
    var page = 1
    var totalPages = 1
    var loadedCached = false
    var loadedNw = false
    var selectedPos = 1
    var toTime = 0L


    fun loadRecents(page: Int) {
        this.page = page
        LFMRequester(getApplication()).getRecents(page, toTime, !loadedCached, username).asAsyncTask(tracksReceiver)
        loadedCached = true
    }

    fun loadLoves(page: Int) {
        this.page = page
        LFMRequester(getApplication()).getLoves(page, !loadedCached, username).asAsyncTask(tracksReceiver)
        loadedCached = true
    }

    fun loadHero(url: String?): MutableLiveData<MutableList<String>> {
        lastHeroInfoAsyncTask?.cancel(true)
        if (url != null) {
            lastHeroInfoAsyncTask = LFMRequester(getApplication()).getHeroInfo(url).asAsyncTask(heroInfo)
        }
        return heroInfo
    }

    fun loadSimilar(artist: String, track: String, limit: Int) {
        LFMRequester(getApplication()).getSimilar(artist, track, limit).asAsyncTask(similarReceiver)
    }

    fun loadInfo(track: Track, pos:Int) {
        LFMRequester(getApplication()).getTrackInfo(track, pos).asAsyncTask(trackInfo)
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