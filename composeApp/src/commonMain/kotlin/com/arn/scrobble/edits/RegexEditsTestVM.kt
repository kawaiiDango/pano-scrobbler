package com.arn.scrobble.edits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.RegexEditsDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

class RegexEditsTestVM : ViewModel() {
    private val dao = PanoDb.db.getRegexEditsDao()
    private val _scrobbleDataInput = MutableStateFlow<ScrobbleData?>(null)

    val regexResults =
        _scrobbleDataInput
            .debounce(500)
            .filterNotNull()
            .combine(dao.enabledFlow()) { sd, regexEdits ->
                if (sd.track.isEmpty() || sd.artist.isEmpty()) {
                    null
                } else {
                    withContext(Dispatchers.IO) {
                        RegexEditsDao.performRegexReplace(sd, regexEdits)
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun setScrobbleData(sd: ScrobbleData?) {
        _scrobbleDataInput.value = sd
    }
}