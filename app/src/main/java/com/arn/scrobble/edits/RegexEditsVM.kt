package com.arn.scrobble.edits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.RegexEdit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RegexEditsVM : ViewModel() {
    val dao = PanoDb.db.getRegexEditsDao()
    val regexes = mutableListOf<RegexEdit>()
    val regexesReceiver = dao.allLd
    val countReceiver = dao.countLd

    fun upsertAll(el: List<RegexEdit>) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insert(el)
        }
    }
}