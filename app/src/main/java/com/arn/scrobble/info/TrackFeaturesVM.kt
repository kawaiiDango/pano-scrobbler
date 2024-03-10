package com.arn.scrobble.info

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.spotify.SpotifySearchType
import com.arn.scrobble.api.spotify.TrackWithFeatures
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class TrackFeaturesVM : ViewModel() {
    private val _spotifyTrackWithFeatures = MutableStateFlow<TrackWithFeatures?>(null)
    val spotifyTrackWithFeatures = _spotifyTrackWithFeatures.asSharedFlow()
    private val _track = MutableStateFlow<Track?>(null)
    private val _hasLoaded = MutableStateFlow(false)
    val hasLoaded = _hasLoaded.asStateFlow()

    init {
        viewModelScope.launch {
            _track.filterNotNull().collectLatest { track ->
                Requesters.spotifyRequester.search(
                    "${track.artist.name} ${track.name}",
                    SpotifySearchType.track
                ).onSuccess {
                    it.tracks?.items?.firstOrNull()?.let { spotifyTrack ->
                        if (spotifyTrack.artists.first().name == track.artist.name &&
                            spotifyTrack.name == track.name
                        ) {
                            val features =
                                Requesters.spotifyRequester.trackFeatures(spotifyTrack.id)

                            _spotifyTrackWithFeatures.emit(
                                TrackWithFeatures(spotifyTrack, features.getOrNull())
                            )
                        }
                    }
                }


                _hasLoaded.emit(true)
            }
        }
    }

    fun loadTrackFeaturesIfNeeded(track: Track) {
        viewModelScope.launch {
            _track.emit(track)
        }
    }
}