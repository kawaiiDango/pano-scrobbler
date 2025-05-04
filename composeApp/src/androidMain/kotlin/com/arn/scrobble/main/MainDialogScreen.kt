package com.arn.scrobble.main

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.setSingletonImageLoaderFactory
import com.arn.scrobble.imageloader.newImageLoader
import com.arn.scrobble.navigation.DeepLinkUtils
import com.arn.scrobble.navigation.PanoDialog


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanoMainDialogContent(
    currentDialogArgs: PanoDialog,
    onFinish: () -> Unit,
    viewModel: MainViewModel = viewModel { MainViewModel() },
) {
    var currentDialog by remember { mutableStateOf<PanoDialog?>(currentDialogArgs) }

    LaunchedEffect(currentDialog) {
        if (currentDialog == null)
            onFinish()
    }

    setSingletonImageLoaderFactory { context ->
        newImageLoader(context)
    }

    if (currentDialog != null) {
        PanoDialogs(
            dialogData = currentDialog!!,
            onDismiss = { currentDialog = null },
            onNavigate = { route ->
                DeepLinkUtils.handleNavigationFromInfoScreen(route)
                currentDialog = null
            },
            onOpenDialog = { currentDialog = it },
            navMetadataList = { emptyList() },
            mainViewModel = viewModel,
        )
    }
}