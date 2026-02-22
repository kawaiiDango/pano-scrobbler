package com.arn.scrobble.main

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.TopAppBar
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import coil3.compose.setSingletonImageLoaderFactory
import com.arn.scrobble.api.DrawerData
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.automirrored.ArrowBack
import com.arn.scrobble.imageloader.newImageLoader
import com.arn.scrobble.navigation.BottomSheetSceneStrategy
import com.arn.scrobble.navigation.LocalNavigationType
import com.arn.scrobble.navigation.NavFromOutsideEffect
import com.arn.scrobble.navigation.PanoFabData
import com.arn.scrobble.navigation.PanoNavGraph
import com.arn.scrobble.navigation.PanoNavigationType
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.navigation.PanoTab
import com.arn.scrobble.navigation.rememberPanoNavBackStack
import com.arn.scrobble.ui.AvatarOrInitials
import com.arn.scrobble.ui.LocalInnerPadding
import com.arn.scrobble.ui.PanoPullToRefreshStateForTab
import com.arn.scrobble.ui.PanoSnackbarVisuals
import com.arn.scrobble.ui.horizontalOverscanPadding
import com.arn.scrobble.updates.runUpdateAction
import com.arn.scrobble.utils.LocaleUtils
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
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

    val locale by if (!PlatformStuff.hasSystemLocaleStore)
        LocaleUtils.locale.collectAsStateWithLifecycle()
    else
        remember { mutableStateOf(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    val titlesMap = remember { mutableStateMapOf<PanoRoute, String>() }
    val tabIdxMap = remember { mutableStateMapOf<PanoRoute.HasTabs, Int>() }
    val drawerDataMap = remember { viewModel.drawerDataMap }

    val topBarScrollBehavior = if (PlatformStuff.isTv)
        TopAppBarDefaults.pinnedScrollBehavior()
    else
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val pullToRefreshState = rememberPullToRefreshState()
    val pullToRefreshStateForSelfHomePager =
        remember { mutableStateMapOf<Int, PanoPullToRefreshStateForTab>() }

    val currentAccountType by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.currentAccountType }
    val userSelf by PlatformStuff.mainPrefs.data
        .collectAsStateWithInitialValue { it.currentAccount?.user }
    val profilePicUrlOverride by PlatformStuff.mainPrefs.data
        .collectAsStateWithInitialValue { it.drawerData[it.currentAccountType]?.profilePicUrl }

    val backStack = rememberPanoNavBackStack(
        SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(PanoRoute::class)
            }
        },
        when {
            userSelf == null -> PanoRoute.Onboarding
            else -> PanoRoute.SelfHomePager()
        }
    )

    val currentUser = remember(backStack.lastOrNull()) {
        val withUser = backStack.lastOrNull { it is PanoRoute.HasUser } as? PanoRoute.HasUser
        if (withUser != null)
            withUser.user ?: userSelf
        else
            null
    }

    val currentPanoRoute = remember(backStack.lastOrNull()) {
        backStack.lastOrNull { it !is PanoRoute.Modal }
    }

    val fabData = remember(currentPanoRoute) {
        (currentPanoRoute as? PanoRoute.HasFab)?.getFabData()
    }

    val tabData = remember(currentPanoRoute, currentAccountType) {
        (currentPanoRoute as? PanoRoute.HasTabs)?.getTabsList(currentAccountType)
    }

    fun goBack(): PanoRoute? {
        if (backStack.size <= 1)
            return null

        val route = backStack.removeLastOrNull()

        if (route is PanoRoute.HasTabs)
            tabIdxMap.remove(route)

        if (route != null)
            titlesMap.remove(route)

        return route
    }

    fun removeAllModals() {
        backStack.removeAll { it is PanoRoute.Modal }
    }

    fun navigate(route: PanoRoute) {
        if (route !is PanoRoute.Modal)
            removeAllModals()

        if (backStack.lastOrNull() != route)
            backStack.add(route)
    }

    fun replaceRoutes(syntheticBackStack: List<PanoRoute>) {
        tabIdxMap.clear()
        titlesMap.clear()

        val oldSize = backStack.size
        backStack.addAll(syntheticBackStack)

        var itemsRemoved = 0
        backStack.removeAll {
            itemsRemoved++ < oldSize
        }
    }

    val bottomSheetStrategy =
        remember { BottomSheetSceneStrategy<PanoRoute>(::removeAllModals) }

    // show onboarding again when logged out
    LaunchedEffect(currentUser == null) {
        if (currentUser == null && PanoRoute.Onboarding !in backStack)
            replaceRoutes(listOf(PanoRoute.Onboarding))
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
        delay(500)

        val changelog = Res.readBytes("files/changelog.md").decodeToString()
        val changelogHashcode = changelog.hashCode()
        val storedHashcode = PlatformStuff.mainPrefs.data.map { it.changelogSeenHashcode }.first()

        if (storedHashcode != changelogHashcode) {
            if (storedHashcode != null) {
                navigate(PanoRoute.Modal.Changelog(changelog))
            }
            // else is fresh install
            PlatformStuff.mainPrefs.updateData { it.copy(changelogSeenHashcode = changelogHashcode) }
        }
    }

    NavFromOutsideEffect(
        onNavigate = {
            // disable deeplinks if logged out
            if (currentUser != null)
                navigate(it)
        },
        isAndroidDialogActivity = false
    )

    setSingletonImageLoaderFactory { context ->
        newImageLoader(context)
    }

    CompositionLocalProvider(LocalNavigationType provides navigationType) {
        val currentNavType = LocalNavigationType.current

        Surface {
            key(locale) {
                Row(Modifier.fillMaxSize()) {
                    if (currentNavType != PanoNavigationType.BOTTOM_NAVIGATION && currentUser != null) {
                        PanoNavigationRail(
                            tabs = tabData.orEmpty(),
                            selectedTabIdx = tabIdxMap.getOrDefault(currentPanoRoute, 0),
                            fabData = fabData,
                            onNavigate = ::navigate,
                            onBack = ::goBack,
                            onTabClicked = { pos ->
                                (currentPanoRoute as? PanoRoute.HasTabs)?.let {
                                    tabIdxMap[it] = pos
                                }
                            },
                            onProfileClicked = {
                                navigate(
                                    PanoRoute.Modal.NavPopup(
                                        otherUser = currentUser.takeIf { currentUser != userSelf },
                                        initialDrawerData = drawerDataMap.getOrElse(currentUser) {
                                            DrawerData(0)
                                        }
                                    )
                                )
                            },
                            user = currentUser,
                            profilePicUrlOverride = profilePicUrlOverride.takeIf { currentUser == userSelf }
                        )
                    }

                    Box(
                        contentAlignment = Alignment.TopCenter,
                        modifier = Modifier
                            .fillMaxSize()
                            .focusGroup()
                    ) {
                        Scaffold(
                            modifier = Modifier
                                .widthIn(max = 1020.dp)
                                .pullToRefresh(
                                    state = pullToRefreshState,
                                    isRefreshing = pullToRefreshStateForSelfHomePager.values.any { it == PanoPullToRefreshStateForTab.Refreshing },
                                    enabled = (!PlatformStuff.isDesktop && !PlatformStuff.isTv) && !pullToRefreshStateForSelfHomePager.values.all { it == PanoPullToRefreshStateForTab.Disabled },
                                    onRefresh = {
                                        // find the right tab
                                        pullToRefreshStateForSelfHomePager.entries
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
                                    titlesMap[currentPanoRoute] ?: "",
                                    scrollBehavior = topBarScrollBehavior,
                                    showBack = !PlatformStuff.isTv && backStack.count { it !is PanoRoute.Modal } > 1,
                                    onBack = ::goBack,
                                )
                            },
                            bottomBar = {
                                if (currentNavType == PanoNavigationType.BOTTOM_NAVIGATION && tabData != null && currentUser != null) {
                                    PanoBottomNavigationBar(
                                        tabs = tabData,
                                        selectedTabIdx = tabIdxMap.getOrDefault(
                                            currentPanoRoute,
                                            0
                                        ),
                                        onTabClicked = { pos ->
                                            (currentPanoRoute as? PanoRoute.HasTabs)?.let {
                                                tabIdxMap[it] = pos
                                            }
                                        },
                                        user = currentUser,
                                        onProfileClicked = {
                                            navigate(
                                                PanoRoute.Modal.NavPopup(
                                                    otherUser = currentUser.takeIf { currentUser != userSelf },
                                                    initialDrawerData = drawerDataMap.getOrElse(
                                                        currentUser
                                                    ) {
                                                        DrawerData(0)
                                                    }
                                                )
                                            )
                                        },
                                        profilePicUrlOverride = profilePicUrlOverride.takeIf { currentUser == userSelf }
                                    )
                                }
                            },
                            floatingActionButton = {
                                if (currentNavType == PanoNavigationType.BOTTOM_NAVIGATION) {
                                    fabData?.let { fabData ->
                                        PanoFab(
                                            fabData,
                                            onBack = ::goBack,
                                            onNavigate = ::navigate,
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
                                val topPadding =
                                    PaddingValues(top = innerPadding.calculateTopPadding())
                                val offsetDenominator = 4
                                val scaleFactor = 0.95f

                                val scaleInTransformOrigin = remember {
                                    TransformOrigin(
                                        0.15f,
                                        0.75f,
                                    )
                                }

                                val scaleOutTransformOrigin = remember {
                                    TransformOrigin(
                                        0.85f,
                                        0.75f,
                                    )
                                }

                                NavDisplay(
                                    backStack = backStack,
                                    onBack = ::goBack,
                                    entryDecorators = listOf(
                                        rememberSaveableStateHolderNavEntryDecorator(),
                                        rememberViewModelStoreNavEntryDecorator()
                                    ),
                                    sceneStrategy = bottomSheetStrategy then
                                            SinglePaneSceneStrategy(),
                                    transitionSpec = {
                                        ContentTransform(
                                            fadeIn() +
                                                    scaleIn(
                                                        initialScale = scaleFactor,
                                                        transformOrigin = scaleInTransformOrigin
                                                    ) +
                                                    slideIntoContainer(
                                                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                                                        initialOffset = { -it / offsetDenominator }
                                                    ),
                                            fadeOut() +
                                                    scaleOut(
                                                        targetScale = scaleFactor,
                                                        transformOrigin = scaleOutTransformOrigin
                                                    ) +
                                                    slideOutOfContainer(
                                                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                                        targetOffset = { it / offsetDenominator }
                                                    )
                                        )
                                    },
                                    popTransitionSpec = {
                                        ContentTransform(
                                            fadeIn() +
                                                    scaleIn(
                                                        initialScale = scaleFactor,
                                                        transformOrigin = scaleInTransformOrigin
                                                    ) +
                                                    slideIntoContainer(
                                                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                                        animationSpec = spring(),
                                                        initialOffset = { -it / offsetDenominator }
                                                    ),
                                            fadeOut() +
                                                    scaleOut(
                                                        targetScale = scaleFactor,
                                                        transformOrigin = scaleOutTransformOrigin
                                                    ) +
                                                    slideOutOfContainer(
                                                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                                                        targetOffset = { it / offsetDenominator }
                                                    ),
                                        )
                                    },
                                    predictivePopTransitionSpec = {
                                        ContentTransform(
                                            fadeIn() +
                                                    scaleIn(
                                                        initialScale = scaleFactor,
                                                        transformOrigin = scaleInTransformOrigin
                                                    ) +
                                                    slideIntoContainer(
                                                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                                        initialOffset = { -it / offsetDenominator }
                                                    ),
                                            fadeOut() +
                                                    scaleOut(
                                                        targetScale = scaleFactor,
                                                        transformOrigin = scaleOutTransformOrigin
                                                    ) +
                                                    slideOutOfContainer(
                                                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                                                        targetOffset = { it / offsetDenominator }
                                                    ),
                                        )
                                    },
                                    entryProvider = PanoNavGraph.panoNavEntryProvider(
                                        onSetTitle = { route, title ->
                                            titlesMap[route] = title
                                        },
                                        getTabIdx = { route, default ->
                                            tabIdxMap.getOrPut(route) { default }
                                        },
                                        onSetTabIdx = { route, it ->
                                            tabIdxMap[route] = it
                                        },
                                        navigate = ::navigate,
                                        goBack = ::goBack,
                                        onSetOnboardingFinished = {
                                            replaceRoutes(listOf(PanoRoute.SelfHomePager()))
                                        },
                                        pullToRefreshState = { pullToRefreshState },
                                        onSetRefreshing = { id, prState ->
                                            pullToRefreshStateForSelfHomePager[id] = prState
                                        },
                                        onSetDrawerData = { drawerData ->
                                            currentUser?.let {
                                                drawerDataMap[it] = drawerData
                                            }
                                        },
                                        mainViewModel = viewModel,
                                    ),
                                    modifier = Modifier
                                        .padding(topPadding)
                                        .consumeWindowInsets(topPadding),
                                )
                            }
                        }
                    }
                }

//            if (BuildKonfig.DEBUG && PlatformStuff.isTv) {
//                Box(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .padding(vertical = 27.dp, horizontal = 48.dp)
//                        .border(1.dp, MaterialTheme.colorScheme.error)
//                )
//            }
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
    modifier: Modifier = Modifier,
) {
    if (LocalNavigationType.current == PanoNavigationType.PERMANENT_NAVIGATION_DRAWER) {
        SmallExtendedFloatingActionButton(
            onClick = {
                if (fabData.route == null)
                    onBack()
                else
                    onNavigate(fabData.route)
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
                if (fabData.route == null)
                    onBack()
                else
                    onNavigate(fabData.route)
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
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
) {
    if (PlatformStuff.isTv) {
        TopAppBar(
            modifier = modifier,
            title = {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp) // for TV overscan
                )
            },
            scrollBehavior = scrollBehavior
        )
    } else {
        MediumFlexibleTopAppBar(
            modifier = modifier,
            title = {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.primary,
                )
            },
            navigationIcon = {
                AnimatedVisibility(
                    visible = showBack,
                    enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
                    exit = shrinkOut(shrinkTowards = Alignment.Center) + fadeOut(),
                ) {
                    IconButton(
                        onClick = onBack,
                        enabled = showBack
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.ArrowBack,
                            contentDescription = stringResource(Res.string.back)
                        )
                    }
                }
            },
            scrollBehavior = scrollBehavior
        )
    }
}

@Composable
private fun PanoNavigationRail(
    tabs: List<PanoTab>,
    selectedTabIdx: Int,
    fabData: PanoFabData?,
    onBack: () -> Unit,
    onNavigate: (PanoRoute) -> Unit,
    onTabClicked: (pos: Int) -> Unit,
    onProfileClicked: (UserCached) -> Unit,
    user: UserCached,
    profilePicUrlOverride: String?,
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
        header =
            if (fabData != null && (fabData.showOnTv && PlatformStuff.isTv || !PlatformStuff.isTv)) {
                {
                    PanoFab(
                        fabData,
                        onBack = onBack,
                        onNavigate = onNavigate,
                        modifier = Modifier
                            .padding(start = horizontalOverscanPadding() / 2, top = 16.dp)
                    )
                }
            } else
                null,
        arrangement = Arrangement.aligned(Alignment.CenterVertically),
        modifier = modifier
            .widthIn(max = 200.dp)
            .fillMaxHeight()
    ) {
        (
                tabs +
                        if (tabs.find { it is PanoTab.Profile } == null) {
                            listOf(PanoTab.Profile)
                        } else {
                            emptyList()
                        }
                )
            .forEachIndexed { index, tabMetadata ->

                if (tabMetadata is PanoTab.Profile) {
                    Spacer(
                        modifier = Modifier
                            .height(4.dp)
                    )
                }

                WideNavigationRailItem(
                    selected = index == selectedTabIdx,
                    onClick = {
                        if (tabMetadata is PanoTab.Profile) {
                            onProfileClicked(user)
                            return@WideNavigationRailItem
                        }

                        if (index != selectedTabIdx) {
                            onTabClicked(index)
                        }
                    },
                    railExpanded = state.targetValue == WideNavigationRailValue.Expanded,
                    icon = {
                        if (tabMetadata is PanoTab.Profile) {

                            AvatarOrInitials(
                                avatarUrl = profilePicUrlOverride ?: user.largeImage,
                                avatarName = user.name,
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
                            text = if (tabMetadata is PanoTab.Profile)
                                if (Stuff.isInDemoMode)
                                    "me"
                                else
                                    user.name
                            else
                                stringResource(tabMetadata.titleRes),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
    }
}

@Composable
private fun PanoBottomNavigationBar(
    tabs: List<PanoTab>,
    selectedTabIdx: Int,
    onTabClicked: (pos: Int) -> Unit,
    onProfileClicked: () -> Unit,
    user: UserCached,
    profilePicUrlOverride: String?,
) {
    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
    ) {
        tabs.forEachIndexed { index, tabMetadata ->

            NavigationBarItem(
                selected = index == selectedTabIdx,
                onClick = {
                    if (tabMetadata is PanoTab.Profile) {
                        onProfileClicked()
                        return@NavigationBarItem
                    }

                    if (index != selectedTabIdx) {
                        onTabClicked(index)
                    }
                },
                icon = {
                    if (tabMetadata is PanoTab.Profile) {

                        AvatarOrInitials(
                            avatarUrl = profilePicUrlOverride ?: user.largeImage,
                            avatarName = user.name,
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
                                user.name
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