package com.arn.scrobble.edits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.RegexEdit
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RegexEditsVM : ViewModel() {
    private val dao = PanoDb.db.getRegexEditsDao()
    val regexes = dao.allFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)
    val limitReached = dao.count()
        .mapLatest { it > Stuff.MAX_PATTERNS }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)
    val presetsAvailable = dao.allPresets().mapLatest {
        (RegexPresets.presetKeys - it.map { it.preset!! }.toSet()).toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

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