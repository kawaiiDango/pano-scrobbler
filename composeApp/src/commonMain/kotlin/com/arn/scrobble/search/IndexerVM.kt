package com.arn.scrobble.search

import androidx.lifecycle.ViewModel
import com.arn.scrobble.work.IndexerWork

class IndexerVM : ViewModel() {
    init {
        preferDeltaIndex()
    }

    val indexingProgress by lazy {
        IndexerWork.getProgress()
    }

    fun fullIndex() {
        IndexerWork.schedule(full = true)
    }

    private fun preferDeltaIndex() {
        IndexerWork.schedule(full = false)
    }
}