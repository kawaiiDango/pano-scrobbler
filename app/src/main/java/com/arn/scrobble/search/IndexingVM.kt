package com.arn.scrobble.search

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.App
import com.arn.scrobble.LFMRequester
import com.arn.scrobble.R
import com.hadilq.liveevent.LiveEvent

class IndexingVM : ViewModel() {
    val indexingProgress by lazy { MutableLiveData<Double>(null) }
    val indexingError by lazy { LiveEvent<Throwable>() }
    val indexingMessage by lazy { MutableLiveData("") }
    private val prefs = App.prefs
    private var lastTask: LFMRequester? = null

    init {
        if (prefs.lastMaxIndexTime == null) // first time
            fullIndex()
        else
            deltaIndex()
    }

    fun fullIndex() {
        lastTask?.cancel()
        indexingProgress.value = 0.0
        indexingMessage.value = App.context.getString(R.string.take_long_time)
        lastTask = LFMRequester(viewModelScope, indexingProgress, indexingError)
            .apply {
                runFullIndex()
            }
    }

    fun deltaIndex() {
        lastTask?.cancel()
        indexingProgress.value = 0.0
        indexingMessage.value = ""
        lastTask = LFMRequester(viewModelScope, indexingProgress, indexingError)
            .apply {
                runDeltaIndex()
            }
    }


}