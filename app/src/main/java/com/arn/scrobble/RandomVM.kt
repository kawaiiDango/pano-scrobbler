package com.arn.scrobble

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import de.umass.lastfm.Track
import kotlin.random.Random


class RandomVM(app: Application): AndroidViewModel(app) {
    val track = MutableLiveData<RandomTrackData>()
    private val rnd = Random(System.currentTimeMillis())
    var totalScrobbles = -1
    var totalLoves = -1

    fun loadRandomScrobble() {
        LFMRequester(getApplication()).getRandom(Stuff.TYPE_TRACKS, totalScrobbles, rnd).asAsyncTask(track)
    }

    fun loadRandomLove(){
        LFMRequester(getApplication()).getRandom(Stuff.TYPE_LOVES, totalLoves, rnd).asAsyncTask(track)
    }

    class RandomTrackData(
        var total: Int,
        var track: Track?,
        var type: Int
        )
}