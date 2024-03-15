package com.arn.scrobble.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.main.App
import com.arn.scrobble.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IndexingVM : ViewModel() {
    val indexingProgress by lazy { IndexingWorker.flow(App.context) }
    private val _indexingMessage = MutableStateFlow("")
    val indexingMessage = _indexingMessage.asStateFlow()
    private var _indexingMessageDelayJob: Job? = null

    init {
        preferDeltaIndex()
        _indexingMessageDelayJob = viewModelScope.launch {
            delay(3000)
            _indexingMessage.emit(App.context.getString(R.string.take_long_time))
        }
    }

    fun fullIndex() {
        IndexingWorker.schedule(App.context, true)
    }

    private fun preferDeltaIndex() {
        IndexingWorker.schedule(App.context, false)
    }

    fun setMessage(message: String) {
        viewModelScope.launch {
            _indexingMessage.emit(message)
            _indexingMessageDelayJob?.cancel()
        }
    }

}