package com.arn.scrobble.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.LFMRequester
import de.umass.lastfm.Album
import de.umass.lastfm.Artist
import de.umass.lastfm.Track


class SearchVM(app: Application) : AndroidViewModel(app) {
    val searchResults by lazy { MutableLiveData<SearchResults>() }

    fun loadSearches(term: String) {
        LFMRequester(getApplication(), viewModelScope, searchResults).getSearches(term)
    }

    class SearchResults(
        val term: String,
        val artists: List<Artist>,
        val albums: List<Album>,
        val tracks: List<Track>
    )
}