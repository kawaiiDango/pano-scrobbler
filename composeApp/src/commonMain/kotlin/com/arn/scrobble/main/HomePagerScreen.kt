package com.arn.scrobble.main

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.charts.ChartsOverviewScreen
import com.arn.scrobble.friends.FriendsScreen
import com.arn.scrobble.navigation.PanoNavMetadata
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.navigation.PanoTabType
import com.arn.scrobble.navigation.PanoTabs
import com.arn.scrobble.recents.ScrobblesScreen
import com.arn.scrobble.ui.PanoPullToRefreshStateForTab
import com.arn.scrobble.utils.PlatformStuff
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.from_mic
import pano_scrobbler.composeapp.generated.resources.get_pro
import pano_scrobbler.composeapp.generated.resources.help
import pano_scrobbler.composeapp.generated.resources.search
import pano_scrobbler.composeapp.generated.resources.settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePagerScreen(
    user: UserCached,
    tabsList: List<PanoTabs>,
    initialTabIdx: Int,
    tabIdx: Int,
    onSetTabIdx: (Int) -> Unit,
    onTitleChange: (String?) -> Unit,
    onSetOtherUser: (UserCached?) -> Unit,
    onSetNavMetadataList: (List<PanoNavMetadata>) -> Unit,
    onNavigate: (PanoRoute) -> Unit,
    pullToRefreshState: PullToRefreshState,
    onSetRefreshing: (Int, PanoPullToRefreshStateForTab) -> Unit,
    getPullToRefreshTrigger: (Int) -> Flow<Unit>,
    modifier: Modifier = Modifier,
) {
    val scrobblesTitle by rememberSaveable { mutableStateOf<String?>(null) }
    var followingTitle by rememberSaveable { mutableStateOf<String?>(null) }
    var chartsTitle by rememberSaveable { mutableStateOf<String?>(null) }
    var lastTabIdx by remember { mutableStateOf(tabIdx) }

    LaunchedEffect(tabIdx, scrobblesTitle, followingTitle, chartsTitle) {
        val title = when (tabsList.getOrNull(tabIdx)) {
            is PanoTabs.Scrobbles -> scrobblesTitle
            PanoTabs.Following -> followingTitle
            PanoTabs.Charts -> chartsTitle
            else -> null
        }
        onTitleChange(title)
        lastTabIdx = tabIdx
    }

    DisposableEffect(user) {
        if (!user.isSelf)
            onSetOtherUser(user)
        else
            onSetOtherUser(null)
        onSetNavMetadataList(getHomePagerNavMetadata())
        onDispose {
            if (user.isSelf) {
                // use lastTabIdx because tabIdx somehow becomes 0 in onDispose
                GlobalScope.launch {
                    PlatformStuff.mainPrefs.updateData {
                        it.copy(lastHomePagerTab = lastTabIdx)
                    }
                }
            }
        }
    }

    PanoPager(
        initialPage = initialTabIdx,
        selectedPage = tabIdx,
        onSelectPage = onSetTabIdx,
        totalPages = remember(tabsList) { tabsList.count { it.type == PanoTabType.TAB } },
        modifier = modifier,
    ) { page ->
        when (val currentTab = tabsList.getOrNull(page)) {
            is PanoTabs.Scrobbles -> ScrobblesScreen(
                user = user,
                pullToRefreshState = pullToRefreshState,
                onSetRefreshing = { onSetRefreshing(page, it) },
                pullToRefreshTriggered = getPullToRefreshTrigger(page),
                showChips = currentTab.showChips,
                onNavigate = onNavigate,
                modifier = Modifier.fillMaxWidth()
            )

            PanoTabs.Following -> FriendsScreen(
                user = user,
                pullToRefreshState = pullToRefreshState,
                onSetRefreshing = { onSetRefreshing(page, it) },
                pullToRefreshTriggered = getPullToRefreshTrigger(page),
                onNavigate = onNavigate,
                onTitleChange = {
                    followingTitle = it
                },
                modifier = Modifier.fillMaxWidth()
            )

            PanoTabs.Charts -> ChartsOverviewScreen(
                user = user,
                onNavigate = onNavigate,
                onTitleChange = {
                    chartsTitle = it
                },
                modifier = Modifier
                    .fillMaxWidth()
            )

            else -> {
            }
        }
    }
}

private fun getHomePagerNavMetadata() = listOfNotNull(
    if (!PlatformStuff.billingRepository.isLicenseValid)
        PanoNavMetadata(
            titleRes = Res.string.get_pro,
            icon = Icons.Outlined.WorkspacePremium,
            route = PanoRoute.Billing,
        ) else
        null,
    if (!PlatformStuff.isDesktop)
        PanoNavMetadata(
            titleRes = Res.string.from_mic,
            icon = Icons.Outlined.Mic,
            route = PanoRoute.MicScrobble,
        )
    else
        null,
    PanoNavMetadata(
        titleRes = Res.string.search,
        icon = Icons.Outlined.Search,
        route = PanoRoute.Search,
    ),
    PanoNavMetadata(
        titleRes = Res.string.settings,
        icon = Icons.Outlined.Settings,
        route = PanoRoute.Prefs,
    ),

    if (!PlatformStuff.isTv)
        PanoNavMetadata(
            titleRes = Res.string.help,
            icon = Icons.AutoMirrored.Outlined.HelpOutline,
            route = PanoRoute.Help,
        ) else
        null,
)