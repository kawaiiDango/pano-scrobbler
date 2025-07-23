package com.arn.scrobble.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import qrcode.QRCode
import qrcode.raw.ErrorCorrectionLevel
import qrcode.render.extensions.drawQRCode

@Composable
actual fun QrCodeCanvas(url: String, modifier: Modifier) {
    val fgColor = MaterialTheme.colorScheme.onPrimaryContainer

    val qrCode = remember {
        QRCode.ofSquares()
            .withColor(fgColor.toArgb())
            .withErrorCorrectionLevel(ErrorCorrectionLevel.MEDIUM)
    }

    Canvas(
        modifier = modifier
            .aspectRatio(1f, true)
    ) {
        drawQRCode(qrCode.build(url))
    }
}