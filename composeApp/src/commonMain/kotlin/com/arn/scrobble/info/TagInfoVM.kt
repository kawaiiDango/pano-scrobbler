package com.arn.scrobble.info

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.lastfm.Tag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class TagInfoVM : ViewModel() {
    private val _tagInfo = MutableStateFlow<Tag?>(null)
    val info = _tagInfo
        .filterNotNull()
        .mapLatest {
            Requesters.lastfmUnauthedRequester.tagGetInfo(it.name)
                .getOrDefault(it)
        }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun loadInfoIfNeeded(tag: Tag) {
        viewModelScope.launch {
            _tagInfo.emit(tag)
        }
    }
}