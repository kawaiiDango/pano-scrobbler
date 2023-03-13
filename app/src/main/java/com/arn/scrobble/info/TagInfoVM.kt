package com.arn.scrobble.info

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.LFMRequester
import de.umass.lastfm.Tag


class TagInfoVM : ViewModel() {
    val info = MutableLiveData<Pair<Tag?, List<Tag>?>>()

    fun loadInfo(tag: String) {
        LFMRequester(viewModelScope, info).getTagInfo(tag)
    }
}