package com.arn.scrobble.ui

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonDefaults.IconButtonWidthOption
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.arn.scrobble.icons.ArrowLeftAutoMirrored
import com.arn.scrobble.icons.ArrowRightAutoMirrored
import com.arn.scrobble.icons.Icons
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.move_left
import pano_scrobbler.composeapp.generated.resources.move_right
import kotlin.math.max
import kotlin.math.min


@Composable
actual fun PanoLazyColumn(
    state: LazyListState,
    contentPadding: PaddingValues,
    reverseLayout: Boolean,
    verticalArrangement: Arrangement.Vertical,
    horizontalAlignment: Alignment.Horizontal,
    modifier: Modifier,
    content: LazyListScope.() -> Unit,
) {
    val scrollbarSize = LocalScrollbarStyle.current.thickness

    Box(
        modifier = modifier
    ) {
        LazyColumn(
            state = state,
            contentPadding = contentPadding,
            reverseLayout = reverseLayout,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            modifier = Modifier.padding(end = scrollbarSize),
            content = content
        )

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(
                scrollState = state
            )
        )
    }
}

@Composable
actual fun PanoLazyVerticalGrid(
    state: LazyGridState,
    columns: GridCells,
    contentPadding: PaddingValues,
    reverseLayout: Boolean,
    verticalArrangement: Arrangement.Vertical,
    horizontalArrangement: Arrangement.Horizontal,
    modifier: Modifier,
    content: LazyGridScope.() -> Unit,
) {
    val scrollbarSize = LocalScrollbarStyle.current.thickness

    Box(
        modifier = modifier
    ) {
        LazyVerticalGrid(
            state = state,
            columns = columns,
            contentPadding = contentPadding,
            reverseLayout = reverseLayout,
            verticalArrangement = verticalArrangement,
            horizontalArrangement = horizontalArrangement,
            modifier = Modifier.padding(end = scrollbarSize),
            content = content
        )

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(
                scrollState = state
            )
        )
    }
}

@Composable
actual fun PanoLazyRow(
    state: LazyListState,
    contentPadding: PaddingValues,
    reverseLayout: Boolean,
    horizontalArrangement: Arrangement.Horizontal,
    verticalAlignment: Alignment.Vertical,
    modifier: Modifier,
    content: LazyListScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val canScroll = state.canScrollForward || state.canScrollBackward
    val scrollButtonSize = if (canScroll)
        IconButtonDefaults.extraSmallContainerSize(IconButtonWidthOption.Wide)
    else
        DpSize.Zero

    fun scroll(forward: Boolean) {
        val lastFullyVisibleIdx =
            state.layoutInfo.visibleItemsInfo.findLast {
                it.offset + it.size <= state.layoutInfo.viewportEndOffset
            }?.index
                ?: 0

        val targetIdx = if (forward) {
            min(lastFullyVisibleIdx + 1, state.layoutInfo.totalItemsCount - 1)
        } else {
            val firstFullyVisibleIdx =
                state.layoutInfo.visibleItemsInfo.find { it.offset >= state.layoutInfo.viewportStartOffset }?.index
                    ?: 0
            max(
                firstFullyVisibleIdx - (lastFullyVisibleIdx - firstFullyVisibleIdx) - 1,
                0
            )
        }

        scope.launch {
            state.animateScrollToItem(targetIdx)
        }
    }

    Box(
        modifier = modifier
    ) {
        LazyRow(
            state = state,
            contentPadding = contentPadding + PaddingValues(
                bottom = scrollButtonSize.height
            ),
            reverseLayout = reverseLayout,
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = verticalAlignment,
            content = content
        )

        if (canScroll) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .align(Alignment.BottomCenter),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalIconButton(
                    shapes = IconButtonDefaults.shapes(),
                    enabled = state.canScrollBackward,
                    onClick = {
                        scroll(false)
                    },
                    modifier = Modifier
                        .requiredSize(scrollButtonSize)
                        .padding(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.ArrowLeftAutoMirrored,
                        contentDescription = stringResource(Res.string.move_left),
                    )
                }

                HorizontalScrollbar(
                    modifier = Modifier.weight(1f),
                    adapter = rememberScrollbarAdapter(state)
                )

                FilledTonalIconButton(
                    shapes = IconButtonDefaults.shapes(),
                    enabled = state.canScrollForward,
                    onClick = {
                        scroll(true)
                    },
                    modifier = Modifier
                        .size(scrollButtonSize)
                        .padding(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.ArrowRightAutoMirrored,
                        contentDescription = stringResource(Res.string.move_right),
                    )
                }
            }
        }
    }
}

@Composable
actual fun OptionalHorizontalScrollbar(
    state: ScrollState,
    modifier: Modifier,
) {
    HorizontalScrollbar(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        adapter = rememberScrollbarAdapter(state)
    )
}