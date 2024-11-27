package com.arn.scrobble.main

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.navigation.panoDialogNavGraph


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanoMainDialogContent(
    navController: NavHostController,
    onFinish: () -> Unit,
    viewModel: MainViewModel = viewModel(),
) {
    NavHost(
        navController = navController,
        startDestination = PanoRoute.BlankScreen,
        modifier = Modifier
    ) {
        panoDialogNavGraph(
            navigate = navController::navigate,
            usingInDialogActivity = true,
            goUp = {
                if (navController.previousBackStackEntry?.destination?.hasRoute<PanoRoute.BlankScreen>() == true) {
                    onFinish()
                } else {
                    navController.navigateUp()
                }
            },
            mainViewModel = viewModel,
        )
    }
}