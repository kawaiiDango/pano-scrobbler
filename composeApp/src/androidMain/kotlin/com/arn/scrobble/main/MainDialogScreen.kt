package com.arn.scrobble.main

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import co.touchlab.kermit.Logger
import coil3.compose.setSingletonImageLoaderFactory
import com.arn.scrobble.imageloader.PanoImageLoader
import com.arn.scrobble.navigation.BottomSheetSceneStrategy
import com.arn.scrobble.navigation.DeepLinkUtils
import com.arn.scrobble.navigation.NavFromOutsideEffect
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.navigation.panoModalNavGraph
import com.arn.scrobble.navigation.rememberPanoNavBackStack
import kotlinx.coroutines.launch
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanoMainDialogContent(
    onClose: () -> Unit,
    viewModel: MainViewModel = viewModel { MainViewModel() },
) {
    setSingletonImageLoaderFactory { context ->
        PanoImageLoader.newImageLoader(context)
    }

    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )

    val backStack = rememberPanoNavBackStack(
        SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(PanoRoute::class)
            }

        },
        PanoRoute.Blank,
    )

    LaunchedEffect(backStack.lastOrNull()) {
        val route = backStack.lastOrNull()
        if (route == null || route == PanoRoute.Blank) {
            onClose()
        }
    }

    fun goBack(): PanoRoute? {
        if (backStack.size > 1)
            return backStack.removeLastOrNull()
        return null
    }

    fun navigate(route: PanoRoute) {
        backStack.add(route)
    }

    fun removeAllModals() {
        scope.launch {
            bottomSheetState.hide()
            backStack.removeAll { it is PanoRoute.Modal }
        }
    }

    fun replace(route: PanoRoute) {
        backStack.add(route)
        backStack.removeAll {
            it != route && it != PanoRoute.Blank
        }
    }

    val bottomSheetStrategy =
        remember { BottomSheetSceneStrategy<PanoRoute>(bottomSheetState, ::removeAllModals) }

    NavFromOutsideEffect(
        onNavigate = ::replace,
        isAndroidDialogActivity = true
    )

    NavDisplay(
        backStack = backStack,
        onBack = ::goBack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        sceneStrategies = remember { listOf(bottomSheetStrategy) },
        entryProvider = entryProvider {
            entry(PanoRoute.Blank) { }

            panoModalNavGraph(
                navigate = {
                    when (it) {
                        !is PanoRoute.Modal if it is PanoRoute.DeepLinkable -> {
                            scope.launch {
                                removeAllModals()
                                DeepLinkUtils.handleNavigationFromInfoScreen(it)
                            }
                        }

                        is PanoRoute.Modal -> {
                            navigate(it)
                        }

                        else -> {
                            Logger.e(
                                "Cannot navigate to non-deeplinkable route",
                                IllegalArgumentException("Called ${it::class.simpleName} from dialog")
                            )
                        }
                    }
                },
                goBack = ::goBack,
                onSetDrawerData = {},
                mainViewModel = viewModel
            )
        }
    )
}