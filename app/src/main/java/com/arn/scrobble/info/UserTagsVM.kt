package com.arn.scrobble.info

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.LFMRequester
import com.arn.scrobble.pref.HistoryPref
import de.umass.lastfm.MusicEntry


class UserTagsVM(app: Application) : AndroidViewModel(app) {
    lateinit var entry: MusicEntry
    lateinit var historyPref: HistoryPref
    val tags = MutableLiveData<MutableSet<String>>()

    fun loadTags() {
        LFMRequester(getApplication(), viewModelScope, tags).getUserTagsForEntry(entry, historyPref)
    }

    fun deleteTag(tag: String) {
        tags.value = tags.value?.filter { it != tag }?.toMutableSet()
        LFMRequester(getApplication(), viewModelScope).deleteUserTagsForEntry(entry, tag)
    }

    fun addTag(newTags: String) {
        tags.value = tags.value?.apply { this += splitTags(newTags) }
        LFMRequester(getApplication(), viewModelScope).addUserTagsForEntry(entry, newTags)
    }

    fun splitTags(tags: String) = tags.split(",").map { it.trim() }
}