package com.arn.scrobble.utils

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

object PanoTrayUtils {

    private val _onTrayMenuItemClicked = MutableSharedFlow<String>()
    val onTrayMenuItemClicked = _onTrayMenuItemClicked.asSharedFlow()

    fun onTrayMenuItemClickedFn(id: String) {
        GlobalScope.launch {
            _onTrayMenuItemClicked.emit(id)
        }
    }

    enum class ItemId {
        TrackName,
        ArtistName,
        Separator,
        Love,
        Edit,
        Cancel,
        Block,
        Error,
        Open,
        Settings,
        Close;

        fun withSuffix(suffix: String): String {
            return "${this.name}:$suffix"
        }
    }
}