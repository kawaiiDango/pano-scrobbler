package com.arn.scrobble.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arn.scrobble.utils.PlatformStuff

@Composable
fun PanoPager(
    initialPage: Int,
    selectedPage: Int,
    onSelectPage: (Int) -> Unit,
    totalPages: Int,
    modifier: Modifier = Modifier,
    content: @Composable (page: Int) -> Unit,
) {
    var firstPageChange by rememberSaveable { mutableStateOf(false) }
    if (!PlatformStuff.isTv) {
        val pagerState = rememberPagerState(
            initialPage = initialPage,
            pageCount = { totalPages }
        )

        LaunchedEffect(selectedPage) {
            if (!firstPageChange) {
                firstPageChange = true
            } else
                pagerState.animateScrollToPage(selectedPage)
        }

        LaunchedEffect(pagerState.settledPage) {
            val page = pagerState.settledPage
            onSelectPage(page)
        }

        HorizontalPager(
            state = pagerState,
            key = { it },
            modifier = modifier,
            userScrollEnabled = !PlatformStuff.isDesktop,
        ) { page ->
            content(page)
        }
    } else {
        LaunchedEffect(Unit) {
            onSelectPage(initialPage)
        }

        Box(modifier = modifier) {
            content(selectedPage)
        }
    }
}