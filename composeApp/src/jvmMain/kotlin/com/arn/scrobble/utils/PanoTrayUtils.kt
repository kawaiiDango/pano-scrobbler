package com.arn.scrobble.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ColorFilter
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint

object PanoTrayUtils {
    enum class TrayIconType {
        NOT_PLAYING,
        PLAYING,
        ERROR;
    }

    data class TrayData(
        val tooltip: String,
        val iconType: TrayIconType,
        val iconIsDark: Boolean,
        val iconSize: Int,
        val menuItemIds: List<String>,
        val menuItemTexts: List<String>,
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
        Settings,
        Update,
        DiscordRpcDisabled,
        Exit;

        fun withSuffix(suffix: String): String {
            return "${this.name}:$suffix"
        }
    }

    @Composable
    fun rememberTrayIcons(
        trayIcons: Triple<ByteArray, ByteArray, ByteArray>,
        isDark: Boolean
    ): Triple<ImageBitmap, ImageBitmap, ImageBitmap> {
        return remember(isDark) {
            val trayIconPlaying = trayIcons.first.decodeToImageBitmap()
            val trayIconNotPlaying = trayIcons.second.decodeToImageBitmap()
            val trayIconError = trayIcons.third.decodeToImageBitmap()

            if (isDark) {
                Triple(trayIconPlaying, trayIconNotPlaying, trayIconError)
            } else {
                Triple(trayIconPlaying.white(), trayIconNotPlaying.white(), trayIconError.white())
            }
        }
    }

    private fun ImageBitmap.white(): ImageBitmap {
        val skiaBitmap = this.asSkiaBitmap()
        val image = Image.makeFromBitmap(skiaBitmap)

        val paint = Paint().apply {
            colorFilter = ColorFilter.makeBlend(0xFFFFFFFF.toInt(), BlendMode.SRC_IN)
        }

        val outputBitmap = Bitmap().apply {
            allocN32Pixels(skiaBitmap.width, skiaBitmap.height)
        }
        val canvas = Canvas(outputBitmap)
        canvas.drawImage(image, 0f, 0f, paint)

        return outputBitmap.asComposeImageBitmap()
    }
}