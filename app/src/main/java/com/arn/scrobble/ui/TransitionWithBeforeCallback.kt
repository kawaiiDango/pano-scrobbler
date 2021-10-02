package com.arn.scrobble.ui

import coil.annotation.ExperimentalCoilApi
import coil.request.ImageResult
import coil.transition.CrossfadeTransition
import coil.transition.Transition
import coil.transition.TransitionTarget
import com.arn.scrobble.Stuff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@ExperimentalCoilApi
class TransitionWithBeforeCallback(
    private val onBeforeTransition: () -> Unit
) : Transition {
    private val delegate = CrossfadeTransition(Stuff.CROSSFADE_DURATION)

    override suspend fun transition(target: TransitionTarget, result: ImageResult) {
        if (!(result.request.data as? String).isNullOrEmpty()) {
            onBeforeTransition()
        }
        // Execute the delegate transition.
        val delegateJob = coroutineScope {
            launch(Dispatchers.Main.immediate) {
                delegate.transition(target, result)
            }
        }

        delegateJob.join()
    }
}