package com.arn.scrobble.navigation

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import com.arn.scrobble.utils.Stuff

inline fun <reified T> jsonSerializableSaver(): Saver<MutableState<T>, String> = Saver(
    save = { state ->
        Stuff.myJson.encodeToString(state.value)
    },
    restore = { jsonString ->
        val obj = Stuff.myJson.decodeFromString<T>(jsonString)
        mutableStateOf(obj)
    }
)
