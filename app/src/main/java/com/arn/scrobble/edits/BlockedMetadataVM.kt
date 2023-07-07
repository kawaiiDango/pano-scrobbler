package com.arn.scrobble.edits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.db.PanoDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BlockedMetadataVM : ViewModel() {
    private val dao = PanoDb.db.getBlockedMetadataDao()
    val blockedMetadata = mutableListOf<BlockedMetadata>()
    val blockedMetadataReceiver = dao.allLd()

    fun delete(index: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.delete(blockedMetadata[index])
        }
    }

}