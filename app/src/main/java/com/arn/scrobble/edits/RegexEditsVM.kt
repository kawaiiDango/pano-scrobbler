package com.arn.scrobble.edits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.RegexEdit
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RegexEditsVM : ViewModel() {
    private val dao = PanoDb.db.getRegexEditsDao()
    private val _regexes = MutableStateFlow<List<RegexEdit>?>(null)
    val regexes = _regexes.asStateFlow()
    val limitReached = dao.count()
        .mapLatest { it > Stuff.MAX_PATTERNS }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)
    val presetsAvailable = dao.allPresets().mapLatest {
        (RegexPresets.presetKeys - it.map { it.preset!! }.toSet()).toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    init {
        viewModelScope.launch {
            dao.allFlow().collectLatest {
                _regexes.value = it
            }
        }
    }

    fun tmpUpdateAll(el: List<RegexEdit>) {
        _regexes.value = el
    }

    fun upsertAll(el: List<RegexEdit>) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insert(el)
        }
    }

    fun insertPreset(preset: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val regexEdit = RegexEdit(preset = preset, order = 0)
            dao.shiftDown()
            dao.insert(listOf(regexEdit))
        }
    }
}