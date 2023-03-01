package com.arn.scrobble.info

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.LFMRequester
import com.arn.scrobble.SpotifyTrack

class TrackFeaturesVM(application: Application): AndroidViewModel(application) {
    val spotifyTrackWithFeatures = MutableLiveData<SpotifyTrack>()

    fun loadTrackFeatures(artist: String, track: String) {
        LFMRequester(viewModelScope, spotifyTrackWithFeatures)
            .getTrackFeatures(artist, track)
    }
}