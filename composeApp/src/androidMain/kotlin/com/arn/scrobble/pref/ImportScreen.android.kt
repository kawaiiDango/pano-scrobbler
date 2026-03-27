package com.arn.scrobble.pref

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.arn.scrobble.utils.AndroidStuff

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

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN &&
            AndroidStuff.applicationContext.checkSelfPermission(
                Manifest.permission.ACCESS_LOCAL_NETWORK
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permRequest?.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
        } else {
            onGranted()
        }
    }
}