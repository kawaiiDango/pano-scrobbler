package com.arn.scrobble.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
    var validSelectedPage by rememberSaveable {
        mutableIntStateOf(selectedPage.coerceIn(0, totalPages - 1))
    }
    // rememberSaveable does not work with items scrolled away in HorizontalPager, so
    val pageStateHolder = rememberSaveableStateHolder()

    LaunchedEffect(selectedPage) {
        validSelectedPage = selectedPage.coerceIn(0, totalPages - 1)
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

        // todo remove the hack when https://issuetracker.google.com/issues/549552303 is fixed
        val activatedPages = rememberSaveable { mutableStateSetOf(selectedPage) }

        LaunchedEffect(pagerState.targetPage) {
            activatedPages.add(pagerState.targetPage)
        }

        HorizontalPager(
            state = pagerState,
            key = { it },
            beyondViewportPageCount = totalPages - 1,
            modifier = modifier,
            userScrollEnabled = !PlatformStuff.isDesktop,
        ) { page ->
            if (page !in activatedPages) {
                Box(modifier = Modifier.fillMaxSize())
                return@HorizontalPager
            }

            pageStateHolder.SaveableStateProvider(page) {
                content(page)
            }
        }
    } else {
        LaunchedEffect(validSelectedPage) {
            onSelectPage(validSelectedPage)
        }

        Box(modifier = modifier) {
            pageStateHolder.SaveableStateProvider(validSelectedPage) {
                content(validSelectedPage)
            }
        }
    }
}