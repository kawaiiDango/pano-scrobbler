package com.arn.scrobble.edits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.db.ArtistWithDelimiters
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.utils.FirstArtistExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ArtistsWithDelimitersVM : ViewModel() {
    private val dao = PanoDb.db.getArtistsWithDelimitersDao()
    private val _searchTerm = MutableStateFlow("")

    private var inited = false
    private var count = 0
    val artistsFiltered = _searchTerm
        .debounce {
            if (!inited) {
                inited = true
                0
            } else {
                500L
            }
        }
        .flatMapLatest { term ->
            withContext(Dispatchers.IO) {
                if (term.isBlank())
                    dao.allFlow()
                else
                    dao.searchPartial(term)
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            null
        )

    val searchTermToFirstArtist = _searchTerm
        .debounce {
            if (!inited) {
                inited = true
                0
            } else {
                500L
            }
        }
        .combine(dao.allFlow()) { term, all ->
            term to FirstArtistExtractor.extract(
                artistString = term,
                useAnd = true,
                updatedUserAllowlist = if (all.size != count) {
                    count = all.size
                    all
                } else
                    null
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            null
        )


    fun setFilter(searchTerm: String) {
        _searchTerm.value = searchTerm.trim()
    }

    fun insert(artist: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insert(ArtistWithDelimiters(artist = artist))
        }
    }

    fun delete(artist: ArtistWithDelimiters) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.delete(artist)
        }
    }

}