package com.arn.scrobble.help

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PrivacyPolicyScreen(
    modifier: Modifier = Modifier,
    viewModel: MdViewerVM = viewModel {
        MdViewerVM(
            "https://kawaiidango.github.io/pano-scrobbler/privacy-policy.md",
        )
    }
) {
    val mdItems by viewModel.mdBlocks.collectAsStateWithLifecycle()
    if (mdItems != null) {
        MdText(
            mdItems!!,
            modifier = modifier
        )
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
        ) {
            CircularWavyProgressIndicator()
        }
    }
}