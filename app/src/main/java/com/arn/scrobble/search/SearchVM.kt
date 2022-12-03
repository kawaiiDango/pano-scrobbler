package com.arn.scrobble.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.LFMRequester
import com.arn.scrobble.ui.SectionedVirtualList
import com.hadilq.liveevent.LiveEvent
import de.umass.lastfm.Album
import de.umass.lastfm.Artist
import de.umass.lastfm.Track


class SearchVM(app: Application) : AndroidViewModel(app) {
    val searchResults by lazy { MutableLiveData<SearchResults>() }
    val virtualList = SectionedVirtualList()
    val indexingProgress by lazy { MutableLiveData<Double>(null) }
    val indexingError by lazy { LiveEvent<Throwable>() }
    private var searchJob: LFMRequester? = null

    fun loadSearches(term: String, searchType: SearchResultsAdapter.SearchType) {
        searchJob?.cancel()
        searchJob = LFMRequester(getApplication(), viewModelScope, searchResults).apply {
            when (searchType) {
                SearchResultsAdapter.SearchType.GLOBAL -> getSearches(term)
                SearchResultsAdapter.SearchType.LOCAL -> getLocalSearches(term)
            }
        }
    }

    fun fullIndex() {
        if (indexingProgress.value == null) {
            indexingProgress.value = 0.0
            LFMRequester(getApplication(), viewModelScope, indexingProgress, indexingError)
                .runFullIndex()
        }
    }

    fun deltaIndex() {
        if (indexingProgress.value == null) {
            indexingProgress.value = 0.0
            LFMRequester(getApplication(), viewModelScope, indexingProgress, indexingError)
                .runDeltaIndex()
        }
    }

    class SearchResults(
        val term: String,
        val searchType: SearchResultsAdapter.SearchType,
        val lovedTracks: List<Track>,
        val tracks: List<Track>,
        val artists: List<Artist>,
        val albums: List<Album>,
    ) {
        val isEmpty: Boolean
            get() = lovedTracks.isEmpty() && tracks.isEmpty() && artists.isEmpty() && albums.isEmpty()
    }
}