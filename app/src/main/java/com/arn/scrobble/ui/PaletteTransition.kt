package com.arn.scrobble.ui

import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.annotation.ExperimentalCoilApi
import coil.request.ImageResult
import coil.request.SuccessResult
import coil.transition.CrossfadeTransition
import coil.transition.Transition
import coil.transition.TransitionTarget
import com.arn.scrobble.Stuff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@ExperimentalCoilApi
class PaletteTransition(
    private val onGenerated: (Palette) -> Unit
) : Transition {
    private val delegate = CrossfadeTransition(Stuff.CROSSFADE_DURATION)

    override suspend fun transition(target: TransitionTarget, result: ImageResult) {
        // Execute the delegate transition.
        val delegateJob = coroutineScope {
            launch(Dispatchers.Main.immediate) {
                delegate.transition(target, result)
            }
        }

        // Compute the palette on a background thread.
        if (result is SuccessResult) {
            val bitmap = result.drawable.toBitmap()
            val palette = withContext(Dispatchers.IO) {
                Palette.Builder(bitmap).generate()
            }
            onGenerated(palette)
        }

        delegateJob.join()
    }
}