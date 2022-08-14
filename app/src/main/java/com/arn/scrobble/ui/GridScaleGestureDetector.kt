package com.arn.scrobble.ui

import android.view.ScaleGestureDetector
import android.view.View

class GridScaleGestureDetector(
    private val view: View,
    private val onScaleEnd: (scaleFactor: Float) -> Unit
): ScaleGestureDetector.SimpleOnScaleGestureListener() {
    private var cumulativeScaleFactor = 1f
    var inProgress = false

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        cumulativeScaleFactor *= detector.scaleFactor
        view.scaleX = cumulativeScaleFactor
        view.scaleY = cumulativeScaleFactor
        return true
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        cumulativeScaleFactor = 1f
        inProgress = true
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        onScaleEnd(cumulativeScaleFactor)

        view.scaleX = 1f
        view.scaleY = 1f

        inProgress = false
    }
}