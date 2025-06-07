package com.arn.scrobble.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier


@Composable
expect fun PanoLazyColumn(
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = panoContentPadding(),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
)

@Composable
expect fun PanoLazyVerticalGrid(
    state: LazyGridState = rememberLazyGridState(),
    columns: GridCells,
    contentPadding: PaddingValues = panoContentPadding(),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    modifier: Modifier = Modifier,
    content: LazyGridScope.() -> Unit,
)

@Composable
expect fun PanoLazyRow(
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = panoContentPadding(bottom = false),
    reverseLayout: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal = if (!reverseLayout) Arrangement.Start else Arrangement.End,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
)

@Composable
expect fun OptionalHorizontalScrollbar(state: ScrollState, modifier: Modifier = Modifier)