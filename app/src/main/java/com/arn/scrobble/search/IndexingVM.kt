package com.arn.scrobble.search

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.arn.scrobble.App
import com.arn.scrobble.R

class IndexingVM : ViewModel() {
    val indexingProgress by lazy { IndexingWorker.livedata(App.context) }
    val indexingMessage by lazy { MutableLiveData("") }

    init {
        preferDeltaIndex()
    }

    fun fullIndex() {
        IndexingWorker.schedule(App.context, true)
        indexingMessage.value = App.context.getString(R.string.take_long_time)
    }

    fun preferDeltaIndex() {
        IndexingWorker.schedule(App.context, false)
        indexingMessage.value = ""
    }


}