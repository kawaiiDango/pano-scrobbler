package com.arn.scrobble.ui

import android.graphics.Bitmap
import coil.size.Size
import coil.transform.Transformation

class BitmapTransformation(
    override val cacheKey: String = "BitmapTransformation"
) : Transformation {
    override suspend fun transform(input: Bitmap, size: Size) = input
}