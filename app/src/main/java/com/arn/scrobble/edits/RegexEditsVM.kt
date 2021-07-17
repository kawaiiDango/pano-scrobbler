package com.arn.scrobble.edits

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.RegexEdit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RegexEditsVM(app: Application): AndroidViewModel(app) {
    val dao = PanoDb.getDb(getApplication()).getRegexEditsDao()
    val regexes = mutableListOf<RegexEdit>()
    val regexesReceiver = dao.allLd
    val countReceiver = dao.countLd

    fun upsert(e: RegexEdit) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insert(e)
        }
    }

    fun upsertAll(el: List<RegexEdit>) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insert(el)
        }
    }

    fun delete(index: Int) {
        val regexEdit = regexes[index]
        viewModelScope.launch(Dispatchers.IO) {
            dao.delete(regexEdit)
        }
    }

}