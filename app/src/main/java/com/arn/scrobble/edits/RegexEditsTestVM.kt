package com.arn.scrobble.edits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.RegexEditsDao.Companion.performRegexReplace
import com.arn.scrobble.main.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegexEditsTestVM : ViewModel() {
    private val dao = PanoDb.db.getRegexEditsDao()
    val pm by lazy { App.application.packageManager!! }
    private val _scrobbleData = MutableStateFlow<ScrobbleData?>(null)
    val scrobbleData = _scrobbleData.asStateFlow()
    val regexMatches = scrobbleData
        .debounce(500)
        .mapLatest { sd ->
            if (sd == null) {
                null
            } else {
                withContext(Dispatchers.IO) {
                    dao.performRegexReplace(sd, pkgNameSelected.value)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)
    val hasPkgName = dao.hasPkgNameFlow().stateIn(viewModelScope, SharingStarted.Lazily, false)
    private val _pkgNameSelected = MutableStateFlow<String?>(null)
    val pkgNameSelected = _pkgNameSelected.asStateFlow()

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