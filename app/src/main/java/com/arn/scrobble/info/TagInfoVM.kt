package com.arn.scrobble.info

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.Requesters.toFlow
import com.arn.scrobble.api.lastfm.Tag
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


class TagInfoVM : ViewModel() {
    private val _info = MutableSharedFlow<Tag>(replay = 1)
    val info = _info.asSharedFlow()

    private val _tagInfo = MutableStateFlow<Tag?>(null)

    init {
        _tagInfo
            .filterNotNull()
            .flatMapLatest {
                Requesters.lastfmUnauthedRequester.tagGetInfo(it.name).toFlow()
            }
            .onEach { _info.emit(it) }
            .launchIn(viewModelScope)
    }

    fun loadInfoIfNeeded(tag: Tag) {
        viewModelScope.launch {
            _tagInfo.emit(tag)
        }
    }
}