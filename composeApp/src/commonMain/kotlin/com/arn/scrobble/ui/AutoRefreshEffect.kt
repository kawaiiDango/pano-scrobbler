package com.arn.scrobble.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


@Composable
fun AutoRefreshEffect(
    lastRefreshTime: Long,
    interval: Long,
    shouldRefresh: () -> Boolean,
    lazyPagingItems: LazyPagingItems<*>,
) {
    val autoRefreshScope = rememberCoroutineScope()

    // auto refresh every n seconds
    LifecycleResumeEffect(lastRefreshTime) {
        val job = autoRefreshScope.launch {
            var tryAgain = true
            while (isActive && tryAgain) {
                val delayMs = if (System.currentTimeMillis() - lastRefreshTime > interval)
                    1000L
                else
                    interval

                delay(delayMs)

                if (lazyPagingItems.loadState.refresh is LoadState.NotLoading &&
                    !lazyPagingItems.loadState.hasError &&
                    lazyPagingItems.itemCount <= (Stuff.DEFAULT_PAGE_SIZE + 4) && // some of them are placeholders
                    shouldRefresh()
                ) {
                    lazyPagingItems.refresh()
                    tryAgain = false
                }
            }
        }

        onPauseOrDispose {
            job.cancel()
        }
    }
}