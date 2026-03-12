package com.arn.scrobble.charts

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import coil3.Image
import coil3.toBitmap

actual fun CollageGeneratorVM.shareCollage(
    image: ImageBitmap,
    text: String?
) {
}

actual fun CollageGeneratorVM.drawCoilImage(
    drawScope: DrawScope,
    image: Image,
    dstOffset: IntOffset,
    dstSize: IntSize
) {
    drawScope.drawImage(
        image = image.toBitmap().asComposeImageBitmap(),
        dstOffset = dstOffset,
        dstSize = dstSize
    )
}