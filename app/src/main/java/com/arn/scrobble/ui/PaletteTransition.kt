package com.arn.scrobble.ui

import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.request.ImageResult
import coil.request.SuccessResult
import coil.transition.CrossfadeTransition
import coil.transition.Transition
import coil.transition.TransitionTarget
import com.arn.scrobble.utils.Stuff

class PaletteTransition(
    private val target: TransitionTarget,
    private val result: ImageResult,
    private val onGenerated: (Palette) -> Unit
) : Transition {
    private val delegate = CrossfadeTransition.Factory(Stuff.CROSSFADE_DURATION)

    override fun transition() {
        // Execute the delegate transition.
        delegate.create(target, result).transition()

        // Compute the palette on a background thread.
        if (result is SuccessResult) {
            val bitmap = result.drawable.toBitmap()
            Palette.Builder(bitmap).generate {
                onGenerated(it ?: return@generate)
            }
        }
    }

    class Factory(private val onGenerated: (Palette) -> Unit) : Transition.Factory {
        override fun create(target: TransitionTarget, result: ImageResult) =
            PaletteTransition(target, result, onGenerated)
    }
}