package com.arn.scrobble.help

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.decodeFromStream
import pano_scrobbler.composeapp.generated.resources.Res

class HelpVM : ViewModel() {
    private val _searchTerm = MutableStateFlow("")
    private var inited = false
    private val currentPlatform = when {
        PlatformStuff.isDesktop -> FaqPlatform.desktop
        PlatformStuff.isTv -> FaqPlatform.tv
        else -> FaqPlatform.android
    }

    val faqs = flow {
        val f = Stuff.myJson.decodeFromStream<List<FaqItem>>(
            Res.readBytes("files/faq.json").inputStream()
        ).filter { it.platform == null || it.platform == currentPlatform }

        emit(f)
    }
        .combine(_searchTerm.debounce {
            if (!inited) {
                inited = true
                0
            } else {
                500L
            }
        }) { faqs, term ->
            if (term.isBlank()) faqs
            else faqs.filter {
                it.question.contains(term, ignoreCase = true) ||
                        it.answer.contains(term, ignoreCase = true)
            }

        }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun setFilter(searchTerm: String) {
        _searchTerm.value = searchTerm.trim()
    }
}