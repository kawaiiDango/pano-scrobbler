package com.arn.scrobble.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarDefaults
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailItemDefaults
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.result.ResultEventBus
import androidx.navigation3.runtime.result.ResultEventBusNavEntryDecorator
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import coil3.compose.setSingletonImageLoaderFactory
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.icons.Close
import com.arn.scrobble.icons.Fullscreen
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.Minimize
import com.arn.scrobble.icons.Search
import com.arn.scrobble.icons.automirrored.ArrowBack
import com.arn.scrobble.imageloader.PanoImageLoader
import com.arn.scrobble.navigation.BottomSheetSceneStrategy
import com.arn.scrobble.navigation.FabClickedResult
import com.arn.scrobble.navigation.LocalNavigationType
import com.arn.scrobble.navigation.NavFromOutsideEffect
import com.arn.scrobble.navigation.PanoFabData
import com.arn.scrobble.navigation.PanoNavGraph
import com.arn.scrobble.navigation.PanoNavigationType
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.navigation.PanoTab
import com.arn.scrobble.navigation.PanoTab.Subtab
import com.arn.scrobble.navigation.SubTabClickedResult
import com.arn.scrobble.navigation.rememberPanoNavBackStack
import com.arn.scrobble.themes.LocalThemeAttributes
import com.arn.scrobble.ui.AvatarOrInitials
import com.arn.scrobble.ui.LocalAppBarBg
import com.arn.scrobble.ui.LocalInnerPadding
import com.arn.scrobble.ui.OutlinedToggleButtons
import com.arn.scrobble.ui.PanoPullToRefreshStateForTab
import com.arn.scrobble.ui.PanoSnackbarVisuals
import com.arn.scrobble.ui.PanoToggleButtonsMode
import com.arn.scrobble.ui.horizontalOverscanPadding
import com.arn.scrobble.updates.runUpdateAction
import com.arn.scrobble.utils.LocaleUtils
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.back
import pano_scrobbler.composeapp.generated.resources.close
import pano_scrobbler.composeapp.generated.resources.delete
import pano_scrobbler.composeapp.generated.resources.download
import pano_scrobbler.composeapp.generated.resources.expand
import pano_scrobbler.composeapp.generated.resources.minimize
import pano_scrobbler.composeapp.generated.resources.reload
import pano_scrobbler.composeapp.generated.resources.update_available
import pano_scrobbler.composeapp.generated.resources.update_downloaded
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanoAppContent(
    draggableWrapper: @Composable (content: @Composable (windowTitleActions: WindowTitleActions?) -> Unit) -> Unit = {
        it(null)
    },
    viewModel: MainViewModel = viewModel { MainViewModel() },
) {
    val sizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
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

    val density = LocalDensity.current
    val titlesMap = remember { mutableStateMapOf<PanoRoute, String>() }
    val tabIdxMap = remember { mutableStateMapOf<PanoRoute.HasTabs, Int>() }
    val searchTextMap = remember { mutableStateMapOf<PanoRoute.HasSearch, String>() }

//    val topBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(
//        state = rememberTopAppBarState(
//            initialHeightOffsetLimit = with(density) { -32.dp.toPx() }
//        )
//    )
    val pullToRefreshState = rememberPullToRefreshState()
    val pullToRefreshStateForSelfHomePager =
        remember { mutableStateMapOf<Int, PanoPullToRefreshStateForTab>() }

    val currentAccountType by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.currentAccountType }
    val userSelf by PlatformStuff.mainPrefs.data
        .collectAsStateWithInitialValue { it.currentAccount?.user }
    val bottomSheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    val scope = rememberCoroutineScope()
    var navRailWidth by remember { mutableStateOf(0.dp) }
    val searchFieldState = rememberTextFieldState()
    val searchFieldFocusRequester = remember { FocusRequester() }
    val resultEventBus = remember { ResultEventBus() }
    var modalTransitionJob: Job? = remember { null }

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

    val subTabData = remember(tabData, tabIdxMap[currentPanoRoute]) {
        tabData?.getOrNull(tabIdxMap.getOrDefault(currentPanoRoute, -1)) as? PanoTab.HasSubtabs
    }

    var selectedSubTabId by remember { mutableIntStateOf(-1) }

    fun goBack(): PanoRoute? {
        if (backStack.size <= 1)
            return null

        val route = backStack.lastOrNull()

        if (backStack.count { it is PanoRoute.Modal } == 1 && route is PanoRoute.Modal) {
            scope.launch {
                bottomSheetState.hide()
                backStack.remove(route)
            }
        } else {
            backStack.remove(route)
        }

        if (route is PanoRoute.HasTabs)
            tabIdxMap.remove(route)

        if (route is PanoRoute.HasSearch)
            searchTextMap.remove(route)

        if (route != null)
            titlesMap.remove(route)

        return route
    }

    fun removeAllModals() {
        backStack.removeAll { it is PanoRoute.Modal }
    }

    fun navigate(route: PanoRoute) {
        fun add() {
            val last = backStack.lastOrNull()
            if (last != route) {
                if (last is PanoRoute.HasSearch)
                    searchTextMap[last] = searchFieldState.text.toString()
                backStack.add(route)
            }
        }


        if (route !is PanoRoute.Modal && backStack.any { it is PanoRoute.Modal }) {
            modalTransitionJob = scope.launch {
                bottomSheetState.hide()
                removeAllModals()

                add()
            }
        } else if (modalTransitionJob?.isActive != true) {
            add()
        }
    }

    fun replaceRoutes(syntheticBackStack: List<PanoRoute>) {
        tabIdxMap.clear()
        titlesMap.clear()
        searchTextMap.clear()

        val oldSize = backStack.size
        backStack.addAll(syntheticBackStack)

        var itemsRemoved = 0
        backStack.removeAll {
            itemsRemoved++ < oldSize
        }
    }

    val bottomSheetStrategy =
        remember {
            BottomSheetSceneStrategy<PanoRoute>(
                false,
                bottomSheetState,
                ::removeAllModals
            )
        }
    val singlePaneStrategy = remember { SinglePaneSceneStrategy<PanoRoute>() }

    LaunchedEffect(currentPanoRoute) {
        if (currentPanoRoute is PanoRoute.HasSearch) {
            val searchText = searchTextMap[currentPanoRoute]

            if (searchText != null)
                searchFieldState.setTextAndPlaceCursorAtEnd(searchText)
            else {
                searchFieldState.clearText()
                if (currentPanoRoute is PanoRoute.SearchRequestsFocus)
                    searchFieldFocusRequester.requestFocus()
                else
                    searchFieldFocusRequester.freeFocus()
            }
        }
    }

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
        delay(500.milliseconds)

        val changelog = Res.readBytes("files/changelog.md").decodeToString()
        val changelogHashcode = changelog.hashCode()
        val storedHashcode = PlatformStuff.mainPrefs.data.map { it.changelogSeenHashcode }.first()

        if (storedHashcode != changelogHashcode) {
            if (storedHashcode != null && currentUser != null) { // don't show on onboarding
                navigate(PanoRoute.Modal.Changelog(changelog))
            }
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
        PanoImageLoader.newImageLoader(context)
    }

    CompositionLocalProvider(LocalNavigationType provides navigationType) {
        val currentNavType = LocalNavigationType.current

        key(locale) {
            val needsRoundedCorners = PlatformStuff.isDesktop &&
                    !LocalThemeAttributes.current.blurMainWindow &&
                    MaterialTheme.colorScheme.background.alpha < 1f

            val needsBackdropBlur = PlatformStuff.isDesktop &&
                    PlatformStuff.supportsBlur &&
                    LocalThemeAttributes.current.blurSubWindow &&
                    bottomSheetState.isVisible

            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
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
//                    .nestedScroll(topBarScrollBehavior.nestedScrollConnection)
                    .focusGroup()
                    .then(
                        if (needsRoundedCorners) Modifier.clip(MaterialTheme.shapes.medium) else Modifier
                    ).then(
                        if (needsBackdropBlur) Modifier.blur(radius = Stuff.BLUR_BACKDROP_RADIUS_DP.dp) else Modifier
                    ),
                topBar = {
                    draggableWrapper { windowTitleActions ->
                        PanoTopAppBar(
                            titlesMap[currentPanoRoute] ?: "",
//                            scrollBehavior = topBarScrollBehavior,
                            scrollBehavior = null,
                            innerPaddingStart = if (currentNavType != PanoNavigationType.BOTTOM_NAVIGATION) navRailWidth else 0.dp,
                            searchTextFieldState = if (currentPanoRoute is PanoRoute.HasSearch) searchFieldState else null,
                            searchFieldFocusRequester = searchFieldFocusRequester,
                            windowTitleActions = windowTitleActions,
                            showBack = !PlatformStuff.isTv && backStack.count { it !is PanoRoute.Modal } > 1,
                            subTabs = subTabData,
                            selectedSubTabId = selectedSubTabId,
                            resultEventBus = resultEventBus,
                            onBack = ::goBack,
                        )
                    }
                },
                bottomBar = {
                    val showBottomBar =
                        currentNavType == PanoNavigationType.BOTTOM_NAVIGATION && tabData != null && currentUser != null

                    if (currentUser != null) {
                        AnimatedVisibility(
                            visible = showBottomBar,
                            enter = fadeIn() + slideInVertically { it / 2 },
                            exit = slideOutVertically { it / 2 } + fadeOut(),
                        ) {
                            // to show at least something in the animation
                            var capturedTabData by remember { mutableStateOf(tabData.orEmpty()) }

                            LaunchedEffect(tabData) {
                                if (tabData != null)
                                    capturedTabData = tabData
                            }

                            PanoBottomAppBar(
                                tabs = capturedTabData,
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
                                            otherUser = currentUser.takeIf { currentUser != userSelf }
                                        )
                                    )
                                },
                            )
                        }
                    }
                },
                floatingActionButton = {
                    if (currentNavType == PanoNavigationType.BOTTOM_NAVIGATION) {
                        fabData?.let { fabData ->
                            PanoFab(
                                fabData,
                                resultEventBus,
                                onNavigate = ::navigate,
                                modifier = Modifier.imePadding()
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
                val topAppBarColors = TopAppBarDefaults.topAppBarColors()
                val appBarBg = topAppBarColors.containerColor
                /*
                val appBarBg by remember {
                    derivedStateOf {
                        val overlappingFraction = topBarScrollBehavior.state.overlappedFraction

                        if (overlappingFraction > 0.01f)
                            topAppBarColors.scrolledContainerColor.copy(
                                alpha = topAppBarColors.containerColor.alpha
                            )
                        else
                            topAppBarColors.containerColor
                    }
                }
                 */

                CompositionLocalProvider(
                    LocalInnerPadding provides innerPadding,
                    LocalAppBarBg provides appBarBg
                ) {
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

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        if (currentNavType != PanoNavigationType.BOTTOM_NAVIGATION) {
                            PanoNavigationRail(
                                tabs = tabData.orEmpty(),
                                selectedTabIdx = tabIdxMap.getOrDefault(currentPanoRoute, 0),
                                fabData = fabData,
                                onNavigate = ::navigate,
                                resultEventBus = resultEventBus,
                                onTabClicked = { pos ->
                                    (currentPanoRoute as? PanoRoute.HasTabs)?.let {
                                        tabIdxMap[it] = pos
                                    }
                                },
                                onProfileClicked = {
                                    if (currentUser != null) {
                                        navigate(
                                            PanoRoute.Modal.NavPopup(
                                                otherUser = currentUser.takeIf { currentUser != userSelf }
                                            )
                                        )
                                    }
                                },
                                user = currentUser,
                                modifier = Modifier
                                    .padding(topPadding)
                                    .consumeWindowInsets(topPadding)
                                    .onGloballyPositioned {
                                        navRailWidth = with(density) { it.size.width.toDp() }
                                    }
                            )
                        }

                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                        )

                        NavDisplay(
                            backStack = backStack,
                            onBack = ::goBack,
                            entryDecorators = listOf(
                                rememberSaveableStateHolderNavEntryDecorator(),
                                rememberViewModelStoreNavEntryDecorator(),
                                rememberPanoResultEventBusNavEntryDecorator(resultEventBus)
                            ),
                            sceneStrategies = listOf(
                                bottomSheetStrategy,
                                singlePaneStrategy
                            ),
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
                                selectSubTabId = {
                                    selectedSubTabId = it
                                },
                                searchFieldState = searchFieldState,
                                mainViewModel = viewModel,
                            ),
                            modifier = Modifier
                                .widthIn(max = 1020.dp)
                                .padding(topPadding)
                                .consumeWindowInsets(topPadding),
                        )


                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                        )
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


@Composable
private fun PanoFab(
    fabData: PanoFabData,
    resultEventBus: ResultEventBus,
    onNavigate: (PanoRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (LocalNavigationType.current == PanoNavigationType.PERMANENT_NAVIGATION_DRAWER) {
        SmallExtendedFloatingActionButton(
            onClick = {
                if (fabData.route == null)
                    resultEventBus.sendResult(FabClickedResult)
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
                    textAlign = TextAlign.Center
                )
            },
            modifier = modifier
        )
    } else {
        FloatingActionButton(
            onClick = {
                if (fabData.route == null)
                    resultEventBus.sendResult(FabClickedResult)
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

@Composable
private fun PanoTopAppBar(
    title: String,
    windowTitleActions: WindowTitleActions?,
    showBack: Boolean,
    onBack: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior?,
    searchTextFieldState: TextFieldState?,
    searchFieldFocusRequester: FocusRequester,
    innerPaddingStart: Dp,
    selectedSubTabId: Int,
    subTabs: PanoTab.HasSubtabs?,
    resultEventBus: ResultEventBus,
    modifier: Modifier = Modifier,
) {
    val colors = TopAppBarDefaults.topAppBarColors().let {
        if (it.containerColor.alpha == 1f)
            it.copy(titleContentColor = MaterialTheme.colorScheme.primary)
        else
            it.copy(
                titleContentColor = MaterialTheme.colorScheme.primary,
                scrolledContainerColor = it.scrolledContainerColor.copy(
                    alpha = it.containerColor.alpha
                )
            )
    }

    @Composable
    fun NavIconContent() {
        if (!PlatformStuff.isTv) {
            AnimatedVisibility(
                visible = showBack,
                enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
                exit = shrinkOut(shrinkTowards = Alignment.Center) + fadeOut(),
            ) {
                IconButton(
                    onClick = onBack,
                    enabled = showBack,
                    shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Default)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.ArrowBack,
                        contentDescription = stringResource(Res.string.back)
                    )
                }
            }
        }
    }

    @Composable
    fun TitleText(
        text: String,
        modifier: Modifier = Modifier,
        style: TextStyle = LocalTextStyle.current
    ) {
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = style,
            modifier = modifier
        )
    }

    @Composable
    fun RowScope.ActionsContent() {
        if (windowTitleActions != null) {
            // show manual minimize, maximize, close buttons

            IconButton(
                shapes = IconButtonDefaults.shapes(),
                onClick = windowTitleActions::minimize,
                modifier = Modifier.pointerHoverIcon(PointerIcon.Default)
            ) {
                Icon(
                    imageVector = Icons.Minimize,
                    contentDescription = stringResource(Res.string.minimize)
                )
            }

            IconButton(
                shapes = IconButtonDefaults.shapes(),
                onClick = windowTitleActions::maximizeRestore,
                modifier = Modifier.pointerHoverIcon(PointerIcon.Default)
            ) {
                Icon(
                    imageVector = Icons.Fullscreen,
                    contentDescription = stringResource(Res.string.expand)
                )
            }

            IconButton(
                shapes = IconButtonDefaults.shapes(),
                onClick = windowTitleActions::close,
                modifier = Modifier.pointerHoverIcon(PointerIcon.Default)
            ) {
                Icon(
                    imageVector = Icons.Close,
                    contentDescription = stringResource(Res.string.close)
                )
            }
        }
    }

    val outerTextStyle = LocalTextStyle.current

    CenterAlignedTopAppBar(
        modifier = modifier,
        title = {
            val titleContent = when {
                searchTextFieldState != null -> TitleContent.Search(searchTextFieldState, title)
                subTabs != null -> TitleContent.SubTabs(subTabs.subTabs)
                else -> TitleContent.Text(title)
            }

            AnimatedContent(
                titleContent,
                contentAlignment = Alignment.Center,
                label = "TitleContent",
                transitionSpec = {
                    // same default but with clip = false
                    ContentTransform(
                        fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                                scaleIn(
                                    initialScale = 0.92f,
                                    animationSpec = tween(220, delayMillis = 90)
                                ),
                        fadeOut(animationSpec = tween(90)),
                        sizeTransform = SizeTransform(clip = false)
                    )
                },
                contentKey = { it::class },
                modifier = Modifier
                    .padding(
                        top = if (PlatformStuff.isTv) 8.dp else 0.dp, // for TV overscan
                        start = innerPaddingStart
                    )
            ) { titleContent ->
                when (titleContent) {
                    is TitleContent.Search -> {
                        TextField(
                            state = titleContent.searchState,
                            lineLimits = TextFieldLineLimits.SingleLine,
                            shape = CircleShape,
                            textStyle = outerTextStyle,
                            colors = TextFieldDefaults.tonalColors().copy(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                            placeholder = {
                                TitleText(
                                    text = titleContent.placeholder,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = colors.titleContentColor,
                                        textAlign = TextAlign.Center,
                                    ),
                                    modifier = Modifier
                                        .clearAndSetSemantics {}
                                        .fillMaxWidth(),
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Search,
                                    contentDescription = null,
                                    tint = if (titleContent.searchState.text.isEmpty())
                                        LocalContentColor.current
                                    else
                                        MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = if (!PlatformStuff.isTv) {
                                {
                                    if (titleContent.searchState.text.isNotEmpty()) {
                                        IconButton(
                                            shapes = IconButtonDefaults.shapes(),
                                            onClick = {
                                                titleContent.searchState.clearText()
                                            }
                                        ) {
                                            Icon(
                                                Icons.Close,
                                                contentDescription = stringResource(Res.string.delete)
                                            )
                                        }
                                    }
                                }
                            } else
                                null,
                            keyboardOptions = KeyboardOptions.Default.copy(
                                imeAction = ImeAction.Search
                            ),
                            modifier = Modifier
                                .focusRequester(searchFieldFocusRequester)
                                .padding(horizontal = horizontalOverscanPadding())
                                .widthIn(max = 720.dp)
                                .fillMaxWidth()
                        )
                    }

                    is TitleContent.SubTabs -> {
                        val colors = ToggleButtonDefaults.outlinedToggleButtonColors(
                            checkedContainerColor = MaterialTheme.colorScheme.secondary,
                        )

                        OutlinedToggleButtons(
                            texts = titleContent.subTabsList.map { subtab ->
                                if (subtab.id == selectedSubTabId)
                                    title
                                else
                                    stringResource(subtab.titleRes)
                            },
                            icons = titleContent.subTabsList.map { it.icon },
                            selectedIndex = titleContent.subTabsList.indexOfFirst { it.id == selectedSubTabId },
                            onSelected = { index ->
                                val subtab = titleContent.subTabsList.getOrNull(index)
                                if (subtab != null) {
                                    resultEventBus.sendResult(SubTabClickedResult(subtab.id))
                                }
                            },
                            mode = PanoToggleButtonsMode.Icon,
                            colors = colors,
                            horizontalArrangement = Arrangement.spacedBy(
                                2.dp,
                                Alignment.CenterHorizontally
                            ),
                            canReClick = true,
                        )
                    }

                    is TitleContent.Text -> {
                        TitleText(
                            text = titleContent.text
                        )
                    }
                }
            }
        },
        navigationIcon = {
            NavIconContent()
        },
        actions = { ActionsContent() },
        colors = colors,
        scrollBehavior = scrollBehavior
    )
}

@Composable
private fun PanoNavigationRail(
    tabs: List<PanoTab>,
    selectedTabIdx: Int,
    fabData: PanoFabData?,
    resultEventBus: ResultEventBus,
    onNavigate: (PanoRoute) -> Unit,
    onTabClicked: (pos: Int) -> Unit,
    onProfileClicked: () -> Unit,
    user: UserCached?,
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
                        resultEventBus = resultEventBus,
                        onNavigate = onNavigate,
                        modifier = Modifier
                            .padding(start = horizontalOverscanPadding() / 2)
                            .padding(16.dp)
                    )
                }
            } else
                null,
        arrangement = Arrangement.aligned(Alignment.CenterVertically),
        modifier = modifier
            .widthIn(max = 200.dp)
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

                if (user != null) {
                    WideNavigationRailItem(
                        selected = index == selectedTabIdx,
                        onClick = {
                            if (tabMetadata is PanoTab.Profile) {
                                onProfileClicked()
                            } else if (index != selectedTabIdx) {
                                onTabClicked(index)
                            }
                        },
                        railExpanded = state.targetValue == WideNavigationRailValue.Expanded,
                        icon = {
                            if (tabMetadata is PanoTab.Profile) {
                                AvatarOrInitials(
                                    avatarUrl = user.largeImage,
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
                                overflow = TextOverflow.MiddleEllipsis,
                            )
                        },
                        colors = if (state.targetValue == WideNavigationRailValue.Expanded)
                            WideNavigationRailItemDefaults.colors(
                                selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        else
                            WideNavigationRailItemDefaults.colors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
) {
    ShortNavigationBar {
        tabs.forEachIndexed { index, tabMetadata ->

            ShortNavigationBarItem(
                selected = index == selectedTabIdx,
                onClick = {
                    if (tabMetadata is PanoTab.Profile) {
                        onProfileClicked()
                    } else if (index != selectedTabIdx) {
                        onTabClicked(index)
                    }
                },
                icon = {
                    if (tabMetadata is PanoTab.Profile) {

                        AvatarOrInitials(
                            avatarUrl = user.largeImage,
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

@Composable
private fun PanoBottomAppBar(
    tabs: List<PanoTab>,
    selectedTabIdx: Int,
    onTabClicked: (pos: Int) -> Unit,
    onProfileClicked: () -> Unit,
    user: UserCached,
) {
    val profileTab = tabs.find { it is PanoTab.Profile }
    val otherTabs = tabs.filter { it !is PanoTab.Profile }

    val colors = FloatingToolbarDefaults.standardFloatingToolbarColors().let {
        if (it.toolbarContainerColor.alpha == 1f)
            it
        else
            it.copy(
                toolbarContainerColor = it.toolbarContainerColor.copy(
                    alpha = 1f
                )
            )
    }

    val modifier = Modifier
        .fillMaxWidth()
        .wrapContentWidth()
        .pointerInput(Unit) {
            // Swallow any tap that lands in the inset area and isn't already consumed by a child
            awaitEachGesture {
                val down = awaitFirstDown(pass = PointerEventPass.Final)
                if (!down.isConsumed) down.consume()
            }
        }
        .windowInsetsPadding(ShortNavigationBarDefaults.windowInsets)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        Surface(
            color = colors.toolbarContainerColor,
            contentColor = colors.toolbarContentColor,
            shape = FloatingToolbarDefaults.ContainerShape,
            shadowElevation = FloatingToolbarDefaults.ContainerExpandedElevationWithFab,
        ) {
            OutlinedToggleButtons(
                texts = otherTabs.map { stringResource(it.titleRes) },
                icons = otherTabs.map { it.icon },
                selectedIndex = selectedTabIdx,
                horizontalArrangement = Arrangement.Start,
                onSelected = onTabClicked,
                textStyle = MaterialTheme.typography.labelSmall,
                border = false,
                mode = PanoToggleButtonsMode.BothVertical,
                colors = ToggleButtonDefaults.outlinedToggleButtonColors(
                    checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            )
        }

        if (profileTab != null) {
            SmallFloatingActionButton(
                onClick = onProfileClicked,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.inversePrimary,
                elevation = FloatingActionButtonDefaults.loweredElevation()
            ) {
                AvatarOrInitials(
                    avatarUrl = user.largeImage,
                    avatarName = user.name,
                    modifier = Modifier
                        .padding(4.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                )
            }
        }
    }
}

@Composable
private fun <T : Any> rememberPanoResultEventBusNavEntryDecorator(resultEventBus: ResultEventBus): ResultEventBusNavEntryDecorator<T> =
    remember {
        ResultEventBusNavEntryDecorator(resultEventBus)
    }

private sealed interface TitleContent {
    data class Text(val text: String) : TitleContent
    data class Search(val searchState: TextFieldState, val placeholder: String) : TitleContent
    data class SubTabs(val subTabsList: List<Subtab>) : TitleContent
}
