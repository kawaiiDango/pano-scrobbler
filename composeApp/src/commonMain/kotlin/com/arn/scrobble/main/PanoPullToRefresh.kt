package com.arn.scrobble.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arn.scrobble.utils.PlatformStuff

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanoPullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    if (!PlatformStuff.isTv && !PlatformStuff.isDesktop) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = modifier,
            content = content
        )
    } else {
        Box(
            modifier = modifier,
            content = content
        )
    }
}