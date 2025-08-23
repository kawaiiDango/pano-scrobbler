package com.arn.scrobble.main

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.setSingletonImageLoaderFactory
import com.arn.scrobble.imageloader.newImageLoader
import com.arn.scrobble.navigation.DeepLinkUtils
import com.arn.scrobble.navigation.PanoDialog


@Composable
fun PanoMainDialogContent(
    dialogArgs: PanoDialog,
    onFinish: () -> Unit,
    viewModel: MainViewModel = viewModel { MainViewModel() },
) {
    setSingletonImageLoaderFactory { context ->
        newImageLoader(context)
    }

    PanoDialogStack(
        initialDialogArgs = dialogArgs,
        onNavigate = {
            DeepLinkUtils.handleNavigationFromInfoScreen(it)
        },
        onDismissRequest = onFinish,
        mainViewModel = viewModel,
    )
}