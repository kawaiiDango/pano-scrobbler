package com.arn.scrobble.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.discordrpc.DiscordRpcScreen
import com.arn.scrobble.help.HelpScreen
import com.arn.scrobble.main.MainViewModel
import com.arn.scrobble.ui.addColumnPadding
import com.arn.scrobble.ui.panoContentPadding
import com.arn.scrobble.work.DesktopWorkManager
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.discord_rich_presence
import pano_scrobbler.composeapp.generated.resources.help
import kotlin.system.exitProcess

actual fun NavGraphBuilder.panoPlatformSpecificNavGraph(
    onSetTitle: (String, String?) -> Unit,
    navigate: (PanoRoute) -> Unit,
    goUp: () -> Unit,
    mainViewModel: MainViewModel,
) {
    @Composable
    fun onSetTitleString(destId: String, title: String) {
        DisposableEffect(title) {
            onSetTitle(destId, title)

            onDispose {
                onSetTitle(destId, null)
            }
        }
    }

    @Composable
    fun modifier() = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surface)

    dialog<PanoRoute.Exit> {
        LaunchedEffect(Unit) {
            DesktopWorkManager.clearAll()
            PanoDb.db.close()
            exitProcess(0)
        }
    }


    composable<PanoRoute.DiscordRpcSettings> {
        onSetTitleString(it.id, stringResource(Res.string.discord_rich_presence))
        DiscordRpcScreen(
            modifier = modifier().addColumnPadding()
        )
    }
}