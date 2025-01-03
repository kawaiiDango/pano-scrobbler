package com.arn.scrobble.ui

import androidx.compose.foundation.HorizontalScrollbar
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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

    ) {
        LazyColumn(
            state = state,
            contentPadding = contentPadding,
            reverseLayout = reverseLayout,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            modifier = modifier.padding(end = scrollbarSize),
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

    ) {
        LazyVerticalGrid(
            state = state,
            columns = columns,
            contentPadding = contentPadding,
            reverseLayout = reverseLayout,
            verticalArrangement = verticalArrangement,
            horizontalArrangement = horizontalArrangement,
            modifier = modifier.padding(end = scrollbarSize),
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
    Box(

    ) {
        LazyRow(
            state = state,
            contentPadding = contentPadding,
            reverseLayout = reverseLayout,
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = verticalAlignment,
            modifier = modifier.padding(vertical = scrollbarSize),
            content = content
        )

        HorizontalScrollbar(
            modifier = Modifier.align(Alignment.BottomStart)
                .fillMaxWidth(),
            adapter = rememberScrollbarAdapter(state)
        )
    }
}