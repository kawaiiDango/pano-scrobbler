package com.arn.scrobble.pref

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import org.jetbrains.compose.resources.ExperimentalResourceApi
import pano_scrobbler.composeapp.generated.resources.Res

class TranslatorsVM : ViewModel() {
    // read each line from the raw resource file and collect them into a list

    @OptIn(ExperimentalResourceApi::class)
    val translators = flow {
        Res.readBytes("files/crowdin_members.txt")
            .decodeToString()
            .lines()
            .let { emit(it) }
    }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}