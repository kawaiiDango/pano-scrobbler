package com.arn.scrobble.info

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.App
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.ui.MusicEntryLoaderInput
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class InfoExtraFullVM : ViewModel() {
    private val _entries = MutableStateFlow<List<MusicEntry>?>(null)
    val entries = _entries.asStateFlow()
    private val _input = MutableStateFlow<MusicEntryLoaderInput?>(null)
    val input = _input.asStateFlow()
    var reachedEnd = false
    private val limit = 90
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

    fun setInput(input: MusicEntryLoaderInput, initial: Boolean = false) {
        if (initial && _input.value == null || !initial)
            _input.value = input
    }

    private suspend fun loadAppropriateCharts(type: Int, entry: MusicEntry, page: Int) {
        val requester = Requesters.lastfmUnauthedRequester

        val result = when {
            entry is Artist && type == Stuff.TYPE_ARTISTS -> {
                reachedEnd = true
                requester.artistGetSimilar(entry, limit = limit)
            }

            entry is Artist && type == Stuff.TYPE_ALBUMS -> {
                requester.artistGetTopAlbums(entry, page = page, limit = limit)
                    .onSuccess { reachedEnd = page >= it.attr.totalPages }
                    .map { it.entries }
            }

            entry is Artist && type == Stuff.TYPE_TRACKS -> {
                requester.artistGetTopTracks(entry, page = page, limit = limit)
                    .onSuccess { reachedEnd = page >= it.attr.totalPages }
                    .map { it.entries }
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

        result.onFailure {
            App.globalExceptionFlow.emit(it)
        }

        result.onSuccess {
            if (page > 1)
                _entries.emit((_entries.value ?: emptyList()) + it)
            else
                _entries.emit(it)
        }
    }
}
