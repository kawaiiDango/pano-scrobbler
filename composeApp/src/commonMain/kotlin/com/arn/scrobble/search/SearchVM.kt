package com.arn.scrobble.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.lastfm.SearchResults
import com.arn.scrobble.api.lastfm.SearchType
import com.arn.scrobble.db.CachedAlbum.Companion.toAlbum
import com.arn.scrobble.db.CachedArtist.Companion.toArtist
import com.arn.scrobble.db.CachedTrack.Companion.toTrack
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext


class SearchVM : ViewModel() {
    private val _searchTerm = MutableSharedFlow<Pair<String, SearchType>>()
    private val _searchResults = MutableStateFlow<SearchResults?>(null)
    val searchResults = _searchResults.asSharedFlow()
    private val _hasLoaded = MutableStateFlow(false)
    val hasLoaded = _hasLoaded.asStateFlow()

    init {
        viewModelScope.launch {
            _searchTerm
                .distinctUntilChanged()
                .debounce(500)
                .collectLatest { (term, searchType) ->
                    if (term.length < 3)
                        return@collectLatest

                    val results = when (searchType) {
                        SearchType.GLOBAL -> {
                            _hasLoaded.value = false
                            val r = Requesters.lastfmUnauthedRequester.search(term)
                            _hasLoaded.value = true
                            r
                        }

                        SearchType.LOCAL -> withContext(Dispatchers.IO) {
                            getLocalSearches(term)
                        }
                    }

                    results.onSuccess {
                        _searchResults.emit(it)
                    }.onFailure { e ->
                        Stuff.globalExceptionFlow.emit(e)
                    }
                }
        }
    }

    fun search(term: String, searchType: SearchType) {
        viewModelScope.launch {
            _searchTerm.emit(term to searchType)
        }
    }

    private suspend fun getLocalSearches(term: String) = supervisorScope {
        val db = PanoDb.db

        val artists = async {
            kotlin.runCatching {
                db.getCachedArtistsDao().find(term).map { it.toArtist() }
            }
        }.await()
        val albums = async {
            kotlin.runCatching {
                db.getCachedAlbumsDao().find(term).map { it.toAlbum() }
            }
        }.await()
        val tracks = async {
            kotlin.runCatching {
                db.getCachedTracksDao().findTop(term).map { it.toTrack() }
            }
        }.await()
        val lovedTracks = async {
            kotlin.runCatching {
                db.getCachedTracksDao().findLoved(term).map { it.toTrack() }
            }
        }.await()

        if (artists.isFailure || albums.isFailure || tracks.isFailure || lovedTracks.isFailure)
            return@supervisorScope Result.failure(
                artists.exceptionOrNull()
                    ?: albums.exceptionOrNull()
                    ?: tracks.exceptionOrNull()
                    ?: lovedTracks.exceptionOrNull()!!
            )

        val sr = SearchResults(
            term,
            SearchType.LOCAL,
            lovedTracks.getOrDefault(listOf()),
            tracks.getOrDefault(listOf()),
            artists.getOrDefault(listOf()),
            albums.getOrDefault(listOf()),
        )

        Result.success(sr)
    }

}