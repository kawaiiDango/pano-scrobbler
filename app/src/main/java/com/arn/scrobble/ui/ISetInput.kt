package com.arn.scrobble.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface ISetInput {
    val _input: MutableStateFlow<MusicEntryLoaderInput?>
    val input: StateFlow<MusicEntryLoaderInput?>
}

fun ISetInput.setInput(inputp: MusicEntryLoaderInput, initial: Boolean = false) {
    if (initial && _input.value == null || !initial)
        _input.value = inputp
}