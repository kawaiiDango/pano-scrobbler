package com.arn.scrobble.edits

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.App
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.RegexEdit
import com.arn.scrobble.db.RegexEditsDao.Companion.performRegexReplace
import de.umass.lastfm.scrobble.ScrobbleData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class RegexEditsTestVM : ViewModel() {
    private val dao = PanoDb.db.getRegexEditsDao()
    private val mutex = Mutex()
    val scrobbleData = MutableLiveData<ScrobbleData>(null)
    val regexMatches = MutableLiveData<Map<String, Set<RegexEdit>>?>(null)
    val hasPkgName by lazy { dao.hasPkgNameLd() }
    val pkgNameSelected = MutableLiveData<String>(null)
    val pm by lazy { App.context.packageManager!! }

    fun performRegexReplace() {
        val sd = scrobbleData.value ?: return
        viewModelScope.launch {
            mutex.withLock {
                regexMatches.value = withContext(Dispatchers.IO) {
                    dao.performRegexReplace(sd, pkgNameSelected.value)
                }
            }
        }
    }
}