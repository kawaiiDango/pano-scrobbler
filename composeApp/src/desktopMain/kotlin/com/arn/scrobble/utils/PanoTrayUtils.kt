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
    ) {
        fun copy(
            tooltip: String = this.tooltip,
            argb: IntArray = this.argb,
            iconSize: Int = this.iconSize,
            menuItemIds: Array<String> = this.menuItemIds,
            menuItemTexts: Array<String> = this.menuItemTexts,
        ) = TrayData(
            tooltip = tooltip,
            argb = argb,
            iconSize = iconSize,
            menuItemIds = menuItemIds,
            menuItemTexts = menuItemTexts,
        )
    }

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