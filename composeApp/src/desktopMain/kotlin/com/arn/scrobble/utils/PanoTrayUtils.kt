package com.arn.scrobble.utils

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

object PanoTrayUtils {
    class TrayData(
        val tooltip: String,
        val argb: IntArray,
        val iconSize: Int,
        val menuItemIds: Array<String>,
        val menuItemTexts: Array<String>,
    )

    private val _onTrayMenuItemClicked = MutableSharedFlow<String>(replay = 1)
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
        Copy,
        Error,
        Open,
        Exit;

        fun withSuffix(suffix: String): String {
            return "${this.name}:$suffix"
        }
    }
}