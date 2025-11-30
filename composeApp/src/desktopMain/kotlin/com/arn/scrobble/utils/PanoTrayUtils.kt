package com.arn.scrobble.utils

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object PanoTrayUtils {
    data class TrayData(
        val tooltip: String,
        val bitmap: ImageBitmap,
        val iconSize: Int,
        val menuItemIds: List<String>,
        val menuItemTexts: List<String>,
    )

    data class TrayClickEvent(
        val x: Int,
        val y: Int,
        val button: Int,
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
        Update,
        DiscordRpcDisabled,
        Exit;

        fun withSuffix(suffix: String): String {
            return "${this.name}:$suffix"
        }
    }
}