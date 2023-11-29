package com.arn.scrobble.info

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.spotify.SpotifyTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class TrackFeaturesVM : ViewModel() {
    private val _spotifyTrackWithFeatures = MutableStateFlow<SpotifyTrack?>(null)
    val spotifyTrackWithFeatures = _spotifyTrackWithFeatures.asSharedFlow()
    private val _track = MutableStateFlow<Track?>(null)
    private val _hasLoaded = MutableStateFlow(false)
    val hasLoaded = _hasLoaded.asStateFlow()

    init {
        _track.filterNotNull().onEach { track ->
            val spotifyTrack = Requesters.spotifyRequester.getSpotifyTrack(track)?.let {
                val features = Requesters.spotifyRequester.getTrackFeatures(it.id)
                if (features.getOrNull() != null) {
                    it.copy(features = features.getOrThrow())
                } else {
                    it
                }
            }
            _hasLoaded.emit(true)
            
            _spotifyTrackWithFeatures.emit(spotifyTrack)

        }
            .launchIn(viewModelScope)
    }

    fun loadTrackFeaturesIfNeeded(track: Track) {
        viewModelScope.launch {
            _track.emit(track)
        }
    }
}