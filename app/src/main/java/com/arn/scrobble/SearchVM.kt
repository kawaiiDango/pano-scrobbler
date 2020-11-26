package com.arn.scrobble

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import de.umass.lastfm.Album
import de.umass.lastfm.Artist
import de.umass.lastfm.Track
import kotlin.random.Random


class SearchVM(app: Application): AndroidViewModel(app) {
    val searchResults by lazy { MutableLiveData<SearchResults>() }
    val history = mutableListOf<String>()

    fun loadSearches(term: String) {
        LFMRequester(getApplication()).getSearches(term).asAsyncTask(searchResults)
    }

    class SearchResults (
            val term: String,
            val artists: List<Artist>,
            val albums: List<Album>,
            val tracks: List<Track>
    )
}