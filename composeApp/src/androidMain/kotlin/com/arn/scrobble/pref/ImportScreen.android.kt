package com.arn.scrobble.pref

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.arn.scrobble.ui.AlertDialogOk
import com.arn.scrobble.utils.AndroidStuff
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.missing_local_network_permission

@Composable
actual fun ImportScreenPermissionsRequest(
    onGranted: () -> Unit,
    onDenied: () -> Unit,
) {
    val permRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted)
                onGranted()
            else
                onDenied()
        }
    } else
        null

    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN &&
            AndroidStuff.applicationContext.checkSelfPermission(
                Manifest.permission.ACCESS_LOCAL_NETWORK
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            showDialog = true
        } else {
            onGranted()
        }
    }

    if (showDialog && Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
        AlertDialogOk(
            text = stringResource(Res.string.missing_local_network_permission),
            onConfirmation = {
                showDialog = false
                permRequest?.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
            }
        )
    }
}