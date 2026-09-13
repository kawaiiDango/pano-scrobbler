package com.arn.scrobble.main

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.contentColorFor
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultShadowColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.result.ResultEffect
import androidx.navigation3.runtime.result.ResultEventBus
import androidx.navigation3.runtime.result.ResultEventBusNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import coil3.compose.setSingletonImageLoaderFactory
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.api.DrawerData
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.charts.TimePeriodSelectorRow
import com.arn.scrobble.icons.ArrowBackAutoMirrored
import com.arn.scrobble.icons.Close
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.Minimize
import com.arn.scrobble.icons.Search
import com.arn.scrobble.imageloader.PanoImageLoader
import com.arn.scrobble.navigation.BottomSheetDialogParent
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
import com.arn.scrobble.navigation.ProfileDialogContent
import com.arn.scrobble.navigation.ProfilePopup
import com.arn.scrobble.navigation.PullToRefreshResult
import com.arn.scrobble.navigation.SubTabClickedResult
import com.arn.scrobble.navigation.TimePeriodDataResult
import com.arn.scrobble.navigation.rememberPanoNavBackStack
import com.arn.scrobble.themes.LocalThemeAttributes
import com.arn.scrobble.ui.AvatarOrInitials
import com.arn.scrobble.ui.LocalInnerPadding
import com.arn.scrobble.ui.LocalModalShownTracker
import com.arn.scrobble.ui.LocalNavDestBackground
import com.arn.scrobble.ui.PanoPullToRefreshStateForTab
import com.arn.scrobble.ui.PanoSnackbarVisuals
import com.arn.scrobble.ui.PanoToggleButtonGroup
import com.arn.scrobble.ui.PanoToggleButtonsMode
import com.arn.scrobble.ui.horizontalOverscanPadding
import com.arn.scrobble.ui.makeOpaque
import com.arn.scrobble.ui.verticalOverscanPadding
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
import pano_scrobbler.composeapp.generated.resources.minimize
import pano_scrobbler.composeapp.generated.resources.reload
import pano_scrobbler.composeapp.generated.resources.update_available
import pano_scrobbler.composeapp.generated.resources.update_downloaded
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanoAppContent(
    draggableWrapper: (@Composable (content: @Composable (windowTitleActions: WindowTitleActions) -> Unit) -> Unit)? = null,
    onCloseLastDialog: (() -> Unit)? = null,
    fallbackNavigationType: PanoNavigationType? = null,
    viewModel: MainViewModel = viewModel { MainViewModel() },
) {
    val isDialogActivity = onCloseLastDialog != null
    val sizeClass = currentWindowAdaptiveInfoV2().windowSizeClass

    val navigationType = when {
        sizeClass.minWidthDp == 0 && sizeClass.minHeightDp == 0 && fallbackNavigationType != null ->
            fallbackNavigationType

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
    val modalShownCount = rememberSaveable { mutableIntStateOf(0) }

//    val topBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(
//        state = rememberTopAppBarState(
//            initialHeightOffsetLimit = with(density) { -32.dp.toPx() }
//        )
//    )
    val pullToRefreshState = rememberPullToRefreshState()
    val pullToRefreshStateForHomePager =
        remember { mutableStateMapOf<PanoTab, PanoPullToRefreshStateForTab>() }

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
            isDialogActivity -> PanoRoute.Blank
            userSelf == null -> PanoRoute.Onboarding
            else -> PanoRoute.SelfHomePager()
        }
    )

    val currentPanoRoute = remember(backStack.lastOrNull()) {
        backStack.lastOrNull { !it.isModal() }
    }

    val currentUser = remember(currentPanoRoute, currentAccountType) {
        backStack
            .filterIsInstance<PanoRoute.HasUser>()
            .lastOrNull()
            ?.user
            ?: userSelf
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

    val hasTimePeriods = remember(currentPanoRoute, tabIdxMap[currentPanoRoute]) {
        tabData?.getOrNull(
            tabIdxMap.getOrDefault(currentPanoRoute, -1)
        ) is PanoRoute.HasTimePeriods ||
                currentPanoRoute is PanoRoute.HasTimePeriods
    }

    var profilePopupShown by rememberSaveable { mutableStateOf(false) }
    val drawerDataMap = viewModel.drawerDataMap

    val transparentScaffold = isDialogActivity

    var lastTimePeriodDataResult by remember { mutableStateOf<TimePeriodDataResult?>(null) }
    var selectedSubTabId by remember { mutableIntStateOf(-1) }
    val title = titlesMap[currentPanoRoute].orEmpty()
    val subtitle = if (
        currentPanoRoute is PanoRoute.HasUser &&
        navigationType == PanoNavigationType.BOTTOM_NAVIGATION &&
        tabData == null &&
        currentUser?.isSelf == false
    )
        currentUser.name
    else
        null
    val useAltBottomBar = false // todo testing only

    fun goBack(): PanoRoute? {
        if (backStack.size <= 1)
            return null

        val route = backStack.lastOrNull()

        if (backStack.count { it.isModal() } == 1 && route?.isModal() == true) {
            scope.launch {
                bottomSheetState.hide()
                backStack.remove(route)
            }
        } else {
            backStack.remove(route)
        }

        if (route is PanoRoute.HasTabs)
            tabIdxMap.remove(route)

        if (route != null)
            titlesMap.remove(route)

        return route
    }

    fun removeAllModals() {
        backStack.removeAll { it.isModal() }
    }

    fun navigate(route: PanoRoute) {
        fun add() {
            val last = backStack.lastOrNull()
            if (last != route) {
                // expand the whole chain
                if (last is PanoRoute.Modal.CanExpand && last.isExpanded && route is PanoRoute.Modal.CanExpand)
                    backStack.add(route.copyExpanded())
                else
                    backStack.add(route)
            }
        }


        if (!route.isModal() && backStack.any { it.isModal() }) {
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

        val oldSize = backStack.size
        backStack.addAll(syntheticBackStack)

        var itemsRemoved = 0
        backStack.removeAll {
            itemsRemoved++ < oldSize
        }
    }

    fun expandModal(route: PanoRoute.Modal.CanExpand) {
        if (route.isExpanded) return

        // expand the whole chain
        var routeFound = false
        for (i in backStack.indices.reversed()) {
            val item = backStack[i]
            if (item == route) {
                routeFound = true
            } else if (!routeFound) {
                continue
            }

            if (item is PanoRoute.Modal.CanExpand && !item.isExpanded) {
                backStack[i] = item.copyExpanded()
            } else
                break
        }

        scope.launch { bottomSheetState.hide() }
    }

    val bottomSheetStrategy = remember {
        BottomSheetSceneStrategy<PanoRoute>(
            sheetState = bottomSheetState,
            onDismiss = ::removeAllModals
        )
    }
    val singlePaneStrategy = remember { SinglePaneSceneStrategy<PanoRoute>() }

    LaunchedEffect(currentPanoRoute) {
        if (currentPanoRoute is PanoRoute.HasSearch) {
            if (currentPanoRoute is PanoRoute.SearchRequestsFocus)
                searchFieldFocusRequester.requestFocus()
            else
                searchFieldFocusRequester.freeFocus()
        } else {
            searchFieldState.clearText()
        }
        if (!hasTimePeriods) {
            lastTimePeriodDataResult = null
        }
    }

    if (onCloseLastDialog != null) {
        LaunchedEffect(backStack.lastOrNull()) {
            val route = backStack.lastOrNull()
            if (route == null || route == PanoRoute.Blank) {
                onCloseLastDialog()
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

    LaunchedEffect(profilePopupShown) {
        if (profilePopupShown && currentUser != null) {
            viewModel.loadDrawerData(currentUser)
        }
    }

    NavFromOutsideEffect(
        onNavigate = {
            // disable deeplinks if logged out
            if (currentUser != null)
                navigate(it)
        },
        isAndroidDialogActivity = isDialogActivity
    )

    ResultEffect<TimePeriodDataResult>(resultEventBus) { res ->
        lastTimePeriodDataResult = res
    }

    setSingletonImageLoaderFactory { context ->
        PanoImageLoader.newImageLoader(context)
    }

    key(locale) {
        val needsRoundedCorners = draggableWrapper != null

        val needsBackdropBlur = PlatformStuff.isDesktop &&
                PlatformStuff.supportsBlur &&
                LocalThemeAttributes.current.blurSubWindow &&
                modalShownCount.intValue > 0

        CompositionLocalProvider(
            LocalNavigationType provides navigationType,
            LocalModalShownTracker provides modalShownCount,
        ) {
            Scaffold(
                containerColor = if (transparentScaffold)
                    Color.Transparent
                else
                    MaterialTheme.colorScheme.background,
                contentColor = contentColorFor(MaterialTheme.colorScheme.background),
                modifier = Modifier
                    .fillMaxSize()
                    .pullToRefresh(
                        state = pullToRefreshState,
                        isRefreshing = pullToRefreshStateForHomePager.values.any { it == PanoPullToRefreshStateForTab.Refreshing },
                        enabled = (!PlatformStuff.isDesktop && !PlatformStuff.isTv) && !pullToRefreshStateForHomePager.values.all { it == PanoPullToRefreshStateForTab.Disabled },
                        onRefresh = {
                            // find the right tab
                            pullToRefreshStateForHomePager.entries
                                .find { it.value == PanoPullToRefreshStateForTab.NotRefreshing }
                                ?.key
                                ?.let {
                                    resultEventBus.sendResult(PullToRefreshResult(it))
                                }
                        }
                    )
//                    .nestedScroll(topBarScrollBehavior.nestedScrollConnection)
                    .then(
                        if (needsRoundedCorners) Modifier.clip(MaterialTheme.shapes.medium) else Modifier
                    ).then(
                        if (needsBackdropBlur) Modifier.blur(radius = Stuff.BLUR_BACKDROP_RADIUS_DP.dp) else Modifier
                    ),
                topBar = {
                    @Composable
                    fun topAppBar(windowTitleActions: WindowTitleActions?) {
                        PanoTopAppBar(
                            titleContent = when {
                                currentPanoRoute is PanoRoute.HasSearch -> TitleContent.Search(
                                    searchFieldState,
                                    searchFieldFocusRequester,
                                    title
                                )

                                subTabData != null -> TitleContent.SubTabs(
                                    subTabData.subTabs,
                                    selectedSubTabId,
                                    title
                                )

                                hasTimePeriods && lastTimePeriodDataResult != null ->
                                    TitleContent.TimePeriods(lastTimePeriodDataResult!!)

                                else -> TitleContent.Text(title)
                            },
                            subtitle = subtitle,
                            //                            scrollBehavior = topBarScrollBehavior,
                            scrollBehavior = null,
                            innerPaddingStart = if (navigationType != PanoNavigationType.BOTTOM_NAVIGATION) navRailWidth else 0.dp,
                            windowTitleActions = windowTitleActions,
                            containerColor = if (transparentScaffold && currentPanoRoute !is PanoRoute.Blank) Color.Unspecified else Color.Transparent,
                            showBack = !PlatformStuff.isTv && backStack.count { !it.isModal() } > 1 && currentPanoRoute !is PanoRoute.Blank,
                            resultEventBus = resultEventBus,
                            onBack = ::goBack,
                        )
                    }

                    if (draggableWrapper != null)
                        draggableWrapper { topAppBar(it) }
                    else
                        topAppBar(null)
                },
                bottomBar = {
                    val showBottomBar =
                        navigationType == PanoNavigationType.BOTTOM_NAVIGATION && tabData != null && currentUser != null

                    AnimatedVisibility(
                        visible = showBottomBar,
                        enter = fadeIn() + expandVertically(expandFrom = Alignment.Top) + slideInVertically { it / 2 },
                        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top) + slideOutVertically { it / 2 },
                    ) {
                        // to show at least something in the animation
                        var capturedTabData by remember { mutableStateOf(tabData.orEmpty()) }

                        LaunchedEffect(tabData) {
                            if (tabData != null)
                                capturedTabData = tabData
                        }

                        val user = currentUser?.takeIf {
                            currentPanoRoute is PanoRoute.SelfHomePager || !it.isSelf
                        }

                        if (useAltBottomBar) {
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
                                user = user,
                                onProfileClicked = {
                                    profilePopupShown = true
                                },
                            )
                        } else {
                            PanoBottomNavigationBar(
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
                                onProfileClicked = {
                                    profilePopupShown = true
                                },
                                user = user,
                            )
                        }
                    }
                },
                floatingActionButton = {
                    if (navigationType == PanoNavigationType.BOTTOM_NAVIGATION) {
                        fabData?.let { fabData ->
                            PanoFab(
                                fabData,
                                resultEventBus,
                                onNavigate = ::navigate,
                                modifier = Modifier.windowInsetsPadding(
                                    WindowInsets.ime.exclude(WindowInsets.navigationBars)
                                )
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
                CompositionLocalProvider(
                    LocalInnerPadding provides innerPadding,
                    LocalNavDestBackground provides MaterialTheme.colorScheme.background
                        .let { if (transparentScaffold || it.alpha == 1f) it else Color.Transparent }
                ) {
                    val topPadding =
                        PaddingValues(top = innerPadding.calculateTopPadding())

                    val navContentFocusRequester = remember { FocusRequester() }

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (PlatformStuff.isTv) {
                                    Modifier.focusProperties {
                                        onEnter = {
                                            navContentFocusRequester.requestFocus()
                                        }
                                    }
                                } else Modifier
                            )
                    ) {
                        AnimatedVisibility(navigationType != PanoNavigationType.BOTTOM_NAVIGATION) {
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
                                onProfilePopupShown = {
                                    if (currentUser != null) {
                                        profilePopupShown = true
                                    }
                                },
                                profilePopupSlot = {
                                    if (currentUser != null && profilePopupShown) {
                                        ProfilePopup(
                                            shown = true,
                                            onDismiss = { profilePopupShown = false },
                                            user = currentUser,
                                            drawerData = drawerDataMap.getOrElse(currentUser) {
                                                DrawerData(0)
                                            },
                                            onNavigate = ::navigate
                                        )
                                    }
                                },
                                user = if (!transparentScaffold) currentUser else null,
                                containerColor = if (transparentScaffold && currentPanoRoute !is PanoRoute.Blank) Color.Unspecified else Color.Transparent,
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

                        fun <T : Any> AnimatedContentTransitionScope<Scene<T>>.navTransition(towards: AnimatedContentTransitionScope.SlideDirection):
                                ContentTransform {
                            val enterOffsetDenominator = 4
                            val exitOffsetDenominator = 4
                            val scaleFactor = 0.9f
                            val enterStiffness = Spring.StiffnessMediumLow
                            val exitStiffness = Spring.StiffnessHigh

                            return ContentTransform(
                                targetContentEnter = fadeIn(
                                    animationSpec = spring(
                                        stiffness = enterStiffness
                                    )
                                ) + scaleIn(
                                    initialScale = scaleFactor,
                                    animationSpec = spring(
                                        stiffness = enterStiffness
                                    )
                                ) + slideIntoContainer(
                                    towards = towards,
                                    initialOffset = { it / enterOffsetDenominator }
                                ),
                                initialContentExit = fadeOut(
                                    animationSpec = spring(
                                        stiffness = exitStiffness
                                    )
                                ) + scaleOut(
                                    targetScale = scaleFactor,
                                    animationSpec = spring(
                                        stiffness = exitStiffness
                                    )
                                ) + slideOutOfContainer(
                                    towards = towards,
                                    targetOffset = { it / exitOffsetDenominator }
                                ),
                            )
                        }

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
                                navTransition(AnimatedContentTransitionScope.SlideDirection.Start)
                            },
                            popTransitionSpec = {
                                navTransition(AnimatedContentTransitionScope.SlideDirection.End)

                            },
                            predictivePopTransitionSpec = { edge ->
//                                val towards = when (edge) {
//                                    NavigationEvent.EDGE_LEFT -> AnimatedContentTransitionScope.SlideDirection.Right
//                                    NavigationEvent.EDGE_RIGHT -> AnimatedContentTransitionScope.SlideDirection.Left
//                                    else -> AnimatedContentTransitionScope.SlideDirection.End
//                                }

                                navTransition(AnimatedContentTransitionScope.SlideDirection.End)
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
                                onExpandModal = ::expandModal,
                                onSetOnboardingFinished = {
                                    replaceRoutes(listOf(PanoRoute.SelfHomePager()))
                                },
                                pullToRefreshState = { pullToRefreshState },
                                onSetRefreshing = { tab, prState ->
                                    pullToRefreshStateForHomePager[tab] = prState
                                },
                                selectSubTabId = {
                                    selectedSubTabId = it
                                },
                                searchFieldState = searchFieldState,
                                mainViewModel = viewModel,
                            ),
                            modifier = Modifier
                                .widthIn(max = 960.dp)
                                .padding(topPadding)
                                .consumeWindowInsets(topPadding)
                                .focusRequester(navContentFocusRequester)
                                .focusGroup(),
                        )


                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                        )
                    }

                    if (profilePopupShown && currentUser != null && navigationType == PanoNavigationType.BOTTOM_NAVIGATION) {
                        BottomSheetDialogParent(
                            sheetState = rememberBottomSheetState(
                                initialValue = SheetValue.Hidden,
                                enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
                            ),
                            onDismissRequest = { profilePopupShown = false },
                            onBack = null,
                        ) {
                            ProfileDialogContent(
                                onDismiss = { profilePopupShown = false },
                                user = currentUser,
                                drawerData = drawerDataMap.getOrElse(currentUser) { DrawerData(0) },
                                onNavigate = ::navigate,
                                modifier = Modifier
                                    .safeContentPadding()
                            )
                        }
                    }
                }
            }

            if (BuildKonfig.DEBUG && PlatformStuff.isTv) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            vertical = verticalOverscanPadding(),
                            horizontal = horizontalOverscanPadding()
                        )
                        .border(1.dp, MaterialTheme.colorScheme.error)
                )
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
    titleContent: TitleContent,
    subtitle: String?,
    windowTitleActions: WindowTitleActions?,
    showBack: Boolean,
    onBack: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior?,
    innerPaddingStart: Dp,
    resultEventBus: ResultEventBus,
    containerColor: Color
) {
    val colors = TopAppBarDefaults.topAppBarColors(
        titleContentColor = MaterialTheme.colorScheme.primary,
        containerColor = containerColor
    )

    val startPadding = innerPaddingStart.let {
        var padding = it
        if (windowTitleActions != null)
            padding += 24.dp
        else if (showBack)
            padding -= IconButtonDefaults.smallContainerSize().width + (2 * 4).dp // TopAppBarHorizontalPadding is 4.dp
        padding.coerceAtLeast(0.dp)
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
                        imageVector = Icons.ArrowBackAutoMirrored,
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
                    contentDescription = stringResource(Res.string.minimize),
                )
            }

//            IconButton(
//                shapes = IconButtonDefaults.shapes(),
//                onClick = windowTitleActions::maximizeRestore,
//                modifier = Modifier.pointerHoverIcon(PointerIcon.Default)
//            ) {
//                Icon(
//                    imageVector = Icons.Fullscreen,
//                    contentDescription = stringResource(Res.string.expand)
//                )
//            }

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

    TopAppBar(
        contentPadding = TopAppBarDefaults.ContentPadding + PaddingValues(
            top = (verticalOverscanPadding() - TopAppBarDefaults.ContentPadding.calculateTopPadding())
                .coerceAtLeast(0.dp),
            end = (horizontalOverscanPadding() -
                    TopAppBarDefaults.ContentPadding.calculateEndPadding(LocalLayoutDirection.current)
                    ).coerceAtLeast(0.dp),
        ),
        titleHorizontalAlignment = Alignment.CenterHorizontally,
        subtitle = {
            // does not reserve space if null
            if (subtitle != null) {
                Text(
                    subtitle,
                    modifier = Modifier
                        .padding(start = startPadding)
                )
            }
        },
        title = {
            // AnimatedContent has issues due to TopAppBar's own animations
            val alpha = remember(titleContent::class) { Animatable(0f) }

            LaunchedEffect(titleContent::class) {
                alpha.animateTo(1f, tween())
            }

            Box(
                modifier = Modifier
                    .padding(start = startPadding)
                    .graphicsLayer { this.alpha = alpha.value }
            ) {
                when (titleContent) {
                    is TitleContent.Search -> {
                        var isEditable by rememberSaveable { mutableStateOf(!PlatformStuff.isTv) }
                        val ime = LocalSoftwareKeyboardController.current

                        TextField(
                            state = titleContent.searchState,
                            lineLimits = TextFieldLineLimits.SingleLine,
                            shape = CircleShape,
                            readOnly = !isEditable,
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
                                imeAction = ImeAction.Done,
                            ),
                            onKeyboardAction = {
                                ime?.hide()
                            },
                            contentPadding = TextFieldDefaults.contentPaddingWithLabel(), // has lower vertical padding
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .widthIn(max = 720.dp)
                                .fillMaxWidth()
                                .focusRequester(titleContent.focusRequester)
                                .then(
                                    if (PlatformStuff.isTv) {
                                        Modifier
                                            .onFocusChanged {
                                                if (!it.isFocused)
                                                    isEditable = false
                                            }
                                            .onPreviewKeyEvent { e ->
                                                when {
                                                    e.type != KeyEventType.KeyUp -> false
                                                    !isEditable && (e.key == Key.DirectionCenter || e.key == Key.Enter) -> {
                                                        isEditable = true
                                                        true
                                                    }

                                                    isEditable && (e.key == Key.Back || e.key == Key.Escape) -> {
                                                        isEditable = false
//                                                        searchFieldFocusRequester.freeFocus()
                                                        true
                                                    }

                                                    else -> false
                                                }
                                            }
                                    } else
                                        Modifier
                                )
                        )
                    }

                    is TitleContent.SubTabs -> {
                        PanoToggleButtonGroup(
                            texts = titleContent.subTabs.map { subtab ->
                                if (subtab.id == titleContent.selectedId)
                                    titleContent.title
                                else
                                    stringResource(subtab.titleRes)
                            },
                            icons = titleContent.subTabs.map { it.icon },
                            chevronAt = titleContent.subTabs.indexOfFirst { it.id == titleContent.selectedId && it.isDropdown },
                            selectedIndex = titleContent.subTabs.indexOfFirst { it.id == titleContent.selectedId },
                            onSelected = { index ->
                                val subtab = titleContent.subTabs.getOrNull(index)
                                if (subtab != null) {
                                    resultEventBus.sendResult(SubTabClickedResult(subtab.id))
                                }
                            },
                            mode = PanoToggleButtonsMode.Icon,
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Default)
                        )
                    }

                    is TitleContent.TimePeriods -> {
                        TimePeriodSelectorRow(
                            typeSelectorShown = titleContent.data.typeSelectorShown,
                            periodType = titleContent.data.periodType,
                            selectedPeriod = titleContent.data.selectedPeriod,
                            timePeriodsList = titleContent.data.timePeriodsList,
                            enabled = titleContent.data.enabled,
                            resultEventBus = resultEventBus,
                            modifier = Modifier
                                .pointerHoverIcon(PointerIcon.Default)
                        )
                    }

                    is TitleContent.Text -> {
                        TitleText(
                            text = titleContent.title
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
    onProfilePopupShown: () -> Unit,
    profilePopupSlot: @Composable () -> Unit,
    user: UserCached?,
    containerColor: Color,
    modifier: Modifier = Modifier,
) {
    val hasProfileButton = user != null

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

    val colors = WideNavigationRailDefaults.colors(
        containerColor = containerColor
    )

    val centerWithPinnedFooter = object : Arrangement.Vertical {
        override fun Density.arrange(totalSize: Int, sizes: IntArray, outPositions: IntArray) {
            if (sizes.isEmpty()) return

            if (hasProfileButton) {
                val footerIndex = sizes.lastIndex
                val footerSize = sizes[footerIndex]

                // Sum the height of everything except the footer.
                var itemsHeight = 0
                for (i in 0 until footerIndex) {
                    itemsHeight += sizes[i]
                }

                // Center that group within the space above the footer.
                var y = ((totalSize - footerSize - itemsHeight) / 2).coerceAtLeast(0)
                for (i in 0 until footerIndex) {
                    outPositions[i] = y
                    y += sizes[i]
                }

                // Pin the footer to the bottom, but never let it overlap the items above.
                outPositions[footerIndex] = (totalSize - footerSize).coerceAtLeast(y)
            } else {
                // No footer — just center everything as a group.
                var itemsHeight = 0
                for (size in sizes) itemsHeight += size

                var y = ((totalSize - itemsHeight) / 2).coerceAtLeast(0)
                for (i in sizes.indices) {
                    outPositions[i] = y
                    y += sizes[i]
                }
            }
        }
    }

    LaunchedEffect(expandedByDefault) {
        if (expandedByDefault) {
            state.expand()
        } else {
            state.collapse()
        }
    }

    WideNavigationRail(
        state = state,
        colors = colors,
        header =
            if (fabData != null && (fabData.showOnTv && PlatformStuff.isTv || !PlatformStuff.isTv)) {
                {
                    PanoFab(
                        fabData,
                        resultEventBus = resultEventBus,
                        onNavigate = onNavigate,
                        modifier = Modifier
                            .padding(start = (horizontalOverscanPadding() / 2).coerceAtLeast(16.dp))
                            .padding(16.dp)
                    )
                }
            } else
                null,
        arrangement = centerWithPinnedFooter,
        contentPadding = PaddingValues(
            // below top bar now
            bottom = 8.dp,
        ),
        modifier = modifier
            .widthIn(max = 220.dp)
    ) {

        tabs.forEachIndexed { index, tabMetadata ->
            WideNavigationRailItem(
                selected = index == selectedTabIdx,
                onClick = {
                    onTabClicked(index)
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
                        stringResource(tabMetadata.titleRes),
                        maxLines = 1,
                        overflow = TextOverflow.MiddleEllipsis,
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (hasProfileButton) {
            Row {
                WideNavigationRailItem(
                    selected = false,
                    onClick = {
                        onProfilePopupShown()
                    },
                    railExpanded = state.targetValue == WideNavigationRailValue.Expanded,
                    icon = {
                        AvatarOrInitials(
                            avatarUrl = user.largeImage,
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
                            overflow = TextOverflow.MiddleEllipsis,
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                profilePopupSlot()
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
    user: UserCached?,
) {
    val hasProfileButton = user != null

    val containerColor = ShortNavigationBarDefaults.containerColor.makeOpaque()

    ShortNavigationBar(
        containerColor = containerColor,
        windowInsets = ShortNavigationBarDefaults.windowInsets
            .exclude(WindowInsets(bottom = 8.dp)), // 64 - 8 = 56
    ) {
        tabs.forEachIndexed { index, tabMetadata ->
            ShortNavigationBarItem(
                selected = index == selectedTabIdx,
                onClick = {
                    if (index != selectedTabIdx) {
                        onTabClicked(index)
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .widthIn(max = 100.dp)
                    )
                }
            )
        }

        if (hasProfileButton) {
            ShortNavigationBarItem(
                selected = false,
                onClick = {
                    onProfileClicked()
                },
                icon = {
                    AvatarOrInitials(
                        avatarUrl = user.largeImage,
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
    user: UserCached?,
) {
    val hasProfileButton = user != null

    val colors = FloatingToolbarDefaults.standardFloatingToolbarColors().let {
        if (it.toolbarContainerColor.alpha == 1f)
            it
        else
            it.copy(
                toolbarContainerColor = it.toolbarContainerColor.makeOpaque()
            )
    }

    val shadowColor = if (LocalThemeAttributes.current.isDark)
        Color.Gray
    else
        DefaultShadowColor

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
        .windowInsetsPadding(
            ShortNavigationBarDefaults.windowInsets.union(
                WindowInsets(
                    left = 16.dp,
                    right = 16.dp,
                    bottom = 16.dp
                )
            )
        )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        Surface(
            color = colors.toolbarContainerColor,
            contentColor = colors.toolbarContentColor,
            shape = FloatingToolbarDefaults.ContainerShape,
            modifier = Modifier.shadow(
                elevation = FloatingToolbarDefaults.ContainerExpandedElevationWithFab,
                shape = FloatingToolbarDefaults.ContainerShape,
                clip = false,
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
        ) {
            PanoToggleButtonGroup(
                texts = tabs.map { stringResource(it.titleRes) },
                icons = tabs.map { it.icon },
                selectedIndex = selectedTabIdx,
                horizontalArrangement = Arrangement.Start,
                onSelected = onTabClicked,
                textStyle = MaterialTheme.typography.labelMedium,
                border = false,
                mode = PanoToggleButtonsMode.BothVertical,
                colors = ToggleButtonDefaults.colors(
                    checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            )
        }

        if (hasProfileButton) {
            val shape = CircleShape

            FilledTonalIconButton(
                onClick = onProfileClicked,
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier
                    .size(56.dp) // normal fab size
                    .shadow(
                        elevation = FloatingToolbarDefaults.ContainerExpandedElevationWithFab,
                        shape = FloatingToolbarDefaults.ContainerShape,
                        clip = false,
                        ambientColor = shadowColor,
                        spotColor = shadowColor
                    )
            ) {
                AvatarOrInitials(
                    avatarUrl = user.largeImage,
                    avatarName = user.name,
                    modifier = Modifier
                        .size(40.dp) // small fab size
                        .clip(shape)
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
    data class Text(
        val title: String,
    ) : TitleContent

    data class Search(
        val searchState: TextFieldState,
        val focusRequester: FocusRequester,
        val placeholder: String
    ) : TitleContent

    data class SubTabs(val subTabs: List<Subtab>, val selectedId: Int, val title: String) :
        TitleContent

    data class TimePeriods(val data: TimePeriodDataResult) : TitleContent
}
