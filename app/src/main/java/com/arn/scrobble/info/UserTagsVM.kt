package com.arn.scrobble.info

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.LFMRequester
import com.arn.scrobble.Stuff
import com.arn.scrobble.pref.HistoryPref
import de.umass.lastfm.MusicEntry


class UserTagsVM : ViewModel() {
    lateinit var entry: MusicEntry
    lateinit var historyPref: HistoryPref
    val tags = MutableLiveData<Set<String>>()

    fun loadTags() {
        LFMRequester(viewModelScope, tags).getUserTagsForEntry(entry, historyPref)
    }

    fun deleteTag(tag: String) {
        tags.value = tags.value?.filter { it != tag }?.toSet()
        LFMRequester(viewModelScope).deleteUserTagsForEntry(entry, tag)
    }

    fun addTag(newTags: String) {
        tags.value = tags.value?.also { it + splitTags(newTags) }

        if (Stuff.isTestLab)
            return

        LFMRequester(viewModelScope).addUserTagsForEntry(entry, newTags)
    }

    fun splitTags(tags: String) = tags.split(",").map { it.trim() }
}