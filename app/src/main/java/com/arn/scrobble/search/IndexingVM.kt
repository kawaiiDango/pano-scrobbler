package com.arn.scrobble.search

import androidx.lifecycle.ViewModel
import com.arn.scrobble.PlatformStuff

class IndexingVM : ViewModel() {
    val indexingProgress by lazy { IndexingWorker.flow(PlatformStuff.application) }

    init {
        preferDeltaIndex()
    }

    fun fullIndex() {
        IndexingWorker.schedule(PlatformStuff.application, true)
    }

    private fun preferDeltaIndex() {
        IndexingWorker.schedule(PlatformStuff.application, false)
    }
}