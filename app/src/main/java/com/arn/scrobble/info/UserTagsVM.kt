package com.arn.scrobble.info

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.LastFm
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class UserTagsVM : ViewModel() {
    private val entry = MutableStateFlow<MusicEntry?>(null)

    private val _tags = MutableStateFlow<Set<String>?>(null)
    val tags = _tags.asStateFlow()
    val tagHistory = PlatformStuff.mainPrefs.data.map { it.tagHistory }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        emptyList()
    )

    init {
        viewModelScope.launch {
            populateTagHistoryIfNeeded()

            entry.filterNotNull()
                .mapLatest {
                    (Scrobblables.current.value as? LastFm)
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

            (Scrobblables.current.value as? LastFm)
                ?.removeUserTagFor(entry.value!!, tag)

        }
    }

    fun addTag(newTags: String) {
        viewModelScope.launch {
            val newTagsList = splitTags(newTags)
            _tags.emit(tags.value?.plus(newTagsList))

            if (Stuff.isTestLab)
                return@launch

            PlatformStuff.mainPrefs.updateData {
                it.copy(
                    tagHistory = (it.tagHistory + newTagsList).distinct()
                        .take(Stuff.MAX_HISTORY_ITEMS)
                )
            }

            (Scrobblables.current.value as? LastFm)
                ?.addUserTagsFor(entry.value!!, newTagsList.joinToString(","))
        }
    }

    private suspend fun populateTagHistoryIfNeeded() {
        val mainPrefs = PlatformStuff.mainPrefs
        if (mainPrefs.data.map { it.userTopTagsFetched }.first()) return

        (Scrobblables.current.value as? LastFm)
            ?.userGetTopTags(limit = 20)
            ?.map { it.toptags.tag }
            ?.onSuccess {
                val tags = it.reversed().map { it.name }
                mainPrefs.updateData {
                    it.copy(tagHistory = tags, userTopTagsFetched = true)
                }
            }
    }

    fun splitTags(tags: String) = tags.split(",").map { it.trim() }
}