package com.arn.scrobble.info

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.LFMRequester
import de.umass.lastfm.MusicEntry


class InfoVM(app: Application) : AndroidViewModel(app) {
    val info = mutableListOf<Pair<String, MusicEntry?>>()
    val loadedTypes = mutableSetOf<String>()
    val receiver = MutableLiveData<Pair<String, MusicEntry?>>()
    private var lastTask: LFMRequester? = null

    fun loadInfo(artist: String, album: String?, track: String?, username: String?) {
        lastTask = LFMRequester(getApplication(), viewModelScope, receiver).apply {
            getInfo(artist, album, track, username)
        }
    }

    fun cancel() {
        lastTask?.cancel()
        lastTask = null
    }
}