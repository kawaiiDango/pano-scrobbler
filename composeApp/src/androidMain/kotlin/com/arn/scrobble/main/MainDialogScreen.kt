package com.arn.scrobble.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import co.touchlab.kermit.Logger
import coil3.compose.setSingletonImageLoaderFactory
import com.arn.scrobble.imageloader.newImageLoader
import com.arn.scrobble.navigation.BottomSheetSceneStrategy
import com.arn.scrobble.navigation.DeepLinkUtils
import com.arn.scrobble.navigation.NavFromOutsideEffect
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.navigation.panoModalNavGraph
import com.arn.scrobble.navigation.rememberPanoNavBackStack
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic


@Composable
fun PanoMainDialogContent(
    onClose: () -> Unit,
    viewModel: MainViewModel = viewModel { MainViewModel() },
) {
    setSingletonImageLoaderFactory { context ->
        newImageLoader(context)
    }

    val backStack = rememberPanoNavBackStack(
        SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(PanoRoute::class)
            }

        },
        PanoRoute.Blank,
    )
    val mainViewModelStoreOwner = LocalViewModelStoreOwner.current

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
        backStack.removeAll { it is PanoRoute.Modal }
    }

    fun replace(route: PanoRoute) {
        backStack.add(route)
        backStack.removeAll {
            it != route && it != PanoRoute.Blank
        }
    }

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
        sceneStrategy = remember { BottomSheetSceneStrategy(::removeAllModals) },
        entryProvider = entryProvider {
            entry(PanoRoute.Blank) { }

            panoModalNavGraph(
                navigate = {
                    when (it) {
                        !is PanoRoute.Modal if it is PanoRoute.DeepLinkable ->
                            DeepLinkUtils.handleNavigationFromInfoScreen(it)

                        is PanoRoute.Modal -> navigate(it)

                        else -> Logger.e(
                            "Cannot navigate to non-deeplinkable route",
                            IllegalArgumentException("Called ${it::class.simpleName} from dialog")
                        )
                    }
                },
                goBack = ::goBack,
                mainViewModel = viewModel,
                mainViewModelStoreOwner = { mainViewModelStoreOwner!! },
            )
        }
    )
}