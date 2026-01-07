package com.arn.scrobble.navigation

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver

inline fun <reified T : Enum<T>> enumSaver(): Saver<MutableState<T>, String> = Saver(
    save = {
        it.value.name
    },
    restore = {
        mutableStateOf(enumValueOf<T>(it))
    }
)
