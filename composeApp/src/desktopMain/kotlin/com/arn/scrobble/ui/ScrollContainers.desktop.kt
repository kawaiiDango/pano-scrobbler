package com.arn.scrobble.ui

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.move_left
import pano_scrobbler.composeapp.generated.resources.move_right
import kotlin.math.max
import kotlin.math.min

private val scrollbarSize = 12.dp

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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    val scrollButtonsPadding = if (canScroll) 50.dp else 0.dp

    Box(
        modifier = modifier
    ) {
        if (canScroll) {
            FilledTonalIconButton(
                enabled = state.canScrollBackward,
                onClick = {
                    scope.launch {
                        val lastFullyVisibleIdx =
                            state.layoutInfo.visibleItemsInfo.findLast { it.offset <= state.layoutInfo.viewportEndOffset }?.index
                                ?: 0

                        val firstFullyVisibleIdx =
                            state.layoutInfo.visibleItemsInfo.find { it.offset >= state.layoutInfo.viewportStartOffset }?.index
                                ?: 0

                        val targetIdx =
                            max(
                                firstFullyVisibleIdx - (lastFullyVisibleIdx - firstFullyVisibleIdx) - 1,
                                0
                            )

                        state.animateScrollToItem(targetIdx)
                    }
                },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = stringResource(Res.string.move_left),
                )
            }
        }

        LazyRow(
            state = state,
            contentPadding = contentPadding,
            reverseLayout = reverseLayout,
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = verticalAlignment,
            modifier = Modifier.padding(
                vertical = scrollbarSize,
                horizontal = scrollButtonsPadding
            ),
            content = content
        )

        if (canScroll) {
            FilledTonalIconButton(
                enabled = state.canScrollForward,
                onClick = {
                    scope.launch {
                        val lastFullyVisibleIdx =
                            state.layoutInfo.visibleItemsInfo.findLast { it.offset <= state.layoutInfo.viewportEndOffset }?.index
                                ?: 0

                        val targetIdx =
                            min(lastFullyVisibleIdx + 1, state.layoutInfo.totalItemsCount - 1)

                        state.animateScrollToItem(targetIdx)
                    }
                },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = stringResource(Res.string.move_right),
                )
            }
        }

        HorizontalScrollbar(
            modifier = Modifier.align(Alignment.BottomStart)
                .padding(horizontal = scrollButtonsPadding)
                .fillMaxWidth(),
            adapter = rememberScrollbarAdapter(state)
        )
    }
}

@Composable
actual fun OptionalHorizontalScrollbar(
    state: ScrollState,
    modifier: Modifier,
) {
    HorizontalScrollbar(
        modifier = modifier
            .fillMaxWidth(),
        adapter = rememberScrollbarAdapter(state)
    )
}