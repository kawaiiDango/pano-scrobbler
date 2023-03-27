package com.arn.scrobble.edits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.SimpleEdit
import com.arn.scrobble.db.SimpleEditsDao.Companion.insertReplaceLowerCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SimpleEditsVM : ViewModel() {
    val dao = PanoDb.db.getSimpleEditsDao()
    val edits = mutableListOf<SimpleEdit>()
    val editsReceiver = dao.allLd

    fun upsert(simpleEdit: SimpleEdit) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertReplaceLowerCase(simpleEdit)
        }
    }

    fun delete(index: Int) {
        val edit = edits[index]
        viewModelScope.launch(Dispatchers.IO) {
            dao.delete(edit)
        }
    }

}