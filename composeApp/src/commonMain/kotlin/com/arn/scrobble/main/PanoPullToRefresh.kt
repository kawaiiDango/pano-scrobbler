package com.arn.scrobble.main

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PanoPullToRefresh(
    isRefreshing: Boolean,
    state: PullToRefreshState,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val scaleFraction = {
        if (isRefreshing) 1f
        else LinearOutSlowInEasing.transform(state.distanceFraction).coerceIn(0f, 1f)
    }

    Box(
        modifier = modifier,
    ) {
        content()

        Box(
            Modifier.align(Alignment.TopCenter).graphicsLayer {
                scaleX = scaleFraction()
                scaleY = scaleFraction()
            }
        ) {
            PullToRefreshDefaults.LoadingIndicator(state = state, isRefreshing = isRefreshing)
        }
    }
}