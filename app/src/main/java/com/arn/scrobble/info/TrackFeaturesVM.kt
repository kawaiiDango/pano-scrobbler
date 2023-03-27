package com.arn.scrobble.info

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.LFMRequester
import com.arn.scrobble.SpotifyTrack

class TrackFeaturesVM: ViewModel() {
    val spotifyTrackWithFeatures = MutableLiveData<SpotifyTrack>()

    fun loadTrackFeatures(artist: String, track: String) {
        LFMRequester(viewModelScope, spotifyTrackWithFeatures)
            .getTrackFeatures(artist, track)
    }
}