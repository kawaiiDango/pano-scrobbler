package com.arn.scrobble.info

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.LastFm
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.main.App
import com.arn.scrobble.pref.HistoryPref
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch


class UserTagsVM : ViewModel() {
    private val prefs = App.prefs
    val historyPref by lazy {
        HistoryPref(
            App.prefs.sharedPreferences,
            MainPrefs.PREF_ACTIVITY_TAG_HISTORY,
            20
        )
    }

    private val entry = MutableStateFlow<MusicEntry?>(null)

    private val _tags = MutableStateFlow<Set<String>?>(null)
    val tags = _tags.asStateFlow()

    init {
        populateTagHistoryIfNeeded()

        historyPref.load()

        viewModelScope.launch {
            entry.filterNotNull()
                .mapLatest {
                    (Scrobblables.current as? LastFm)
                        ?.getUserTagsFor(entry.value!!)
                        ?.map {
                            it.toptags.tag.map { it.name }
                                .toSet()
                        }
                        ?.getOrNull() ?: emptySet()
                }
                .collectLatest { _tags.emit(it) }
        }
    }

    fun setEntry(musicEntry: MusicEntry) {
        viewModelScope.launch {
            entry.emit(musicEntry)
        }
    }

    fun deleteTag(tag: String) {
        viewModelScope.launch {
            _tags.emit(tags.value?.filter { it != tag }?.toSet())

            (Scrobblables.current as? LastFm)
                ?.removeUserTagFor(entry.value!!, tag)

        }
    }

    fun addTag(newTags: String) {
        viewModelScope.launch {
            _tags.emit(tags.value?.plus(splitTags(newTags)))

            if (Stuff.isTestLab)
                return@launch

            (Scrobblables.current as? LastFm)
                ?.addUserTagsFor(entry.value!!, splitTags(newTags).joinToString(","))
        }
    }

    private fun populateTagHistoryIfNeeded() {
        if (prefs.userTopTagsFetched) return

        viewModelScope.launch {
            (Scrobblables.current as? LastFm)
                ?.userGetTopTags(limit = 20)
                ?.map { it.toptags.tag }
                ?.onSuccess {
                    it.reversed()
                        .forEach {
                            historyPref.add(it.name)
                        }
                    historyPref.save()
                    prefs.userTopTagsFetched = true
                }
        }
    }

    fun splitTags(tags: String) = tags.split(",").map { it.trim() }
}