package com.arn.scrobble.edits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.App
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.RegexEdit
import com.arn.scrobble.db.RegexEditsDao.Companion.performRegexReplace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegexEditsTestVM : ViewModel() {
    private val dao = PanoDb.db.getRegexEditsDao()
    val pm by lazy { App.context.packageManager!! }
    private val _scrobbleData = MutableStateFlow<ScrobbleData?>(null)
    val scrobbleData = _scrobbleData.asStateFlow()
    private val _regexMatches = MutableStateFlow<Map<String, Set<RegexEdit>>?>(null)
    val regexMatches = _regexMatches.asStateFlow()
    val hasPkgName = dao.hasPkgNameFlow().stateIn(viewModelScope, SharingStarted.Lazily, false)
    private val _pkgNameSelected = MutableStateFlow<String?>(null)
    val pkgNameSelected = _pkgNameSelected.asStateFlow()

    init {
        scrobbleData
            .debounce(500)
            .onEach { sd ->

                if (sd == null) {
                    _regexMatches.emit(null)
                } else {
                    _regexMatches.emit(
                        withContext(Dispatchers.IO) {
                            dao.performRegexReplace(sd, pkgNameSelected.value)
                        }
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun setScrobbleData(sd: ScrobbleData?) {
        viewModelScope.launch {
            _scrobbleData.emit(sd)
        }
    }

    fun setPkgName(pkgName: String?) {
        viewModelScope.launch {
            _pkgNameSelected.emit(pkgName)
        }
    }
}