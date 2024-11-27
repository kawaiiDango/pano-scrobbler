package com.arn.scrobble.main

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.AnimationConstants.DefaultDurationMillis
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.window.core.layout.WindowWidthSizeClass
import co.touchlab.kermit.Logger
import coil3.compose.setSingletonImageLoaderFactory
import com.arn.scrobble.R
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.friends.UserCached
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
import com.arn.scrobble.ui.PanoSnackbarVisuals
import com.arn.scrobble.ui.horizontalOverscanPadding
import com.arn.scrobble.ui.verticalOverscanPadding
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanoAppContent(
    navController: NavHostController,
    viewModel: MainViewModel = viewModel(),
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
    var currentUserSelf by remember { mutableStateOf<UserCached?>(null) }
    var currentUserOther by remember { mutableStateOf<UserCached?>(null) }
    val currentUser by remember { derivedStateOf { currentUserOther ?: currentUserSelf } }
    var userLoaded by remember { mutableStateOf(false) }
    var loginChangedTrigger by rememberSaveable { mutableIntStateOf(0) }

    val topBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val bottomBarScrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()

    LaunchedEffect(Unit) {
        Scrobblables.current
            .map { it?.userAccount?.user }
            .collectLatest {
                val prev = currentUserSelf
                currentUserSelf = it
                userLoaded = true

                if (it == null && prev != null)
                    loginChangedTrigger++
            }
    }

    LaunchedEffect(Unit) {
        Stuff.globalSnackbarFlow.collectLatest {
            snackbarHostState.showSnackbar(it)
        }
    }

    DisposableEffect(currentBackStackEntry) {
        currentBackStackEntry?.destination?.let { dest ->
            fabData = getFabData(dest)
            tabData = getTabData(dest)
            if (currentUser != null && !dest.hasRoute(PanoRoute.NavPopup::class)) {
                // don't consider the nav popup for nav metadata
                showNavMetadata = hasNavMetadata(dest)
            }
            canPop = navController.previousBackStackEntry != null
        }
        onDispose { }
    }

    setSingletonImageLoaderFactory { context ->
        newImageLoader(context)
    }

    if (!userLoaded) return

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
                        showBack = !Stuff.isTv && canPop,
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

                    NavHost(
                        navController = navController,
                        startDestination = remember(loginChangedTrigger) {
                            if (currentUser == null)
                                PanoRoute.Onboarding
                            else
                                PanoRoute.HomePager(currentUser!!)
                        },
                        enterTransition = {
                            slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                animationSpec = tween()
                            )
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                animationSpec = tween()
                            )
                        },
                        popEnterTransition = {
                            slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = tween()
                            )
                        },
                        popExitTransition = {
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = tween()
                            )
                        },
                        modifier = Modifier
                            .padding(topPadding)
                            .consumeWindowInsets(topPadding)
                    ) {
                        panoNavGraph(
                            onSetTitle = { currentTitle = it },
                            tabIdxFlow = snapshotFlow { selectedTabIdx },
                            onSetTabIdx = { selectedTabIdx = it },
                            navigate = navController::navigate,
                            goBack = navController::popBackStack,
                            goUp = navController::navigateUp,
                            onLoginChanged = { loginChangedTrigger++ },
                            onSetOtherUser = { currentUserOther = it },
                            navMetadataList = { navMetadata },
                            onSetNavMetadataList = { navMetadata = it },
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
                        contentDescription = stringResource(R.string.back)
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
        modifier = Modifier
            .fillMaxHeight()
            .padding(vertical = verticalOverscanPadding())
    ) {
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
        tabs.forEachIndexed { index, tabMetadata ->
            NavigationRailItem(
                selected = index == selectedTabIdx,
                onClick = {
                    if (tabMetadata.type != PanoTabType.TAB) {
                        onMenuClicked()
                        return@NavigationRailItem
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
                        text = if (tabMetadata.type == PanoTabType.PROFILE) user?.name
                            ?: "" else stringResource(tabMetadata.titleRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
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
                        text = if (tabMetadata.type == PanoTabType.PROFILE) user?.name
                            ?: "" else stringResource(tabMetadata.titleRes),
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
