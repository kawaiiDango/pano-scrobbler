package com.arn.scrobble.info

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.lastfm.Tag
import com.arn.scrobble.utils.PlatformStuff
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


class TagInfoVM(tag: Tag) : ViewModel() {
    private val _lang = MutableStateFlow<String?>(null)
    private val _info = MutableStateFlow<Tag?>(null)
    val info = _info.asStateFlow()

    init {
        viewModelScope.launch {
            _lang.value = PlatformStuff.mainPrefs.data.map { it.wikiLangs.firstOrNull() }.first()

            _lang.collectLatest { lang ->
                _info.value = Requesters.lastfmUnauthedRequester.getTagInfo(
                    tag.name,
                    lang = lang.takeIf { it != "en" })
                    .getOrDefault(tag)
            }
        }
    }

    fun setLang(lang: String) {
        _lang.value = lang
    }
}