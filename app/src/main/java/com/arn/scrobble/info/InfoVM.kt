package com.arn.scrobble.info

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.arn.scrobble.LFMRequester
import de.umass.lastfm.MusicEntry


class InfoVM(app: Application): AndroidViewModel(app) {
    val info = mutableListOf<Pair<String, MusicEntry?>>()
    val loadedTypes = mutableSetOf<String>()
    val receiver = MutableLiveData<Pair<String, MusicEntry?>>()
    private var lastAsyncTask: LFMRequester.MyAsyncTask? = null

    fun loadInfo(activity: Activity, artist: String, album: String?, track: String?, username: String?) {
        lastAsyncTask = LFMRequester(getApplication()).getInfo(artist, album, track, username, activity, receiver).asAsyncTask()
    }

    fun cancel() {
        lastAsyncTask?.cancel(true)
        lastAsyncTask = null
    }
}