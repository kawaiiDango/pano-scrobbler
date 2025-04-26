package com.arn.scrobble.main

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.BottomAppBarScrollBehavior
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.window.core.layout.WindowWidthSizeClass
import coil3.compose.setSingletonImageLoaderFactory
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.api.DrawerData
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.imageloader.newImageLoader
import com.arn.scrobble.navigation.LocalNavigationType
import com.arn.scrobble.navigation.PanoFabData
import com.arn.scrobble.navigation.PanoNavMetadata
import com.arn.scrobble.navigation.PanoNavigationType
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.navigation.PanoTabType
import com.arn.scrobble.navigation.PanoTabs
import com.arn.scrobble.navigation.ProfileHeader
import com.arn.scrobble.navigation.getFabData
import com.arn.scrobble.navigation.getTabData
import com.arn.scrobble.navigation.hasNavMetadata
import com.arn.scrobble.navigation.panoNavGraph
import com.arn.scrobble.ui.AvatarOrInitials
import com.arn.scrobble.ui.LocalInnerPadding
import com.arn.scrobble.ui.PanoPullToRefreshStateForTab
import com.arn.scrobble.ui.PanoSnackbarVisuals
import com.arn.scrobble.ui.horizontalOverscanPadding
import com.arn.scrobble.ui.verticalOverscanPadding
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.back


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanoAppContent(
    navController: NavHostController,
    customInitialRoute: PanoRoute? = null,
    viewModel: MainViewModel = viewModel { MainViewModel() },
) {

    val widthSizeClass = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
    val navigationType = when (widthSizeClass) {
        WindowWidthSizeClass.COMPACT -> PanoNavigationType.BOTTOM_NAVIGATION

        WindowWidthSizeClass.MEDIUM -> PanoNavigationType.NAVIGATION_RAIL

        WindowWidthSizeClass.EXPANDED -> PanoNavigationType.PERMANENT_NAVIGATION_DRAWER

        else -> PanoNavigationType.PERMANENT_NAVIGATION_DRAWER
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val currentBackStackEntry by navController.currentBackStackEntryAsState()

    var canPop by remember { mutableStateOf(false) }
    var currentTitle by remember { mutableStateOf<String?>(null) }
    var fabData by remember { mutableStateOf<PanoFabData?>(null) }
    var tabData by remember { mutableStateOf<List<PanoTabs>?>(null) }
    var navMetadata by remember { mutableStateOf<List<PanoNavMetadata>?>(null) }
    var showNavMetadata by remember { mutableStateOf(false) }
    var selectedTabIdx by rememberSaveable { mutableIntStateOf(0) }
    val drawerData by viewModel.drawerDataFlow.collectAsStateWithLifecycle()
    val accountType by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.currentAccountType }
    val currentUserSelf by PlatformStuff.mainPrefs.data
        .collectAsStateWithInitialValue { pref ->
            pref.scrobbleAccounts
                .firstOrNull { it.type == pref.currentAccountType }?.user
        }
    var onboardingFinished by remember { mutableStateOf(currentUserSelf != null) }
    var currentUserOther by remember { mutableStateOf<UserCached?>(null) }
    val currentUser by remember { derivedStateOf { currentUserOther ?: currentUserSelf } }

    val topBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val bottomBarScrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()
    val pullToRefreshState = rememberPullToRefreshState()
    val pullToRefreshStateForTabs =
        remember { mutableStateMapOf<Int, PanoPullToRefreshStateForTab>() }

    LaunchedEffect(Unit) {
        Stuff.globalSnackbarFlow.collectLatest {
            snackbarHostState.showSnackbar(it)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.updateAvailability.collectLatest {
            navController.navigate(
                PanoRoute.UpdateAvailable(it),
            )
        }
    }

    // show changelog if needed
    LaunchedEffect(Unit) {
        delay(2000)

        val changelogHashcode = BuildKonfig.CHANGELOG.hashCode()
        val storedHashcode = PlatformStuff.mainPrefs.data.map { it.changelogSeenHashcode }.first()

        if (storedHashcode != changelogHashcode) {
            if (storedHashcode != null) {
                navController.navigate(PanoRoute.Changelog)
            }
            // else is fresh install
            PlatformStuff.mainPrefs.updateData { it.copy(changelogSeenHashcode = changelogHashcode) }
        }
    }

    DisposableEffect(currentBackStackEntry) {
        currentBackStackEntry?.destination?.let { dest ->
//            dest.navigatorName is either "composable" or "dialog"
            if (currentUser != null && dest.navigatorName == "composable") {
                // don't consider the nav popup for nav metadata
                fabData = getFabData(dest)
                tabData = getTabData(dest, accountType)
                showNavMetadata = hasNavMetadata(dest)
            }
            canPop = navController.previousBackStackEntry != null
        }
        onDispose { }
    }

    setSingletonImageLoaderFactory { context ->
        newImageLoader(context)
    }

    CompositionLocalProvider(LocalNavigationType provides navigationType) {
        val currentNavType = LocalNavigationType.current
        PermanentNavigationDrawer(
            drawerContent = {
                if (currentNavType == PanoNavigationType.NAVIGATION_RAIL) {
                    PanoNavigationRail(
                        tabs = tabData ?: emptyList(),
                        selectedTabIdx = selectedTabIdx,
                        fabData = fabData,
                        onNavigate = { navController.navigate(it) },
                        onBack = { navController.popBackStack() },
                        onTabClicked = { pos, tab ->
                            selectedTabIdx = pos
                        },
                        onMenuClicked = {
                            navController.navigate(
                                PanoRoute.NavPopup(
                                    otherUser = currentUserOther,
                                )
                            )
                        },
                        user = currentUser,
                        drawerData = drawerData,
                    )
                } else if (currentNavType == PanoNavigationType.PERMANENT_NAVIGATION_DRAWER) {
                    PanoNavigationDrawerContent(
                        tabs = tabData ?: emptyList(),
                        selectedTabIdx = selectedTabIdx,
                        fabData = fabData,
                        onNavigate = { navController.navigate(it) },
                        onBack = { navController.popBackStack() },
                        onTabClicked = { pos, tab ->
                            selectedTabIdx = pos
                        },
                        otherUser = currentUserOther,
                        drawerData = drawerData,
                        drawSnowfall = viewModel.isItChristmas,
                        navMetadataList = navMetadata.takeIf { showNavMetadata } ?: emptyList(),
                    )
                }
            },
        ) {

            Scaffold(
                modifier = Modifier
                    .pullToRefresh(
                        state = pullToRefreshState,
                        isRefreshing = pullToRefreshStateForTabs.values.any { it == PanoPullToRefreshStateForTab.Refreshing },
                        enabled = !pullToRefreshStateForTabs.values.all { it == PanoPullToRefreshStateForTab.Disabled } &&
                                !PlatformStuff.isTv &&
                                !PlatformStuff.isDesktop,
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
                    .nestedScroll(topBarScrollBehavior.nestedScrollConnection)
                    .then(
                        if (currentNavType == PanoNavigationType.BOTTOM_NAVIGATION)
                            Modifier.nestedScroll(bottomBarScrollBehavior.nestedScrollConnection)
                        else
                            Modifier
                    ),
                topBar = {
                    PanoTopAppBar(
                        currentTitle ?: "",
                        scrollBehavior = { topBarScrollBehavior },
                        showBack = !PlatformStuff.isTv && canPop,
                        onBack = { navController.navigateUp() },
                    )
                },
                bottomBar = {
                    if (currentNavType == PanoNavigationType.BOTTOM_NAVIGATION) {
                        tabData?.let { tabData ->
                            PanoBottomNavigationBar(
                                tabs = tabData,
                                selectedTabIdx = selectedTabIdx,
                                onTabClicked = { pos, tab ->
                                    selectedTabIdx = pos
                                },
                                scrollBehavior = bottomBarScrollBehavior,
                                onMenuClicked = {
                                    navController.navigate(
                                        PanoRoute.NavPopup(
                                            otherUser = currentUserOther,
                                        )
                                    )
                                },
                                user = currentUser,
                                drawerData = drawerData,
                            )
                        }
                    }
                },
                floatingActionButton = {
                    if (currentNavType == PanoNavigationType.BOTTOM_NAVIGATION) {
                        fabData?.let { fabData ->
                            PanoFab(
                                fabData,
                                onBack = { navController.popBackStack() },
                                onNavigate = { navController.navigate(it) },
                            )
                        }
                    }
                },

                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                    ) { snackbarData ->
                        val visuals = snackbarData.visuals as PanoSnackbarVisuals
                        Snackbar(
                            snackbarData = snackbarData,
                            containerColor = if (visuals.isError) MaterialTheme.colorScheme.errorContainer else SnackbarDefaults.color,
                            contentColor = if (visuals.isError) MaterialTheme.colorScheme.onErrorContainer else SnackbarDefaults.contentColor,
                        )
                    }
                },
            ) { innerPadding ->
                CompositionLocalProvider(LocalInnerPadding provides innerPadding) {
                    val topPadding = PaddingValues(top = innerPadding.calculateTopPadding())
                    val offsetDenominator = 4

                    NavHost(
                        navController = navController,
                        startDestination = remember(onboardingFinished, currentUser) {
                            when {
                                !onboardingFinished || currentUser == null -> PanoRoute.Onboarding
                                customInitialRoute != null -> customInitialRoute
                                else -> PanoRoute.SelfHomePager
                            }
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
                            onSetTitle = { currentTitle = it },
                            tabIdxFlow = snapshotFlow { selectedTabIdx },
                            tabDataFlow = snapshotFlow { tabData },
                            onSetTabIdx = { selectedTabIdx = it },
                            navigate = navController::navigate,
                            goBack = navController::popBackStack,
                            goUp = navController::navigateUp,
                            onOnboardingFinished = {
                                if (!onboardingFinished)
                                    onboardingFinished = true
                                else // case when the user is logged in but some onboarding steps are not done
                                    navController.popBackStack()
                            },
                            onSetOtherUser = { currentUserOther = it },
                            navMetadataList = { navMetadata },
                            onSetNavMetadataList = { navMetadata = it },
                            pullToRefreshState = { pullToRefreshState },
                            onSetRefreshing = { id, prState ->
                                pullToRefreshStateForTabs[id] = prState
                            },
                            mainViewModel = viewModel,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PanoFab(
    fabData: PanoFabData,
    onBack: () -> Unit,
    onNavigate: (PanoRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (LocalNavigationType.current == PanoNavigationType.PERMANENT_NAVIGATION_DRAWER) {
        ExtendedFloatingActionButton(
            onClick = {
                if (fabData.route == PanoRoute.SpecialGoBack)
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
            modifier = modifier
        )
    } else {
        FloatingActionButton(
            onClick = {
                if (fabData.route == PanoRoute.SpecialGoBack)
                    onBack()
                else
                    onNavigate(fabData.route)
            },
            modifier = modifier
        ) {
            Icon(
                imageVector = fabData.icon,
                contentDescription = stringResource(fabData.stringRes)
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PanoTopAppBar(
    title: String,
    showBack: Boolean,
    onBack: () -> Unit,
    scrollBehavior: () -> TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
) {
    MediumTopAppBar(
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

@Composable
private fun PanoNavigationRail(
    tabs: List<PanoTabs>,
    selectedTabIdx: Int,
    fabData: PanoFabData?,
    onBack: () -> Unit,
    onNavigate: (PanoRoute) -> Unit,
    onTabClicked: (pos: Int, PanoTabs) -> Unit,
    onMenuClicked: () -> Unit,
    user: UserCached?,
    drawerData: DrawerData?,
) {
    val profilePicUrl by remember(user, drawerData) {
        mutableStateOf(
            when {
                user?.isSelf == false -> user.largeImage
                user?.isSelf == true && drawerData != null -> drawerData.profilePicUrl
                else -> null
            }
        )
    }

    NavigationRail(
        windowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Vertical + WindowInsetsSides.Start
        ),
        header = {
            Spacer(Modifier.padding(top = verticalOverscanPadding()))

            fabData?.let { fabData ->
                PanoFab(
                    fabData,
                    onBack = onBack,
                    onNavigate = onNavigate,
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                )
            }


            val lastTab = tabs.lastOrNull()
            if (lastTab?.type == PanoTabType.MENU) {
                NavigationRailItem(
                    selected = false,
                    onClick = onMenuClicked,
                    icon = {
                        Icon(
                            imageVector = lastTab.icon,
                            contentDescription = stringResource(lastTab.titleRes)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(lastTab.titleRes),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .width(80.dp)
                )
            } else if (user != null) {
                NavigationRailItem(
                    selected = false,
                    onClick = onMenuClicked,
                    icon = {
                        AvatarOrInitials(
                            avatarUrl = profilePicUrl,
                            avatarInitialLetter = user.name.first(),
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                        )
                    },
                    label = {
                        Text(
                            text = user.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .width(80.dp)
                )
            }
        },
        modifier = Modifier
//            .padding(vertical = verticalOverscanPadding())
            .fillMaxHeight()
    ) {
        Spacer(Modifier.weight(1f))

        tabs.filter { it.type == PanoTabType.TAB }
            .forEachIndexed { index, tabMetadata ->
                NavigationRailItem(
                    selected = index == selectedTabIdx,
                    onClick = {
//                    if (tabMetadata.type != PanoTabType.TAB) {
//                        onMenuClicked()
//                        return@NavigationRailItem
//                    }

                        if (index != selectedTabIdx) {
                            onTabClicked(index, tabMetadata)
                        }
                    },
                    icon = {
//                    if (tabMetadata.type == PanoTabType.PROFILE) {
//                        AvatarOrInitials(
//                            avatarUrl = profilePicUrl,
//                            avatarInitialLetter = user?.name?.first(),
//                            modifier = Modifier
//                                .size(24.dp)
//                                .clip(CircleShape)
//                        )
//                    } else
                        Icon(
                            imageVector = tabMetadata.icon,
                            contentDescription = stringResource(tabMetadata.titleRes)
                        )
                    },
                    label = {
                        Text(
                            text =
//                            if (tabMetadata.type == PanoTabType.PROFILE) user?.name
//                            ?: "" else
                                stringResource(tabMetadata.titleRes),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier
                        .width(80.dp)
                )
            }

        Spacer(Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PanoBottomNavigationBar(
    tabs: List<PanoTabs>,
    selectedTabIdx: Int,
    onTabClicked: (pos: Int, PanoTabs) -> Unit,
    scrollBehavior: BottomAppBarScrollBehavior,
    onMenuClicked: () -> Unit,
    user: UserCached?,
    drawerData: DrawerData?,
) {
    val profilePicUrl by remember(user, drawerData) {
        mutableStateOf(
            when {
                user?.isSelf == false -> user.largeImage
                user?.isSelf == true && drawerData != null -> drawerData.profilePicUrl
                else -> null
            }
        )
    }

    BottomAppBar(
        modifier = Modifier.fillMaxWidth(),
        scrollBehavior = scrollBehavior
    ) {
        tabs.forEachIndexed { index, tabMetadata ->

            NavigationBarItem(
                selected = index == selectedTabIdx,
                onClick = {
                    if (tabMetadata.type != PanoTabType.TAB) {
                        onMenuClicked()
                        return@NavigationBarItem
                    }

                    if (index != selectedTabIdx) {
                        onTabClicked(index, tabMetadata)
                    }
                },
                icon = {
                    if (tabMetadata.type == PanoTabType.PROFILE) {
                        AvatarOrInitials(
                            avatarUrl = profilePicUrl,
                            avatarInitialLetter = user?.name?.first(),
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
                        text = if (tabMetadata.type == PanoTabType.PROFILE)
                            (user?.name ?: "")
                        else
                            stringResource(tabMetadata.titleRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }

}

@Composable
private fun PanoNavigationDrawerContent(
    tabs: List<PanoTabs>,
    selectedTabIdx: Int,
    fabData: PanoFabData?,
    navMetadataList: List<PanoNavMetadata>,
    onBack: () -> Unit,
    onNavigate: (PanoRoute) -> Unit,
    onTabClicked: (pos: Int, PanoTabs) -> Unit,
    drawSnowfall: Boolean,
    otherUser: UserCached?,
    drawerData: DrawerData?,
) {
    PermanentDrawerSheet(
        windowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Vertical + WindowInsetsSides.Start
        )
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = verticalOverscanPadding())
                .width(240.dp)
        ) {
            ProfileHeader(
                otherUser = otherUser,
                drawerData = drawerData,
                compact = true,
                onNavigate = onNavigate,
                drawSnowfall = drawSnowfall,
                modifier = Modifier.padding(16.dp)
            )

            fabData?.let { fabData ->
                PanoFab(
                    fabData,
                    onBack = onBack,
                    onNavigate = onNavigate,
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .align(CenterHorizontally)
                )
            }
            if (tabs.isNotEmpty())
                HorizontalDivider(
                    modifier = Modifier.padding(
                        start = horizontalOverscanPadding(),
                        end = 8.dp,
                        top = 4.dp,
                        bottom = 4.dp
                    )
                )

            tabs.filter { it.type == PanoTabType.TAB }
                .forEachIndexed { index, tabMetadata ->
                    NavigationDrawerItem(
                        selected = index == selectedTabIdx,
                        label = {
                            Text(
                                text = stringResource(tabMetadata.titleRes),
                                fontWeight = if (index == selectedTabIdx)
                                    FontWeight.Bold
                                else null,
                                maxLines = 2
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = tabMetadata.icon,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            if (index != selectedTabIdx) {
                                onTabClicked(index, tabMetadata)
                            }
                        },
                        modifier = Modifier.padding(start = horizontalOverscanPadding(), end = 8.dp)
                    )
                }

            if (navMetadataList.isNotEmpty())
                HorizontalDivider(
                    modifier = Modifier.padding(
                        start = horizontalOverscanPadding(),
                        end = 8.dp,
                        top = 4.dp,
                        bottom = 4.dp
                    )
                )

            navMetadataList.forEach {
                NavigationDrawerItem(
                    selected = false,
                    label = {
                        Text(
                            text = stringResource(it.titleRes),
                            maxLines = 2
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = it.icon,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        onNavigate(it.route)
                    },
                    modifier = Modifier.padding(start = horizontalOverscanPadding(), end = 8.dp)
                )
            }
        }
    }
}
