package com.arn.scrobble

import android.app.Application
import android.content.Intent
import android.os.AsyncTask
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.arn.scrobble.pending.PendingScrService
import com.arn.scrobble.pending.db.PendingScrobble
import com.arn.scrobble.pending.db.PendingScrobblesDb
import de.umass.lastfm.PaginatedResult
import de.umass.lastfm.Track
import java.util.concurrent.Executors


class TracksVM(application: Application) : AndroidViewModel(application) {
    private val recents = MutableLiveData<PaginatedResult<Track>>()
    private val loves = MutableLiveData<PaginatedResult<Track>>()
    private val heroInfo = MutableLiveData<MutableList<String>>()
    private var lastHeroInfoAsyncTask: AsyncTask<*,*,*>? = null
    private val similar = MutableLiveData<List<Track>>()
    val trackInfo = MutableLiveData<Pair<Int,Track>>()
    private val pendingTracks = MutableLiveData<Pair<List<PendingScrobble>, Int>>()
    private val executor = Executors.newSingleThreadExecutor()
    private val dao = PendingScrobblesDb.getDb(application).getScrobblesDao()
    private val lovesDao = PendingScrobblesDb.getDb(application).getLovesDao()
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

    fun loadPending(limit: Int): MutableLiveData<Pair<List<PendingScrobble>, Int>> {
        executor.execute{
            val p = Pair(dao.all(limit), dao.count)
            pendingTracks.postValue(p)
            if ((p.second > 0 || lovesDao.count > 0)
                    && Main.isOnline && !PendingScrService.mightBeRunning) {
                val intent = Intent(getApplication<Application>().applicationContext, PendingScrService::class.java)
                getApplication<Application>().startService(intent)
            }
        }
        return pendingTracks
    }

}