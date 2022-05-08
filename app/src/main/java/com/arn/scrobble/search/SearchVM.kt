package com.arn.scrobble.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.LFMRequester
import com.arn.scrobble.ui.SectionedVirtualList
import de.umass.lastfm.Album
import de.umass.lastfm.Artist
import de.umass.lastfm.Track
import kotlinx.coroutines.Job


class SearchVM(app: Application) : AndroidViewModel(app) {
    val searchResults by lazy { MutableLiveData<SearchResults>() }
    val virtualList = SectionedVirtualList()
    val indexingProgress by lazy { MutableLiveData<Double>(null) }
    private var searchJob: LFMRequester? = null

    fun loadSearches(term: String, searchType: SearchResultsExperimentAdapter.SearchType) {
        searchJob?.cancel()
        searchJob = LFMRequester(getApplication(), viewModelScope, searchResults).apply {
            when (searchType) {
                SearchResultsExperimentAdapter.SearchType.GLOBAL -> getSearches(term)
                SearchResultsExperimentAdapter.SearchType.LOCAL -> getLocalSearches(term)
            }
        }
    }

    fun fullIndex() {
        if (indexingProgress.value == null) {
            indexingProgress.value = 0.0
            LFMRequester(getApplication(), viewModelScope, indexingProgress)
                .runFullIndex()
        }
    }

    class SearchResults(
        val term: String,
        val searchType: SearchResultsExperimentAdapter.SearchType,
        val lovedTracks: List<Track>,
        val tracks: List<Track>,
        val artists: List<Artist>,
        val albums: List<Album>,
    ) {
        val isEmpty: Boolean
            get() = lovedTracks.isEmpty() && tracks.isEmpty() && artists.isEmpty() && albums.isEmpty()
    }
}