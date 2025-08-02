package com.arn.scrobble.main

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import co.touchlab.kermit.Logger
import coil3.compose.setSingletonImageLoaderFactory
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.api.DrawerData
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.imageloader.newImageLoader
import com.arn.scrobble.navigation.LocalNavigationType
import com.arn.scrobble.navigation.NavFromTrayEffect
import com.arn.scrobble.navigation.PanoDialog
import com.arn.scrobble.navigation.PanoFabData
import com.arn.scrobble.navigation.PanoNavigationType
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.navigation.PanoTab
import com.arn.scrobble.navigation.getFabData
import com.arn.scrobble.navigation.hasTabMetadata
import com.arn.scrobble.navigation.jsonSerializableSaver
import com.arn.scrobble.navigation.panoNavGraph
import com.arn.scrobble.ui.AvatarOrInitials
import com.arn.scrobble.ui.LocalInnerPadding
import com.arn.scrobble.ui.PanoPullToRefreshStateForTab
import com.arn.scrobble.ui.PanoSnackbarVisuals
import com.arn.scrobble.ui.horizontalOverscanPadding
import com.arn.scrobble.updates.runUpdateAction
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.back
import pano_scrobbler.composeapp.generated.resources.download
import pano_scrobbler.composeapp.generated.resources.reload
import pano_scrobbler.composeapp.generated.resources.update_available
import pano_scrobbler.composeapp.generated.resources.update_downloaded

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanoAppContent(
    navController: NavHostController,
    viewModel: MainViewModel = viewModel { MainViewModel() },
) {

    val sizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val navigationType = when {
        sizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND)
            -> PanoNavigationType.PERMANENT_NAVIGATION_DRAWER

        sizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)
            -> PanoNavigationType.NAVIGATION_RAIL

        else -> PanoNavigationType.BOTTOM_NAVIGATION
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val currentBackStackEntry by navController.currentBackStackEntryAsState()

    val titlesMap = remember { mutableStateMapOf<String, String>() }
    val tabData = remember { mutableStateMapOf<String, List<PanoTab>>() }
    var currentDialogArgs by rememberSaveable(saver = jsonSerializableSaver<PanoDialog?>()) {
        mutableStateOf(null)
    }
    val drawerData by viewModel.drawerDataFlow.collectAsStateWithLifecycle()
    val currentUserSelf by PlatformStuff.mainPrefs.data
        .collectAsStateWithInitialValue { pref ->
            pref.scrobbleAccounts.firstOrNull { it.type == pref.currentAccountType }?.user
        }
    var onboardingFinished by rememberSaveable { mutableStateOf(currentUserSelf != null) }
    var currentUserOther by rememberSaveable(saver = jsonSerializableSaver()) {
        mutableStateOf<UserCached?>(null)
    }
    val currentUser = currentUserOther ?: currentUserSelf

    val topBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val pullToRefreshState = rememberPullToRefreshState()
    val pullToRefreshStateForTabs =
        remember { mutableStateMapOf<Int, PanoPullToRefreshStateForTab>() }

    val canPop by remember(currentBackStackEntry) { mutableStateOf(navController.previousBackStackEntry != null) }
    val fabData by remember(currentBackStackEntry) {
        mutableStateOf(getFabData(currentBackStackEntry?.destination))
    }

    var selectedTabIdx by rememberSaveable { mutableIntStateOf(0) }

    val showTabs by remember(currentBackStackEntry) {
        mutableStateOf(
            hasTabMetadata(
                currentBackStackEntry?.destination
            )
        )
    }
    val contentFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        combine(
            viewModel.tabIdx,
            navController.currentBackStackEntryFlow
        )
        { (backStackEntryId, tab), backStackEntry ->
            if (backStackEntryId == backStackEntry.id)
                selectedTabIdx = tab

            if (PlatformStuff.isTv) {
                delay(1000)
                val requestFocusRes = contentFocusRequester.requestFocus()
                Logger.d { "Request focus result: $requestFocusRes" }
            }
        }
            .collect()
    }

    LaunchedEffect(Unit) {
        Stuff.globalSnackbarFlow.collectLatest {
            snackbarHostState.showSnackbar(it)
        }
    }

    LaunchedEffect(Unit) {
        Stuff.globalUpdateAction
            .filterNotNull()
            .collectLatest {
                val message = if (PlatformStuff.isDesktop) {
                    getString(Res.string.update_downloaded) +
                            ": ${it.version}"
                } else {
                    getString(Res.string.update_available, it.version)
                }

                val actionLabel = if (PlatformStuff.isDesktop) {
                    getString(Res.string.reload)
                } else {
                    getString(Res.string.download)
                }

                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = actionLabel,
                )

                if (result == SnackbarResult.ActionPerformed) {
                    runUpdateAction(it)
                }
            }
    }

    // show changelog if needed
    LaunchedEffect(Unit) {
        delay(2000)

        val changelogHashcode = BuildKonfig.CHANGELOG.hashCode()
        val storedHashcode = PlatformStuff.mainPrefs.data.map { it.changelogSeenHashcode }.first()

        if (storedHashcode != changelogHashcode) {
            if (storedHashcode != null) {
                currentDialogArgs = PanoDialog.Changelog
            }
            // else is fresh install
            PlatformStuff.mainPrefs.updateData { it.copy(changelogSeenHashcode = changelogHashcode) }
        }
    }

    NavFromTrayEffect(
        onOpenDialog = { currentDialogArgs = it },
    )

    setSingletonImageLoaderFactory { context ->
        newImageLoader(context)
    }

    CompositionLocalProvider(LocalNavigationType provides navigationType) {
        val currentNavType = LocalNavigationType.current
        Surface {
            Row(Modifier.fillMaxSize()) {
                if (currentNavType != PanoNavigationType.BOTTOM_NAVIGATION) {
                    PanoNavigationRail(
                        tabs = if (showTabs)
                            (tabData.values.firstOrNull() ?: emptyList())
                        else
                            emptyList(),
                        selectedTabIdx = selectedTabIdx,
                        fabData = fabData,
                        onNavigate = navController::navigate,
                        onOpenDialog = { currentDialogArgs = it },
                        onBack = { navController.popBackStack() },
                        onTabClicked = { pos, tab ->
                            currentBackStackEntry?.id?.let {
                                viewModel.setTabIdx(it, pos)
                            }
                        },
                        onProfileClicked = {
                            currentDialogArgs = PanoDialog.NavPopup(otherUser = currentUserOther)
                        },
                        userp = currentUser,
                        drawerData = drawerData,
                    )
                }

                Box(
                    contentAlignment = Alignment.TopCenter,
                    modifier = Modifier
                        .fillMaxSize()
                        .focusGroup()
                        .focusRequester(contentFocusRequester)
                ) {
                    Scaffold(
                        modifier = Modifier
                            .widthIn(max = 1020.dp)
                            .pullToRefresh(
                                state = pullToRefreshState,
                                isRefreshing = pullToRefreshStateForTabs.values.any { it == PanoPullToRefreshStateForTab.Refreshing },
                                enabled = (!PlatformStuff.isDesktop && !PlatformStuff.isTv) && !pullToRefreshStateForTabs.values.all { it == PanoPullToRefreshStateForTab.Disabled },
                                onRefresh = {
                                    // find the right tab
                                    pullToRefreshStateForTabs.entries
                                        .find { it.value == PanoPullToRefreshStateForTab.NotRefreshing }
                                        ?.key
                                        ?.let { id ->
                                            viewModel.notifyPullToRefresh(id)
                                        }
                                }
                            )
                            .nestedScroll(topBarScrollBehavior.nestedScrollConnection),
                        topBar = {
                            PanoTopAppBar(
                                titlesMap.values.firstOrNull() ?: "",
                                scrollBehavior = { topBarScrollBehavior },
                                showBack = !PlatformStuff.isTv && canPop,
                                onBack = { navController.navigateUp() },
                            )
                        },
                        bottomBar = {
                            if (currentNavType == PanoNavigationType.BOTTOM_NAVIGATION && showTabs) {
                                PanoBottomNavigationBar(
                                    tabs = tabData.values.firstOrNull() ?: emptyList(),
                                    selectedTabIdx = selectedTabIdx,
                                    onTabClicked = { pos, tab ->
                                        currentBackStackEntry?.id?.let {
                                            viewModel.setTabIdx(it, pos)
                                        }
                                    },
                                    onProfileClicked = {
                                        currentDialogArgs = PanoDialog.NavPopup(
                                            otherUser = if (!it.isSelf) it else null,
                                        )
                                    },
                                    drawerData = drawerData,
                                )
                            }
                        },
                        floatingActionButton = {
                            if (currentNavType == PanoNavigationType.BOTTOM_NAVIGATION) {
                                fabData?.let { fabData ->
                                    PanoFab(
                                        fabData,
                                        onBack = { navController.popBackStack() },
                                        onNavigate = { navController.navigate(it) },
                                        onOpenDialog = { currentDialogArgs = it },
                                    )
                                }
                            }
                        },

                        snackbarHost = {
                            SnackbarHost(
                                hostState = snackbarHostState,
                            ) { snackbarData ->
                                val visuals = snackbarData.visuals as? PanoSnackbarVisuals
                                Snackbar(
                                    snackbarData = snackbarData,
                                    containerColor = if (visuals?.isError == true) MaterialTheme.colorScheme.errorContainer else SnackbarDefaults.color,
                                    contentColor = if (visuals?.isError == true) MaterialTheme.colorScheme.onErrorContainer else SnackbarDefaults.contentColor,
                                )
                            }
                        },
                    ) { innerPadding ->
                        CompositionLocalProvider(LocalInnerPadding provides innerPadding) {
                            val topPadding = PaddingValues(top = innerPadding.calculateTopPadding())
                            val offsetDenominator = 4

                            NavHost(
                                navController = navController,
                                startDestination = when {
                                    !onboardingFinished || currentUserSelf == null -> PanoRoute.Onboarding
                                    else -> PanoRoute.SelfHomePager
                                },
                                enterTransition = {
                                    fadeIn(animationSpec = tween()) + slideIntoContainer(
                                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                                        animationSpec = tween(),
                                        initialOffset = { -it / offsetDenominator }
                                    )
                                },
                                exitTransition = {
                                    slideOutOfContainer(
                                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                        animationSpec = tween(),
                                        targetOffset = { it / offsetDenominator }
                                    ) + fadeOut(animationSpec = tween())
                                },
                                popEnterTransition = {
                                    fadeIn(animationSpec = tween()) + slideIntoContainer(
                                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                        animationSpec = tween(),
                                        initialOffset = { -it / offsetDenominator }
                                    )
                                },
                                popExitTransition = {
                                    slideOutOfContainer(
                                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                                        animationSpec = tween(),
                                        targetOffset = { it / offsetDenominator }
                                    ) + fadeOut(animationSpec = tween())
                                },
                                modifier = Modifier
                                    .padding(topPadding)
                                    .consumeWindowInsets(topPadding)
                            ) {
                                panoNavGraph(
                                    onSetTitle = { id, title ->
                                        if (title != null)
                                            titlesMap[id] = title
                                        else
                                            titlesMap.remove(id)
                                    },
                                    onSetTabData = { id, it ->
                                        if (it != null)
                                            tabData[id] = it
                                        else
                                            tabData.remove(id)
                                    },
                                    navigate = navController::navigate,
                                    goBack = navController::popBackStack,
                                    goUp = navController::navigateUp,
                                    onSetOnboardingFinished = {
                                        onboardingFinished = it
                                    },
                                    onSetOtherUser = { currentUserOther = it },
                                    onOpenDialog = { currentDialogArgs = it },
                                    pullToRefreshState = { pullToRefreshState },
                                    onSetRefreshing = { id, prState ->
                                        pullToRefreshStateForTabs[id] = prState
                                    },
                                    mainViewModel = viewModel,
                                )
                            }
                        }
                    }

                    PanoDialogStack(
                        initialDialogArgs = currentDialogArgs,
                        onNavigate = navController::navigate,
                        onDismissRequest = { currentDialogArgs = null },
                        mainViewModel = viewModel,
                    )
                }
            }

            if (PlatformStuff.isDebug && PlatformStuff.isTv) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 27.dp, horizontal = 48.dp)
                        .border(1.dp, MaterialTheme.colorScheme.error)
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PanoFab(
    fabData: PanoFabData,
    onBack: () -> Unit,
    onNavigate: (PanoRoute) -> Unit,
    onOpenDialog: (PanoDialog) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (LocalNavigationType.current == PanoNavigationType.PERMANENT_NAVIGATION_DRAWER) {
        SmallExtendedFloatingActionButton(
            onClick = {
                if (fabData.route == PanoRoute.SpecialGoBack)
                    onBack()
                else if (fabData.route != null)
                    onNavigate(fabData.route)
                else if (fabData.dialog != null)
                    onOpenDialog(fabData.dialog)
            },
            icon = {
                Icon(
                    imageVector = fabData.icon,
                    contentDescription = null
                )
            },
            text = {
                Text(
                    text = stringResource(fabData.stringRes),
                )
            },
            modifier = modifier.padding(16.dp)
        )
    } else {
        FloatingActionButton(
            onClick = {
                if (fabData.route == PanoRoute.SpecialGoBack)
                    onBack()
                else if (fabData.route != null)
                    onNavigate(fabData.route)
                else if (fabData.dialog != null)
                    onOpenDialog(fabData.dialog)
            },
            modifier = modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = fabData.icon,
                contentDescription = stringResource(fabData.stringRes)
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PanoTopAppBar(
    title: String,
    showBack: Boolean,
    onBack: () -> Unit,
    scrollBehavior: () -> TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
) {
    MediumFlexibleTopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.primary
            )
        },
        navigationIcon = {
            AnimatedVisibility(
                visible = showBack,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(Res.string.back)
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior()
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PanoNavigationRail(
    tabs: List<PanoTab>,
    selectedTabIdx: Int,
    fabData: PanoFabData?,
    onBack: () -> Unit,
    onNavigate: (PanoRoute) -> Unit,
    onOpenDialog: (PanoDialog) -> Unit,
    onTabClicked: (pos: Int, PanoTab) -> Unit,
    onProfileClicked: (UserCached) -> Unit,
    userp: UserCached?,
    drawerData: DrawerData?,
    modifier: Modifier = Modifier,
) {
    val expandedByDefault = when (LocalNavigationType.current) {
        PanoNavigationType.PERMANENT_NAVIGATION_DRAWER -> true
        PanoNavigationType.NAVIGATION_RAIL,
        PanoNavigationType.BOTTOM_NAVIGATION -> false
    }

    val state = rememberWideNavigationRailState(
        if (expandedByDefault)
            WideNavigationRailValue.Expanded
        else
            WideNavigationRailValue.Collapsed
    )

    LaunchedEffect(expandedByDefault) {
        if (expandedByDefault) {
            state.expand()
        } else {
            state.collapse()
        }
    }

    WideNavigationRail(
        state = state,
        header = {
            if (fabData != null) {
                PanoFab(
                    fabData,
                    onBack = onBack,
                    onNavigate = onNavigate,
                    onOpenDialog = onOpenDialog,
                    modifier = Modifier
                        .padding(start = horizontalOverscanPadding() / 2, top = 16.dp)
                )
            } else {
                val user = tabs.filterIsInstance<PanoTab.Profile>().firstOrNull()?.user ?: userp

                if (user != null) {
                    WideNavigationRailItem(
                        selected = false,
                        onClick = { onProfileClicked(user) },
                        railExpanded = state.targetValue == WideNavigationRailValue.Expanded,
                        icon = {
                            AvatarOrInitials(
                                avatarUrl = when {
                                    !user.isSelf -> user.largeImage
                                    user.isSelf && drawerData != null -> drawerData.profilePicUrl
                                    else -> null
                                },
                                avatarName = user.name,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                            )
                        },
                        label = {
                            Text(
                                text = if (Stuff.isInDemoMode)
                                    "me"
                                else
                                    user.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .widthIn(max = 100.dp)
                            )
                        },
                        modifier = Modifier
                            .padding(start = horizontalOverscanPadding() / 2, top = 16.dp)
                    )
                }
            }
        },
        arrangement = Arrangement.aligned(Alignment.CenterVertically),
        modifier = modifier
            .widthIn(max = 200.dp)
            .fillMaxHeight()
    ) {
        tabs.filter { it !is PanoTab.Profile }
            .forEachIndexed { index, tabMetadata ->
                WideNavigationRailItem(
                    selected = index == selectedTabIdx,
                    onClick = {
                        if (index != selectedTabIdx) {
                            onTabClicked(index, tabMetadata)
                        }
                    },
                    railExpanded = state.targetValue == WideNavigationRailValue.Expanded,
                    icon = {
                        Icon(
                            imageVector = tabMetadata.icon,
                            contentDescription = stringResource(tabMetadata.titleRes)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(tabMetadata.titleRes),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PanoBottomNavigationBar(
    tabs: List<PanoTab>,
    selectedTabIdx: Int,
    onTabClicked: (pos: Int, PanoTab) -> Unit,
    onProfileClicked: (UserCached) -> Unit,
    drawerData: DrawerData?,
) {
    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
    ) {
        tabs.forEachIndexed { index, tabMetadata ->

            NavigationBarItem(
                selected = index == selectedTabIdx,
                onClick = {
                    if (tabMetadata is PanoTab.Profile) {
                        onProfileClicked(tabMetadata.user)
                        return@NavigationBarItem
                    }

                    if (index != selectedTabIdx) {
                        onTabClicked(index, tabMetadata)
                    }
                },
                icon = {
                    if (tabMetadata is PanoTab.Profile) {

                        AvatarOrInitials(
                            avatarUrl =
                                when {
                                    !tabMetadata.user.isSelf -> tabMetadata.user.largeImage
                                    tabMetadata.user.isSelf && drawerData != null -> drawerData.profilePicUrl
                                    else -> null
                                },
                            avatarName = tabMetadata.user.name,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                        )
                    } else
                        Icon(
                            imageVector = tabMetadata.icon,
                            contentDescription = stringResource(tabMetadata.titleRes)
                        )
                },
                label = {
                    Text(
                        if (tabMetadata is PanoTab.Profile)
                            if (Stuff.isInDemoMode)
                                "me"
                            else
                                tabMetadata.user.name
                        else
                            stringResource(tabMetadata.titleRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .widthIn(max = 100.dp)
                    )
                }
            )
        }
    }
}