package com.arn.scrobble.info

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.lastfm.Tag
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn


class TagInfoVM(tag: Tag) : ViewModel() {
    val info = flow {
        val result = Requesters.lastfmUnauthedRequester.tagGetInfo(tag.name)
            .getOrDefault(tag)
        emit(result)
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

}