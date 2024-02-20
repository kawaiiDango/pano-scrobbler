package com.arn.scrobble.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.App
import com.arn.scrobble.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IndexingVM : ViewModel() {
    val indexingProgress by lazy { IndexingWorker.flow(App.context) }
    private val _indexingMessage = MutableStateFlow("")
    val indexingMessage = _indexingMessage.asStateFlow()

    init {
        preferDeltaIndex()
    }

    fun fullIndex() {
        IndexingWorker.schedule(App.context, true)
        viewModelScope.launch {
            _indexingMessage.emit(App.context.getString(R.string.take_long_time))
        }
    }

    private fun preferDeltaIndex() {
        IndexingWorker.schedule(App.context, false)
        viewModelScope.launch {
            _indexingMessage.emit("")
        }
    }

    fun setMessage(message: String) {
        viewModelScope.launch {
            _indexingMessage.emit(message)
        }
    }

}