package com.arn.scrobble.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.LastfmPeriod
import com.arn.scrobble.charts.ChartsOverviewScreen
import com.arn.scrobble.friends.FriendsScreen
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.navigation.PanoTab
import com.arn.scrobble.recents.ScrobblesScreen
import com.arn.scrobble.ui.PanoPullToRefreshStateForTab
import com.arn.scrobble.utils.PlatformStuff
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePagerScreen(
    user: UserCached,
    tabIdx: Int,
    digestTimePeriod: LastfmPeriod?,
    onSetTabIdx: (Int) -> Unit,
    onSetTitle: (String) -> Unit,
    tabsList: List<PanoTab>,
    onSetOtherUser: (UserCached?) -> Unit,
    onNavigate: (PanoRoute) -> Unit,
    pullToRefreshState: PullToRefreshState,
    onSetRefreshing: (Int, PanoPullToRefreshStateForTab) -> Unit,
    getPullToRefreshTrigger: (Int) -> Flow<Unit>,
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    var scrobblesTitle by rememberSaveable { mutableStateOf("") }
    var followingTitle by rememberSaveable { mutableStateOf("") }
    var chartsTitle by rememberSaveable { mutableStateOf("") }
    val account by Scrobblables.currentAccount.collectAsStateWithLifecycle()
    var lastTabIdx by remember { mutableIntStateOf(tabIdx) }

    LaunchedEffect(tabIdx, scrobblesTitle, followingTitle, chartsTitle) {
        val title = when (tabsList.getOrNull(tabIdx)) {
            is PanoTab.Scrobbles -> scrobblesTitle
            PanoTab.Following -> followingTitle
            PanoTab.Charts -> chartsTitle
            else -> ""
        }
        onSetTitle(title)
        lastTabIdx = tabIdx
    }


    DisposableEffect(user) {
        if (!user.isSelf)
            onSetOtherUser(user)
        else
            onSetOtherUser(null)
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
                onTitleChange = {
                    followingTitle = it
                },
                modifier = Modifier.fillMaxSize()
            )

            PanoTab.Charts -> ChartsOverviewScreen(
                user = user,
                digestTimePeriod = digestTimePeriod,
                onNavigate = onNavigate,
                onTitleChange = {
                    chartsTitle = it
                },
                modifier = Modifier.fillMaxSize()
            )

            else -> {
            }
        }
    }
}