package com.arn.scrobble.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


@Composable
fun AutoRefreshEffect(
    firstPageLoadedTime: Long?,
    interval: Duration,
    doRefresh: () -> Boolean,
    lazyPagingItems: LazyPagingItems<*>,
) {
    val autoRefreshScope = rememberCoroutineScope()

    // auto refresh every n seconds
    LifecycleResumeEffect(firstPageLoadedTime) {
        val job = autoRefreshScope.launch {
            var tryAgain = true
            while (isActive && tryAgain && firstPageLoadedTime != null) {
                val delay =
                    if ((System.currentTimeMillis() - firstPageLoadedTime).milliseconds > interval)
                        1.seconds
                    else
                        interval

                delay(delay)

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