package com.arn.scrobble.edits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.RegexEdit
import com.arn.scrobble.utils.PlatformStuff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RegexEditsVM : ViewModel() {
    private val dao = PanoDb.db.getRegexEditsDao()
    val regexes = dao.allFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun upsertAll(el: List<RegexEdit>) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insert(el)
        }
    }

    fun updatePreset(preset: RegexPreset, isEnabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            PlatformStuff.mainPrefs.updateData {
                if (isEnabled)
                    it.copy(regexPresets = it.regexPresets + preset.name)
                else
                    it.copy(regexPresets = it.regexPresets - preset.name)
            }
        }
    }
}