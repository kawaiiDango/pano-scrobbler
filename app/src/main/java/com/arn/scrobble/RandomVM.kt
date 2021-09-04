package com.arn.scrobble

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import de.umass.lastfm.Track
import kotlin.random.Random


class RandomVM(app: Application): AndroidViewModel(app) {
    val track = MutableLiveData<RandomTrackData>()
    private val rnd = Random(System.currentTimeMillis())
    var username: String? = null
    var totalScrobbles = -1
    var totalLoves = -1

    fun loadRandomScrobble() {
        LFMRequester(getApplication(), viewModelScope, track).getRandom(Stuff.TYPE_TRACKS, totalScrobbles, rnd, username)
    }

    fun loadRandomLove(){
        LFMRequester(getApplication(), viewModelScope, track).getRandom(Stuff.TYPE_LOVES, totalLoves, rnd, username)
    }

    class RandomTrackData(
        var total: Int,
        var track: Track?,
        var type: Int
        )
}