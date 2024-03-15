package com.arn.scrobble.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.spotify.SpotifySearchResponse
import com.arn.scrobble.api.spotify.SpotifySearchType
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.doOnSuccessLoggingFaliure
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class ImageSearchVM : ViewModel() {
    private val _searchTerm = MutableSharedFlow<Pair<String, Int>>()
    private val _searchResults = MutableStateFlow<SpotifySearchResponse?>(null)
    val searchResults = _searchResults.asSharedFlow()

    init {
        viewModelScope.launch {
            _searchTerm
                .distinctUntilChanged()
                .debounce(500)
                .collectLatest { (term, searchType) ->
                    val results = when (searchType) {
                        Stuff.TYPE_ALBUMS ->
                            Requesters.spotifyRequester.search(term, SpotifySearchType.album, LIMIT)

                        Stuff.TYPE_ARTISTS -> Requesters.spotifyRequester.search(
                            term,
                            SpotifySearchType.artist,
                            LIMIT
                        )

                        else -> throw IllegalArgumentException("Invalid search type: $searchType")
                    }
                    results.doOnSuccessLoggingFaliure {
                        _searchResults.emit(it)
                    }
                }
        }
    }

    fun search(term: String, searchType: Int) {
        viewModelScope.launch {
            _searchTerm.emit(term to searchType)
        }
    }

    companion object {
        private const val LIMIT = 40
    }
}
