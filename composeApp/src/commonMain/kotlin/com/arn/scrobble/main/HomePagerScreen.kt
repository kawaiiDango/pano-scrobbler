package com.arn.scrobble.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.charts.ChartsOverviewScreen
import com.arn.scrobble.friends.FriendsScreen
import com.arn.scrobble.navigation.PanoDialog
import com.arn.scrobble.navigation.PanoNavMetadata
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.navigation.PanoTab
import com.arn.scrobble.recents.ScrobblesScreen
import com.arn.scrobble.ui.PanoPullToRefreshStateForTab
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.get_pro
import pano_scrobbler.composeapp.generated.resources.help
import pano_scrobbler.composeapp.generated.resources.quit
import pano_scrobbler.composeapp.generated.resources.search
import pano_scrobbler.composeapp.generated.resources.settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePagerScreen(
    user: UserCached,
    tabIdx: Int,
    onSetTabIdx: (Int) -> Unit,
    onSetTitle: (String?) -> Unit,
    onSetTabData: (String, List<PanoTab>?) -> Unit,
    onSetOtherUser: (UserCached?) -> Unit,
    onSetNavMetadataList: (List<PanoNavMetadata>) -> Unit,
    onNavigate: (PanoRoute) -> Unit,
    onOpenDialog: (PanoDialog) -> Unit,
    pullToRefreshState: PullToRefreshState,
    onSetRefreshing: (Int, PanoPullToRefreshStateForTab) -> Unit,
    getPullToRefreshTrigger: (Int) -> Flow<Unit>,
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    var scrobblesTitle by rememberSaveable { mutableStateOf<String?>(null) }
    var followingTitle by rememberSaveable { mutableStateOf<String?>(null) }
    var chartsTitle by rememberSaveable { mutableStateOf<String?>(null) }
    val currentAccountType by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.currentAccountType }
    val tabsList = remember(user, currentAccountType) {
        getTabData(user, currentAccountType)
    }
    var lastTabIdx by remember { mutableIntStateOf(tabIdx) }

    DisposableEffect(tabIdx, scrobblesTitle, followingTitle, chartsTitle) {
        val title = when (tabsList.getOrNull(tabIdx)) {
            is PanoTab.Scrobbles -> scrobblesTitle
            PanoTab.Following -> followingTitle
            PanoTab.Charts -> chartsTitle
            else -> null
        }
        onSetTitle(title)
        lastTabIdx = tabIdx

        onDispose {
            onSetTitle(null)
        }
    }

    DisposableEffect(user, currentAccountType) {
        val id =
            PanoRoute.SelfHomePager::class.simpleName + " " + user.name + " " + currentAccountType

        onSetTabData(id, tabsList)

        onDispose {
            onSetTabData(id, null)
        }
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
        selectedPage = tabIdx,
        onSelectPage = onSetTabIdx,
        totalPages = remember(tabsList) { tabsList.count { it !is PanoTab.Profile } },
        modifier = modifier,
    ) { page ->
        when (val currentTab = tabsList.getOrNull(page)) {
            is PanoTab.Scrobbles -> ScrobblesScreen(
                user = user,
                pullToRefreshState = pullToRefreshState,
                onSetRefreshing = { onSetRefreshing(page, it) },
                pullToRefreshTriggered = getPullToRefreshTrigger(page),
                showChips = currentTab.showChips,
                onNavigate = onNavigate,
                onOpenDialog = onOpenDialog,
                editDataFlow = mainViewModel.editDataFlow,
                onTitleChange = {
                    scrobblesTitle = it
                },
                modifier = Modifier.fillMaxSize()
            )

            PanoTab.Following -> FriendsScreen(
                user = user,
                pullToRefreshState = pullToRefreshState,
                onSetRefreshing = { onSetRefreshing(page, it) },
                pullToRefreshTriggered = getPullToRefreshTrigger(page),
                onNavigate = onNavigate,
                onOpenDialog = onOpenDialog,
                onTitleChange = {
                    followingTitle = it
                },
                modifier = Modifier.fillMaxSize()
            )

            PanoTab.Charts -> ChartsOverviewScreen(
                user = user,
                onNavigate = onNavigate,
                onOpenDialog = onOpenDialog,
                onTitleChange = {
                    chartsTitle = it
                },
                modifier = Modifier
                    .fillMaxSize()
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
    PanoNavMetadata(
        titleRes = Res.string.settings,
        icon = Icons.Outlined.Settings,
        route = PanoRoute.Prefs,
    ),
    PanoNavMetadata(
        titleRes = Res.string.search,
        icon = Icons.Outlined.Search,
        route = PanoRoute.Search,
    ),

    if (!PlatformStuff.isTv)
        PanoNavMetadata(
            titleRes = Res.string.help,
            icon = Icons.AutoMirrored.Outlined.HelpOutline,
            route = PanoRoute.Help,
        ) else
        null,

    if (PlatformStuff.isDesktop)
        PanoNavMetadata(
            titleRes = Res.string.quit,
            icon = Icons.Outlined.PowerSettingsNew,
            route = PanoRoute.Exit,
        ) else
        null,
)

private fun getTabData(user: UserCached, accountType: AccountType): List<PanoTab> {
    return when (accountType) {
        AccountType.LASTFM,
        AccountType.LISTENBRAINZ,
        AccountType.CUSTOM_LISTENBRAINZ,
            -> listOf(
            PanoTab.Scrobbles(),
            PanoTab.Following,
            PanoTab.Charts,
            PanoTab.Profile(user),
        )

        AccountType.LIBREFM,
        AccountType.GNUFM,
            -> listOf(
            PanoTab.Scrobbles(),
            PanoTab.Charts,
            PanoTab.Profile(user),
        )

        AccountType.MALOJA,
        AccountType.PLEROMA,
        AccountType.FILE,
            -> listOf(
            PanoTab.Scrobbles(showChips = false),
            PanoTab.Profile(user),
        )
    }
}