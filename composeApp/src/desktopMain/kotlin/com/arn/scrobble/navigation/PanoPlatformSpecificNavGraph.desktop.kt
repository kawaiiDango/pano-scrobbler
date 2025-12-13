package com.arn.scrobble.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import com.arn.scrobble.discordrpc.DiscordRpcScreen
import com.arn.scrobble.main.MainViewModel
import com.arn.scrobble.ui.addColumnPadding
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.discord_rich_presence

actual fun EntryProviderScope<PanoRoute>.panoPlatformSpecificNavGraph(
    onSetTitle: (PanoRoute, String) -> Unit,
    navigate: (PanoRoute) -> Unit,
    goBack: () -> Unit,
    mainViewModel: MainViewModel,
) {
    @Composable
    fun onSetTitleRes(route: PanoRoute, resId: StringResource) {
        val title = stringResource(resId)

        LaunchedEffect(resId) {
            onSetTitle(route, title)
        }
    }

    @Composable
    fun modifier() = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surface)

    entry<PanoRoute.DiscordRpcSettings>(
    ) { route ->
        onSetTitleRes(route, Res.string.discord_rich_presence)
        DiscordRpcScreen(
            modifier = modifier().addColumnPadding()
        )
    }
}