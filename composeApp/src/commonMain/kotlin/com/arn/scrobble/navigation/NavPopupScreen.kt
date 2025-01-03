package com.arn.scrobble.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arn.scrobble.api.DrawerData
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.ui.BottomSheetDialogParent
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource

@Composable
private fun NavPopupContent(
    navMetadataList: List<PanoNavMetadata>,
    otherUser: UserCached?,
    drawerDataFlow: StateFlow<DrawerData>,
    drawSnowfall: Boolean,
    loadOtherUserDrawerData: (UserCached?) -> Unit,
    onNavigate: (PanoRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    val drawerData by drawerDataFlow.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        loadOtherUserDrawerData(otherUser)
    }

    Row(
        modifier = modifier
    ) {
        ProfileHeader(
            otherUser = otherUser,
            drawerData = drawerData,
            compact = false,
            onNavigate = onNavigate,
            drawSnowfall = drawSnowfall,
            modifier = Modifier
                .weight(1f)
                .wrapContentHeight()
        )

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .weight(1f)
        ) {
            navMetadataList.forEach { navMetadata ->
                NavigationDrawerItem(
                    label = { Text(stringResource(navMetadata.titleRes), maxLines = 2) },
                    icon = { Icon(navMetadata.icon, contentDescription = null) },
                    selected = false,
                    onClick = {
                        onNavigate(navMetadata.route)
                    }
                )
            }
        }
    }
}

@Composable
fun NavPopupScreen(
    navMetadataList: List<PanoNavMetadata>,
    onDismiss: () -> Unit,
    otherUser: UserCached?,
    drawSnowfall: Boolean,
    drawerDataFlow: StateFlow<DrawerData>,
    loadOtherUserDrawerData: (UserCached?) -> Unit,
    onNavigate: (PanoRoute) -> Unit,
) {

    BottomSheetDialogParent(
        onDismiss = onDismiss
    ) {
        NavPopupContent(
            navMetadataList = navMetadataList,
            otherUser = otherUser,
            drawerDataFlow = drawerDataFlow,
            loadOtherUserDrawerData = loadOtherUserDrawerData,
            drawSnowfall = drawSnowfall,
            onNavigate = onNavigate,
            modifier = it
        )
    }
}