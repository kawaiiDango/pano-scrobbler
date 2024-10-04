package com.arn.scrobble.main

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.window.core.layout.WindowWidthSizeClass
import coil3.compose.setSingletonImageLoaderFactory
import com.arn.scrobble.R
import com.arn.scrobble.imageloader.newImageLoader
import com.arn.scrobble.navigation.LocalNavigationType
import com.arn.scrobble.navigation.PanoFabData
import com.arn.scrobble.navigation.PanoNavigationType
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.navigation.PanoTabType
import com.arn.scrobble.navigation.PanoTabs
import com.arn.scrobble.navigation.getFabData
import com.arn.scrobble.navigation.getTabData
import com.arn.scrobble.navigation.panoNavGraph
import com.arn.scrobble.ui.LocalInnerPadding
import com.arn.scrobble.ui.PanoSnackbarVisuals
import com.arn.scrobble.ui.horizontalOverscanPadding
import com.arn.scrobble.ui.verticalOverscanPadding
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.flow.collectLatest


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanoAppContent(
    navController: NavHostController,
    viewModel: MainViewModel = viewModel(),
) {
    val widthSizeClass = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
    val context = LocalContext.current
    val navigationType = when (widthSizeClass) {
        WindowWidthSizeClass.COMPACT -> PanoNavigationType.BOTTOM_NAVIGATION

        WindowWidthSizeClass.MEDIUM -> PanoNavigationType.NAVIGATION_RAIL

        WindowWidthSizeClass.EXPANDED -> PanoNavigationType.PERMANENT_NAVIGATION_DRAWER

        else -> PanoNavigationType.PERMANENT_NAVIGATION_DRAWER
    }

    var optionsMenuShown by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val currentBackStackEntry by navController.currentBackStackEntryAsState()

    var canPop by remember { mutableStateOf(false) }
    var currentTitle by remember { mutableStateOf<String?>(null) }
    var fabData by remember { mutableStateOf<PanoFabData?>(null) }
    var tabData by remember { mutableStateOf<List<PanoTabs>?>(null) }

    val topBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val bottomBarScrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()

    LaunchedEffect(Unit) {
        Stuff.globalSnackbarFlow.collectLatest {
            snackbarHostState.showSnackbar(it)
        }
    }

    DisposableEffect(currentBackStackEntry) {
        currentBackStackEntry?.destination?.let { dest ->
            fabData = getFabData(dest)
            tabData = getTabData(dest)
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
                        selectedTabIdx = 0,
                        fabData = fabData,
                        onNavigate = { navController.navigate(it) },
                        onBack = { navController.popBackStack() },
                        onTabClicked = { pos, tab ->
                            //                       viewModel.onTabClicked(pos)
                        },
                        onMenuClicked = {
                            optionsMenuShown = true
                        }
                    )
                } else if (currentNavType == PanoNavigationType.PERMANENT_NAVIGATION_DRAWER) {
                    PanoNavigationDrawerContent(
                        tabs = tabData ?: emptyList(),
                        selectedTabIdx = 0,
                        fabData = fabData,
                        onNavigate = { navController.navigate(it) },
                        onBack = { navController.popBackStack() },
                        onTabClicked = { pos, tab ->
                            //                       viewModel.onTabClicked(pos)
                        },
                        onMenuClicked = {
                            optionsMenuShown = true
                        }
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
                        onBack = { navController.popBackStack() },
                    )
                },
                bottomBar = {
                    if (currentNavType == PanoNavigationType.BOTTOM_NAVIGATION) {
                        tabData?.let { tabData ->
                            PanoBottomNavigationBar(
                                tabs = tabData,
                                selectedTabIdx = 0,
                                onTabClicked = { pos, tab ->
                                    //                       viewModel.onTabClicked(pos)
                                },
                                scrollBehavior = bottomBarScrollBehavior,
                                onMenuClicked = {
                                    optionsMenuShown = true
                                }
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
                        startDestination = PanoRoute.Placeholder,
                        modifier = Modifier
                            .padding(topPadding)
                            .consumeWindowInsets(topPadding)
                    ) {
                        panoNavGraph(
                            onSetTitle = { title ->
                                currentTitle = title?.let { context.getString(it) }
                            },
                            navigate = { navController.navigate(it) },
                            goBack = { navController.popBackStack() },
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
    modifier: Modifier = Modifier
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
            AnimatedVisibility(visible = showBack) {
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
) {
//    val tabMetadatas by viewModel.tabMetadatas.collectAsState()
//    var selectedTab by viewModel.selectedTab

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
                    if (tabMetadata.type == PanoTabType.BUTTON) {
                        onMenuClicked()
                        return@NavigationRailItem
                    }

                    if (index != selectedTabIdx) {
                        onTabClicked(index, tabMetadata)
                    }
                },
                icon = {
                    Icon(
                        imageVector = tabMetadata.icon,
                        contentDescription = stringResource(tabMetadata.titleRes)
                    )
                },
                label = {
                    Text(
                        text = stringResource(tabMetadata.titleRes),
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
) {
    BottomAppBar(
        modifier = Modifier.fillMaxWidth(),
        scrollBehavior = scrollBehavior
    ) {
        tabs.forEachIndexed { index, tabMetadata ->

            NavigationBarItem(
                selected = index == selectedTabIdx,
                onClick = {
                    if (tabMetadata.type == PanoTabType.BUTTON) {
                        onMenuClicked()
                        return@NavigationBarItem
                    }

                    if (index != selectedTabIdx) {
                        onTabClicked(index, tabMetadata)
                    }
                },
                icon = {
                    Icon(
                        imageVector = tabMetadata.icon,
                        contentDescription = stringResource(tabMetadata.titleRes)
                    )
                },
                label = {
                    Text(
                        text = stringResource(tabMetadata.titleRes),
                    )
                }
            )
        }
    }

}

//
@Composable
private fun PanoNavigationDrawerContent(
    tabs: List<PanoTabs>,
    selectedTabIdx: Int,
    fabData: PanoFabData?,
    onBack: () -> Unit,
    onNavigate: (PanoRoute) -> Unit,
    onTabClicked: (pos: Int, PanoTabs) -> Unit,
    onMenuClicked: () -> Unit,
) {
//    val tabMetadatas by viewModel.tabMetadatas.collectAsState()
//    var selectedTab by viewModel.selectedTab
//    val optionsMenuNavItemData by viewModel.optionsMenuNavItemData.collectAsState()
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
//            ProfileHeader(
//                viewModel,
//                modifier = Modifier
//                    .padding(bottom = 24.dp)
//                    .background(MaterialTheme.colorScheme.inverseOnSurface)
//
//            )

            if (tabs.isNotEmpty())
                HorizontalDivider(
                    modifier = Modifier.padding(
                        start = horizontalOverscanPadding(),
                        end = 8.dp,
                        top = 4.dp,
                        bottom = 4.dp
                    )
                )

            tabs.filter { it.type != PanoTabType.BUTTON }
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
                                contentDescription = stringResource(tabMetadata.titleRes)
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

//            if (!optionsMenuNavItemData.isNullOrEmpty())
//                HorizontalDivider(modifier = Modifier.padding(0.dp, 4.dp))
//
//            optionsMenuNavItemData?.let {
//                MenuNavItems(navigator, it)
//            }
        }
    }
}
