@file:Suppress("unused")

package com.arn.scrobble.ui

import android.content.Context
import android.graphics.Bitmap
import coil.size.Size
import coil.transform.Transformation
import com.arn.scrobble.ui.UiUtils.dp
import com.jabistudio.androidjhlabs.filter.CrystallizeFilter
import com.jabistudio.androidjhlabs.filter.util.AndroidUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class CrystallizeTransformation(private val context: Context) : Transformation {

    override val cacheKey = "${CrystallizeTransformation::class.java.name}-a"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap =
        withContext(Dispatchers.IO) {
            val outArr = CrystallizeFilter().apply {
                scale = 15.dp.toFloat()
                randomness = 0.01f
                edgeThickness = 0f
            }.filter(
                AndroidUtils.bitmapToIntArray(input),
                input.width,
                input.height
            )
            Bitmap.createBitmap(
                outArr,
                input.width,
                input.height,
                Bitmap.Config.ARGB_8888
            )
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is CrystallizeTransformation &&
                context == other.context
    }

    override fun hashCode(): Int {
        return context.hashCode()
    }

    override fun toString(): String {
        return "CrystallizeTransformation(context=$context)"
    }
}
