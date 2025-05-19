package com.arn.scrobble.main

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.derivedStateOf
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
import com.arn.scrobble.navigation.NavFromTrayEffect
import com.arn.scrobble.navigation.PanoDialog
import com.arn.scrobble.navigation.PanoFabData
import com.arn.scrobble.navigation.PanoNavMetadata
import com.arn.scrobble.navigation.PanoNavigationType
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.navigation.PanoTab
import com.arn.scrobble.navigation.ProfileHeader
import com.arn.scrobble.navigation.getFabData
import com.arn.scrobble.navigation.hasNavMetadata
import com.arn.scrobble.navigation.hasTabMetadata
import com.arn.scrobble.navigation.jsonSerializableSaver
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

    val titlesMap = remember { mutableStateMapOf<String, String>() }
    val tabData = remember { mutableStateMapOf<String, List<PanoTab>>() }
    var navMetadata by remember { mutableStateOf<List<PanoNavMetadata>?>(null) }
    var selectedTabIdx by rememberSaveable { mutableIntStateOf(0) }
    var currentDialogArgs by rememberSaveable(saver = jsonSerializableSaver<PanoDialog?>()) {
        mutableStateOf(null)
    }
    val drawerData by viewModel.drawerDataFlow.collectAsStateWithLifecycle()
    val currentUserSelf by PlatformStuff.mainPrefs.data
        .collectAsStateWithInitialValue { pref ->
            pref.scrobbleAccounts
                .firstOrNull { it.type == pref.currentAccountType }?.user
        }
    var onboardingFinished by rememberSaveable { mutableStateOf(currentUserSelf != null) }
    var currentUserOther by rememberSaveable(saver = jsonSerializableSaver()) {
        mutableStateOf<UserCached?>(null)
    }
    val currentUser by remember { derivedStateOf { currentUserOther ?: currentUserSelf } }

    val topBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val pullToRefreshState = rememberPullToRefreshState()
    val pullToRefreshStateForTabs =
        remember { mutableStateMapOf<Int, PanoPullToRefreshStateForTab>() }

    val canPop by remember(currentBackStackEntry) { mutableStateOf(navController.previousBackStackEntry != null) }
    val fabData by remember(currentBackStackEntry) {
        mutableStateOf(getFabData(currentBackStackEntry?.destination))
    }

    val showNavMetadata by remember(currentBackStackEntry) {
        mutableStateOf(
            hasNavMetadata(
                currentBackStackEntry?.destination
            )
        )
    }

    val showTabs by remember(currentBackStackEntry) {
        mutableStateOf(
            hasTabMetadata(
                currentBackStackEntry?.destination
            )
        )
    }

    LaunchedEffect(Unit) {
        Stuff.globalSnackbarFlow.collectLatest {
            snackbarHostState.showSnackbar(it)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.updateAvailability.collectLatest {
            currentDialogArgs = PanoDialog.UpdateAvailable(it)
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
        PermanentNavigationDrawer(
            drawerContent = {
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
                            selectedTabIdx = pos
                        },
                        onProfileClicked = {
                            currentDialogArgs = PanoDialog.NavPopup(otherUser = currentUserOther)
                        },
                        userp = currentUser,
                        drawerData = drawerData,
                    )
                }
            },
        ) {

            Scaffold(
                modifier = Modifier
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
                                selectedTabIdx = pos
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
                            onSetTitle = { id, title ->
                                if (title != null)
                                    titlesMap[id] = title
                                else
                                    titlesMap.remove(id)
                            },
                            tabIdx = { selectedTabIdx },
                            onSetTabData = { id, it ->
                                if (it != null)
                                    tabData[id] = it
                                else
                                    tabData.remove(id)
                            },
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
                            onOpenDialog = { currentDialogArgs = it },
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

            PanoDialogStack(
                initialDialogArgs = currentDialogArgs,
                onNavigate = navController::navigate,
                onDismiss = { currentDialogArgs = null },
                navMetadataList = { navMetadata },
                mainViewModel = viewModel,
            )
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
                    )
                }
            }
        },
        modifier = Modifier
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
//        scrollBehavior = scrollBehavior
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
                            avatarInitialLetter = tabMetadata.user.name.first(),
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
                            tabMetadata.user.name
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
    tabs: List<PanoTab>,
    selectedTabIdx: Int,
    fabData: PanoFabData?,
    navMetadataList: List<PanoNavMetadata>,
    onBack: () -> Unit,
    onNavigate: (PanoRoute) -> Unit,
    onOpenDialog: (PanoDialog) -> Unit,
    onTabClicked: (pos: Int, PanoTab) -> Unit,
    drawSnowfall: Boolean,
    otherUserp: UserCached?,
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
            val otherUser = tabs
                .filterIsInstance<PanoTab.Profile>()
                .firstOrNull()
                ?.user
                ?.takeIf { !it.isSelf }
                ?: otherUserp

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
                    onOpenDialog = onOpenDialog,
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .align(Alignment.CenterHorizontally)
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

            tabs.filter { it !is PanoTab.Profile }
                .forEachIndexed { index, tabMetadata ->
                    NavigationDrawerItem(
                        selected = index == selectedTabIdx,
                        label = {
                            Text(
                                text = stringResource(tabMetadata.titleRes),
                                fontWeight = if (index == selectedTabIdx)
                                    FontWeight.Bold
                                else
                                    null,
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
                            maxLines = 2,
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