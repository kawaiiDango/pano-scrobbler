package com.arn.scrobble

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.charts.TimePeriod
import de.umass.lastfm.MusicEntry
import de.umass.lastfm.Period


class RandomVM : ViewModel() {
    val data = MutableLiveData<RandomMusicData>()
    val error = MutableLiveData<Throwable>()
    var username: String? = null
    private var totalScrobbles = -1
    private var totalLoves = -1
    private var totalArtists = -1
    private var totalAlbums = -1
    var isLoading = false
    var timePeriod: TimePeriod = TimePeriod(Period.OVERALL)
        set(value) {
            field = value
            resetTotals()
        }

    private fun getTotal(type: Int): Int {
        return when (type) {
            Stuff.TYPE_TRACKS -> totalScrobbles
            Stuff.TYPE_LOVES -> totalLoves
            Stuff.TYPE_ARTISTS -> totalArtists
            Stuff.TYPE_ALBUMS -> totalAlbums
            else -> throw IllegalArgumentException("Unknown type $type")
        }
    }

    fun setTotal(type: Int, total: Int) {
        when (type) {
            Stuff.TYPE_TRACKS -> totalScrobbles = total
            Stuff.TYPE_LOVES -> totalLoves = total
            Stuff.TYPE_ARTISTS -> totalArtists = total
            Stuff.TYPE_ALBUMS -> totalAlbums = total
            else -> throw IllegalArgumentException("Unknown type $type")
        }
    }

    fun loadRandom(type: Int) {
        LFMRequester(viewModelScope, liveData = data, errorLiveData = error)
            .getRandom(
                type,
                getTotal(type),
                username,
                timePeriod
            )
    }

    private fun resetTotals() {
        totalScrobbles = -1
        totalArtists = -1
        totalAlbums = -1
    }

    class RandomMusicData(
        val total: Int,
        val entry: MusicEntry?,
        val type: Int
    )
}