package com.arn.scrobble.graphics

import androidx.compose.ui.graphics.ImageBitmap
import coil3.request.SuccessResult

expect class PlatformBitmap {
    constructor(width: Int, height: Int)

    constructor(coilBitmap: Any)

    val width: Int
    val height: Int

    fun getPixels(
        pixels: IntArray,
        offset: Int,
        stride: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
    )

    fun setPixels(
        pixels: IntArray,
        offset: Int,
        stride: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
    )

    fun recycle()
}

expect fun PlatformBitmap.toImageBitmap(): ImageBitmap

expect fun PlatformBitmap.copyToCoilResult(successResult: SuccessResult): SuccessResult