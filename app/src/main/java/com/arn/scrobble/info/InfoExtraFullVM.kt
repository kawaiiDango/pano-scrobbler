package com.arn.scrobble.info

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.Requesters.toFlow
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.ui.MusicEntryLoaderInput
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class InfoExtraFullVM : ViewModel() {
    private val _entries = MutableStateFlow<List<MusicEntry>?>(null)
    val entries = _entries.asStateFlow()
    private val _input = MutableStateFlow<MusicEntryLoaderInput?>(null)
    val input = _input.asStateFlow()
    var reachedEnd = false
    private val limit = 100
    private val _hasLoaded = MutableStateFlow(false)
    val hasLoaded = _hasLoaded.asStateFlow()

    init {
        viewModelScope.launch {
            input.filterNotNull()
                .collectLatest {
                    loadAppropriateCharts(
                        type = it.type,
                        entry = it.entry!!,
                        page = it.page,
                    )
                }
        }
    }

    fun setInput(input: MusicEntryLoaderInput) {
        viewModelScope.launch {
            _input.emit(input)
        }
    }

    private suspend fun loadAppropriateCharts(type: Int, entry: MusicEntry, page: Int) {
        val requester = Requesters.lastfmUnauthedRequester

        val result = when {
            entry is Artist && type == Stuff.TYPE_ARTISTS -> {
                reachedEnd = true
                requester.artistGetSimilar(entry, limit = limit)
            }

            entry is Artist && type == Stuff.TYPE_ALBUMS -> {
                requester.artistGetTopAlbums(entry, page = page, limit = limit).map { it.entries }
            }

            entry is Artist && type == Stuff.TYPE_TRACKS -> {
                requester.artistGetTopTracks(entry, page = page, limit = limit).map { it.entries }
            }

            entry is Track && type == Stuff.TYPE_TRACKS -> {
                reachedEnd = true
                requester.trackGetSimilar(entry, limit = limit)
            }

            else -> {
                throw IllegalArgumentException("Unknown type $type")
            }
        }

        _hasLoaded.emit(true)
        _entries.emitAll(result.toFlow())
    }
}
