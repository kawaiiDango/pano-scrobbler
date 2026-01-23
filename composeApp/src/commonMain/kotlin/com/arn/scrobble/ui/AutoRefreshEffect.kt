package com.arn.scrobble.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


@Composable
fun AutoRefreshEffect(
    firstPageLoadedTime: Long?,
    interval: Long,
    doRefresh: () -> Boolean,
    lazyPagingItems: LazyPagingItems<*>,
) {
    val autoRefreshScope = rememberCoroutineScope()

    // auto refresh every n seconds
    LifecycleResumeEffect(firstPageLoadedTime) {
        val job = autoRefreshScope.launch {
            var tryAgain = true
            while (isActive && tryAgain && firstPageLoadedTime != null) {
                val delayMs = if (System.currentTimeMillis() - firstPageLoadedTime > interval)
                    1000L
                else
                    interval

                delay(delayMs)

                if (lazyPagingItems.loadState.refresh is LoadState.NotLoading &&
                    !lazyPagingItems.loadState.hasError
                ) {
                    if (doRefresh())
                        tryAgain = false
                }
            }
        }

        onPauseOrDispose {
            job.cancel()
        }
    }
}