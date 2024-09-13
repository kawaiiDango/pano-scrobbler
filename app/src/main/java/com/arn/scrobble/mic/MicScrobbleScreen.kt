package com.arn.scrobble.mic

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Keep
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R
import com.arn.scrobble.ui.ExtraBottomSpace
import com.arn.scrobble.ui.ScreenParent
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable
private fun MicIdentifyContent(
    viewModel: MicScrobbleVM = viewModel(),
    modifier: Modifier = Modifier
) {
    val grantMicPermText = stringResource(R.string.grant_rec_perm)
    val micPermRequest = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (!viewModel.started) viewModel.start()
        } else {
            viewModel.statusText.value = grantMicPermText
        }
    }

    val statusText by viewModel.statusText.collectAsStateWithLifecycle()
    val progressValue by viewModel.progressValue.collectAsStateWithLifecycle()
    val fadeValue by viewModel.fadeValue.collectAsStateWithLifecycle()
    var canCancelScrobble by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300)
        if (!viewModel.started) {
            startListening(micPermRequest, viewModel)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stop()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.scrobbleEvent.collectLatest {
            canCancelScrobble = true
            delay(Stuff.SCROBBLE_FROM_MIC_DELAY)
            canCancelScrobble = false
        }
    }

    Box(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = stringResource(R.string.shazam_scrobbling).replace("Shazam", "S app"),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.powered_acr),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(end = 16.dp, bottom = 8.dp)
                    .align(Alignment.End)
                    .alpha(0.5f)
            )

            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(CircleShape)
                    .clickable {
                        if (!viewModel.started)
                            startListening(micPermRequest, viewModel)
                        else
                            viewModel.stop()
                    },
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { progressValue / 1000f },
                    modifier = Modifier.size(200.dp),
                )
                Image(
                    imageVector = Icons.Outlined.GraphicEq,
                    contentDescription = null,
                    modifier = Modifier
                        .size(150.dp)
                        .alpha(fadeValue)
                )
            }

            Text(
                text = statusText,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .fillMaxWidth()
            )

            AnimatedVisibility(visible = canCancelScrobble) {
                Button(
                    onClick = {
                        canCancelScrobble = false
                        viewModel.cancelScrobble()
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            }
            ExtraBottomSpace()
        }
    }
}

private fun startListening(
    micPermRequest: ActivityResultLauncher<String>,
    viewModel: MicScrobbleVM
) {
    if (ContextCompat.checkSelfPermission(
            PlatformStuff.application,
            Manifest.permission.RECORD_AUDIO
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        micPermRequest.launch(Manifest.permission.RECORD_AUDIO)
    } else if (!viewModel.started) {
        viewModel.start()
    }
}

@Keep
@Composable
fun MicScrobbleScreen() {
    ScreenParent { MicIdentifyContent(modifier = it) }
}