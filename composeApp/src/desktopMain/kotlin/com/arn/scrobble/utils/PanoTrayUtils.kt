package com.arn.scrobble.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object PanoTrayUtils {
    class TrayData(
        val tooltip: String,
        val argb: IntArray,
        val iconSize: Int,
        val menuItemIds: Array<String>,
        val menuItemTexts: Array<String>,
    )

    private val _onTrayMenuItemClicked =
        MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)
    val onTrayMenuItemClicked = _onTrayMenuItemClicked.asSharedFlow()

    fun onTrayMenuItemClickedFn(id: String) {
        _onTrayMenuItemClicked.tryEmit(id)
    }

    enum class ItemId {
        TrackName,
        ArtistName,
        AlbumName,
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