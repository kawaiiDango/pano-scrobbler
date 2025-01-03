package com.arn.scrobble.imageloader

import co.touchlab.kermit.Logger
import coil3.Bitmap
import coil3.intercept.Interceptor
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.toBitmap
import com.arn.scrobble.graphics.PlatformBitmap
import com.arn.scrobble.graphics.copyToCoilResult
import com.jabistudio.androidjhlabs.filter.CellularCrystallizeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi

class DemoInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val imgUrl = chain.request.data as? String
        if (imgUrl != null &&
            (imgUrl.endsWith(".webp") || imgUrl.endsWith(".gif") || imgUrl.startsWith("https://i.scdn.co"))
        ) {
            val imageResult = chain.proceed()
            val successResult = imageResult as? SuccessResult

            if (successResult != null) {
                val bitmap = successResult.image.toBitmap()
                try {
                    val transformedBitmap = transform(bitmap)
                    return transformedBitmap.copyToCoilResult(imageResult)
                } catch (e: Exception) {
                    Logger.e(e) { "Failed to transform image" }
                }
            }

            return imageResult
        } else
            return chain.proceed()
    }

    // todo don't hardcode the display scaling
    private val Int.dp
        get() = (this * 1.5).toInt()


    @OptIn(ExperimentalResourceApi::class)
    private suspend fun transform(input: Bitmap): PlatformBitmap =
        withContext(Dispatchers.IO) {
            val inputImage = PlatformBitmap(input)
            val inputIntArr = IntArray(inputImage.width * inputImage.height)

            inputImage.getPixels(
                inputIntArr,
                0,
                inputImage.width,
                0,
                0,
                inputImage.width,
                inputImage.height
            )

            val outIntArr = CellularCrystallizeFilter().apply {
                scale = 15.dp.toFloat()
                randomness = 0.01f
                edgeThickness = 0f
            }.filter(
                inputIntArr,
                inputImage.width,
                inputImage.height
            )

            inputImage.setPixels(
                outIntArr,
                0,
                inputImage.width,
                0,
                0,
                inputImage.width,
                inputImage.height
            )

            inputImage
        }

}