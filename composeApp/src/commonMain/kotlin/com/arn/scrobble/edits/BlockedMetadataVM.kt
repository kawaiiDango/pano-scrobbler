package com.arn.scrobble.edits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.db.PanoDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BlockedMetadataVM : ViewModel() {
    private val dao = PanoDb.db.getBlockedMetadataDao()
    private val _searchTerm = MutableStateFlow("")
    val count = dao.count().stateIn(viewModelScope, SharingStarted.Lazily, 0)
    private var inited = false
    val blockedMetadataFiltered = _searchTerm
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

    fun setFilter(searchTerm: String) {
        viewModelScope.launch {
            _searchTerm.emit(searchTerm)
        }
    }

    fun delete(blockedMetadata: BlockedMetadata) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.delete(blockedMetadata)
        }
    }

}