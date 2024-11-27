package com.arn.scrobble

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arn.scrobble.charts.ChartsOverviewScreen
import com.arn.scrobble.friends.FriendsScreen
import com.arn.scrobble.friends.UserCached
import com.arn.scrobble.main.PanoPager
import com.arn.scrobble.navigation.PanoNavMetadata
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.navigation.PanoTabType
import com.arn.scrobble.navigation.PanoTabs
import com.arn.scrobble.recents.ScrobblesScreen
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@Composable
fun HomePagerScreen(
    user: UserCached,
    tabsList: List<PanoTabs>,
    tabIdx: Int,
    onSetTabIdx: (Int) -> Unit,
    onTitleChange: (String?) -> Unit,
    onSetOtherUser: (UserCached?) -> Unit,
    onSetNavMetadataList: (List<PanoNavMetadata>) -> Unit,
    onNavigate: (PanoRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialPage by PlatformStuff.mainPrefs.data.map { it.lastHomePagerTab }
        .collectAsStateWithLifecycle(null)
    val titlesMap = remember { mutableStateMapOf<Int, String>() }

    LaunchedEffect(titlesMap.toMap()) {
        val page = tabIdx
//        val page = pagerState.currentPage
//        onSetTabIdx(page)
        onTitleChange(titlesMap[page])
    }

    DisposableEffect(Unit) {
        if (!user.isSelf)
            onSetOtherUser(user)
        onSetNavMetadataList(getHomePagerNavMetadata())
        onDispose {
            if (user.isSelf) {
                GlobalScope.launch {
                    PlatformStuff.mainPrefs.updateData {
                        it.copy(lastHomePagerTab = tabIdx)
                    }
                }
            } else {
                onSetOtherUser(null)
            }
        }
    }

    PanoPager(
        initialPage = if (user.isSelf) initialPage ?: 0 else 0,
        selectedPage = tabIdx,
        onSelectPage = onSetTabIdx,
        totalPages = remember(tabsList) { tabsList.count { it.type == PanoTabType.TAB } },
        modifier = modifier,
    ) { page ->
        when (page) {
            0 -> ScrobblesScreen(
                user = user,
                onNavigate = onNavigate,
                showChips = true,
                modifier = Modifier.fillMaxWidth()
            )

            1 -> FriendsScreen(
                user = user,
                onNavigate = onNavigate,
                onTitleChange = {
                    if (it == null)
                        titlesMap.remove(page)
                    else
                        titlesMap[page] = it
                },
                modifier = Modifier.fillMaxWidth()
            )

            2 -> ChartsOverviewScreen(
                user = user,
                onNavigate = onNavigate,
                onTitleChange = {
                    if (it == null)
                        titlesMap.remove(page)
                    else
                        titlesMap[page] = it
                },
                modifier = Modifier.fillMaxWidth()
            )

        }
    }
}

private fun getHomePagerNavMetadata() = listOfNotNull(
    if (!Stuff.billingRepository.isLicenseValid)
        PanoNavMetadata(
            titleRes = R.string.get_pro,
            icon = Icons.Outlined.WorkspacePremium,
            route = PanoRoute.Billing,
        ) else
        null,
    PanoNavMetadata(
        titleRes = R.string.from_mic,
        icon = Icons.Outlined.Mic,
        route = PanoRoute.MicScrobble,
    ),
    PanoNavMetadata(
        titleRes = R.string.search,
        icon = Icons.Outlined.Search,
        route = PanoRoute.Search,
    ),
    PanoNavMetadata(
        titleRes = R.string.settings,
        icon = Icons.Outlined.Settings,
        route = PanoRoute.Prefs,
    ),

    if (!Stuff.isTv)
        PanoNavMetadata(
            titleRes = R.string.help,
            icon = Icons.AutoMirrored.Outlined.HelpOutline,
            route = PanoRoute.Help,
        ) else
        null,
)