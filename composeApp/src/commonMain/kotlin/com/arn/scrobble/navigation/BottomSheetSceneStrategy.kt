@file:OptIn(ExperimentalMaterial3Api::class)

package com.arn.scrobble.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.rememberLifecycleOwner
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import com.arn.scrobble.icons.Close
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.automirrored.ArrowBack
import com.arn.scrobble.navigation.BottomSheetSceneStrategy.Companion.bottomSheet
import com.arn.scrobble.themes.LocalThemeAttributes
import com.arn.scrobble.ui.ApplyWindowBlur
import com.arn.scrobble.ui.isImeVisible
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.back
import pano_scrobbler.composeapp.generated.resources.close

// THANKS I HATE IT

private data object BottomSheetKey : NavMetadataKey<Unit>

/** An [OverlayScene] that renders an [entry] within a [ModalBottomSheet]. */
internal data class BottomSheetScene<T : Any>(
    private val isStandalone: Boolean,
    override val key: T,
    override val previousEntries: List<NavEntry<T>>,
    override val overlaidEntries: List<NavEntry<T>>,
    private val entry: NavEntry<T>,
    private val sheetState: SheetState,
    private val onDismissRequest: () -> Unit,
    private val onBack: () -> Unit,
) : OverlayScene<T> {

    override val entries: List<NavEntry<T>> = listOf(entry)

    override val content: @Composable (() -> Unit) = {
        val lifecycleOwner = rememberLifecycleOwner()
        val canGoBack = previousEntries.lastOrNull()?.metadata?.get(BottomSheetKey) != null

        BottomSheetDialogParent(
            isStandalone = isStandalone,
            sheetState = sheetState,
            onDismissRequest = onDismissRequest,
            onBack = if (canGoBack) {
                onBack
            } else {
                null
            },
        ) {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                entry.Content()
            }
        }
    }
}


@Composable
private fun BottomSheetDialogParent(
    isStandalone: Boolean,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onBack: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetGesturesEnabled = !PlatformStuff.isTv && !PlatformStuff.isDesktop
    val isImeVisible = isImeVisible()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        dragHandle = null,
        sheetGesturesEnabled = sheetGesturesEnabled && !isImeVisible,
        sheetState = sheetState,
        tonalElevation = 2.dp,
        modifier = Modifier
            .windowInsetsPadding(
                WindowInsets.statusBars
                    .only(WindowInsetsSides.Top)
                    .add(WindowInsets(top = 42.dp))
            ),
    ) {
        if (LocalThemeAttributes.current.blurSubWindow ||
            isStandalone && LocalThemeAttributes.current.blurMainWindow
        )
            ApplyWindowBlur(behind = 0, bg = Stuff.BLUR_BACKDROP_RADIUS_DP)
        // there can be only one window blur at a time per task, according to android source
        // behind is already used by the main window, use bg to make them stack

        if (onBack != null) {
            OutlinedIconButton(
                onClick = onBack,
                modifier = Modifier.padding(4.dp)
                    .align(Alignment.CenterHorizontally),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.ArrowBack,
                    contentDescription = stringResource(Res.string.back),
                )
            }

        } else if (!sheetGesturesEnabled && !PlatformStuff.isTv) {
            // there isn't much vertical space on a TV
            OutlinedIconButton(
                onClick = {
                    scope.launch {
                        sheetState.hide()
                        onDismissRequest()
                    }
                },
                modifier = Modifier.padding(4.dp)
                    .align(Alignment.CenterHorizontally),
            ) {
                Icon(
                    imageVector = Icons.Close,
                    contentDescription = stringResource(Res.string.close),
                )
            }
        } else {
            // reserve space at the top
            Box(
                modifier = Modifier
                    .height(24.dp)
            )
        }
        content()
    }
}

/**
 * A [SceneStrategy] that displays entries that have added [bottomSheet] to their [NavEntry.metadata]
 * within a [ModalBottomSheet] instance.
 *
 * This strategy should always be added before any non-overlay scene strategies.
 */
class BottomSheetSceneStrategy<T : Any>(
    private val isStandalone: Boolean,
    private val sheetState: SheetState,
    private val onDismiss: () -> Unit
) : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val lastEntry = entries.lastOrNull()
        val isBottomSheet = lastEntry?.metadata?.get(BottomSheetKey) != null

        return if (isBottomSheet)
            @Suppress("UNCHECKED_CAST")
            BottomSheetScene(
                isStandalone = isStandalone,
                key = lastEntry.contentKey as T,
                previousEntries = entries.dropLast(1),
                overlaidEntries = entries.filterNot { it.metadata[BottomSheetKey] != null },
                entry = lastEntry,
                sheetState,
                onDismissRequest = onDismiss,
                onBack = onBack
            )
        else
            null
    }

    companion object {
        fun bottomSheet() = metadata {
            put(BottomSheetKey, Unit)
        }
    }
}