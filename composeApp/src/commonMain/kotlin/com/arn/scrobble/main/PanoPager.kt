package com.arn.scrobble.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arn.scrobble.utils.PlatformStuff

@Composable
fun PanoPager(
    selectedPage: Int,
    onSelectPage: (Int) -> Unit,
    totalPages: Int,
    modifier: Modifier = Modifier,
    content: @Composable (page: Int) -> Unit,
) {
    val initialPage by rememberSaveable { mutableIntStateOf(selectedPage) }
    val validSelectedPage by rememberSaveable(selectedPage, totalPages) {
        mutableIntStateOf(selectedPage.coerceIn(0, totalPages - 1))
    }

    if (!PlatformStuff.isTv) {
        var firstPageChange by rememberSaveable { mutableStateOf(false) }

        val pagerState = rememberPagerState(
            initialPage = initialPage,
            pageCount = { totalPages }
        )

        LaunchedEffect(validSelectedPage) {
            if (!firstPageChange) {
                firstPageChange = true
            } else
                pagerState.animateScrollToPage(validSelectedPage)
        }

        LaunchedEffect(pagerState.settledPage) {
            onSelectPage(pagerState.settledPage)
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
        LaunchedEffect(validSelectedPage) {
            onSelectPage(validSelectedPage)
        }

        Box(modifier = modifier) {
            content(validSelectedPage)
        }
    }
}