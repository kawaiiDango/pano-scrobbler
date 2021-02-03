package com.arn.scrobble.info

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.arn.scrobble.LFMRequester
import de.umass.lastfm.MusicEntry


class UserTagsVM(app: Application): AndroidViewModel(app) {
    lateinit var entry: MusicEntry
    val tags = MutableLiveData<MutableSet<String>>()

    fun loadTags() {
        LFMRequester(getApplication()).getUserTagsForEntry(entry).asAsyncTask(tags)
    }

    fun deleteTag(tag: String) {
        tags.value = tags.value?.filter { it != tag }?.toMutableSet()
        LFMRequester(getApplication()).deleteUserTagsForEntry(entry, tag).asAsyncTask()
    }

    fun addTag(newTags: String) {
        tags.value = tags.value?.apply { this += splitTags(newTags)}
        LFMRequester(getApplication()).addUserTagsForEntry(entry, newTags).asAsyncTask()
    }

    fun splitTags(tags: String) = tags.split(",").map { it.trim() }
}