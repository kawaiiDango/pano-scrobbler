package com.arn.scrobble.ui

import android.graphics.Bitmap
import coil.bitmap.BitmapPool
import coil.size.Size
import coil.transform.Transformation

class BitmapTransformation: Transformation {

    override fun key() = "BitmapTransformation"

    override suspend fun transform(pool: BitmapPool, input: Bitmap, size: Size) = input
}