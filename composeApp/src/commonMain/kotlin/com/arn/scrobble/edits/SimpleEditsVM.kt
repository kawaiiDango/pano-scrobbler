package com.arn.scrobble.edits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.SimpleEdit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SimpleEditsVM : ViewModel() {
    private val dao = PanoDb.db.getSimpleEditsDao()
    private val _searchTerm = MutableStateFlow("")
    val count = dao.count().stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        0
    )
    private var inited = false
    val simpleEditsFiltered = _searchTerm
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
        _searchTerm.value = searchTerm.trim()
    }

//    fun upsert(simpleEdit: SimpleEdit) {
//        viewModelScope.launch(Dispatchers.IO) {
//            dao.insertReplaceLowerCase(simpleEdit)
//        }
//    }

    fun delete(simpleEdit: SimpleEdit) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.delete(simpleEdit)
        }
    }

}