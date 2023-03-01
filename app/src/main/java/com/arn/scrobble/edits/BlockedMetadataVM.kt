package com.arn.scrobble.edits

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.db.PanoDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BlockedMetadataVM(app: Application) : AndroidViewModel(app) {
    private val dao = PanoDb.db.getBlockedMetadataDao()
    val blockedMetadata = mutableListOf<BlockedMetadata>()
    val blockedMetadataReceiver = dao.allLd

    fun delete(index: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.delete(blockedMetadata[index])
        }
    }

}