package com.arn.scrobble.edits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.RegexEditsDao.Companion.performRegexReplace
import com.arn.scrobble.pref.AppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

class RegexEditsTestVM : ViewModel() {
    private val dao = PanoDb.db.getRegexEditsDao()
    private val _scrobbleData = MutableStateFlow<ScrobbleData?>(null)
    private val _appListItem = MutableStateFlow<AppItem?>(null)
    val hasPkgName = dao.hasPkgNameFlow().stateIn(viewModelScope, SharingStarted.Lazily, false)

    val regexResults =
        combine(_scrobbleData, hasPkgName, _appListItem) { sd, hasPkgName, appListItem ->
            Triple(
                sd,
                hasPkgName,
                appListItem
            )
        }
            .debounce(500)
            .mapLatest { (sd, hasPkgName, appListItem) ->
                if ((hasPkgName && appListItem == null) || sd == null ||
                    (sd.track.isEmpty() && sd.album.isNullOrEmpty() && sd.artist.isEmpty() && sd.albumArtist.isNullOrEmpty())
                ) {
                    null
                } else {
                    withContext(Dispatchers.IO) {
                        dao.performRegexReplace(sd, appListItem?.appId)
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun setScrobbleData(sd: ScrobbleData?) {
        _scrobbleData.value = sd
    }

    fun setApp(appListItemp: AppItem?) {
        _appListItem.value = appListItemp
    }
}