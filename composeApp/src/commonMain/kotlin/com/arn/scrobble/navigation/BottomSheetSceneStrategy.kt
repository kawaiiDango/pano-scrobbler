package com.arn.scrobble.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import com.arn.scrobble.icons.Close
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.automirrored.ArrowBack
import com.arn.scrobble.navigation.BottomSheetSceneStrategy.Companion.bottomSheet
import com.arn.scrobble.utils.PlatformStuff
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.back
import pano_scrobbler.composeapp.generated.resources.close

// THANKS I HATE IT

/** An [OverlayScene] that renders an [entry] within a [ModalBottomSheet]. */

private const val BOTTOM_SHEET_KEY = "bottomsheet"

@OptIn(ExperimentalMaterial3Api::class)
internal class BottomSheetScene<T : Any>(
    override val key: T,
    override val previousEntries: List<NavEntry<T>>,
    override val overlaidEntries: List<NavEntry<T>>,
    private val entry: NavEntry<T>,
    private val properties: PanoModalProperties,
    private val onDismissRequest: () -> Unit,
    private val onBack: () -> Unit,
) : OverlayScene<T> {

    override val entries: List<NavEntry<T>> = listOf(entry)

    override val content: @Composable (() -> Unit) = {
        BottomSheetDialogParent(
            onDismissRequest = onDismissRequest,
            onBack = if (previousEntries.lastOrNull()?.metadata?.contains(BOTTOM_SHEET_KEY) == true) {
                onBack
            } else {
                null
            },
            isNestedScrollable = properties.nestedScrollable,
            forceSkipPartiallyExpanded = true,
        ) { entry.Content() }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheetDialogParent(
    onDismissRequest: () -> Unit,
    onBack: (() -> Unit)?,
    isNestedScrollable: Boolean, // disabling nested scrolling is a workaround until google fixes it
    forceSkipPartiallyExpanded: Boolean,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
//    val sheetGesturesEnabled = !PlatformStuff.isTv && !PlatformStuff.isDesktop &&
//            (
//                    (!scrollState.canScrollBackward && !scrollState.canScrollForward) ||
//                            (scrollState.lastScrolledBackward && !scrollState.canScrollBackward) ||
//                            (scrollState.lastScrolledForward && !scrollState.canScrollForward)
//                    )

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = forceSkipPartiallyExpanded || PlatformStuff.isTv || PlatformStuff.isDesktop
    )

    val isMobile = !PlatformStuff.isTv && !PlatformStuff.isDesktop

    val sheetGesturesEnabled by remember {
        derivedStateOf {
            isNestedScrollable && isMobile &&
                    !scrollState.canScrollForward && !scrollState.canScrollBackward ||
                    !isNestedScrollable && isMobile
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        dragHandle = null,
        sheetGesturesEnabled = sheetGesturesEnabled,
        sheetState = sheetState,
    ) {
        if (onBack != null) {
            IconButton(
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
            // there isn't much vertical space on a tv
            IconButton(
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
                    .minimumInteractiveComponentSize()
            )
        }

        CompositionLocalProvider(
            LocalModalScrollProps provides
                    ModalScrollProps(
                        scrollState,
                        isNestedScrollable && !sheetGesturesEnabled
                    )
        ) {
            content()
        }
    }
}

/**
 * A [SceneStrategy] that displays entries that have added [bottomSheet] to their [NavEntry.metadata]
 * within a [ModalBottomSheet] instance.
 *
 * This strategy should always be added before any non-overlay scene strategies.
 */
@OptIn(ExperimentalMaterial3Api::class)
class BottomSheetSceneStrategy<T : Any>(
    private val onDismiss: () -> Unit
) : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val lastEntry = entries.lastOrNull()
        val bottomSheetProperties =
            lastEntry?.metadata?.get(BOTTOM_SHEET_KEY) as? PanoModalProperties

        return bottomSheetProperties?.let { properties ->
            @Suppress("UNCHECKED_CAST")
            BottomSheetScene(
                key = lastEntry.contentKey as T,
                previousEntries = entries.dropLast(1),
                overlaidEntries = entries.filterNot { BOTTOM_SHEET_KEY in it.metadata },
                entry = lastEntry,
                properties = properties,
                onDismissRequest = onDismiss,
                onBack = onBack
            )
        }
    }

    companion object {
        /**
         * Function to be called on the [NavEntry.metadata] to mark this entry as something that
         * should be displayed within a [ModalBottomSheet].
         *
         * @param modalBottomSheetProperties properties that should be passed to the containing
         * [ModalBottomSheet].
         */
        @OptIn(ExperimentalMaterial3Api::class)
        fun bottomSheet(
            properties: PanoModalProperties = PanoModalProperties()
        ): Map<String, Any> = mapOf(BOTTOM_SHEET_KEY to properties)

    }
}