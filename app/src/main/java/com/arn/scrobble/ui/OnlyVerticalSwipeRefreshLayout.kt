package com.arn.scrobble.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.arn.scrobble.Stuff
import kotlin.math.abs

// https://stackoverflow.com/a/34224634/1067596

class OnlyVerticalSwipeRefreshLayout(
    context: Context,
    attrs: AttributeSet?,
) : SwipeRefreshLayout(context, attrs) {

    constructor(context: Context) : this(context, null)

    private val touchSlop: Int
    private var prevX = 0f
    private var declined = false

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (event.pointerCount > 1)
            return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val motionEvent = MotionEvent.obtain(event)
                prevX = motionEvent.x
                motionEvent.recycle()
                declined = false // New action
            }
            MotionEvent.ACTION_MOVE -> {
                val eventX = event.x
                val xDiff = abs(eventX - prevX)
                if (declined || xDiff > touchSlop) {
                    declined = true // Memorize
                    return false
                }
            }
        }
        return super.onInterceptTouchEvent(event)
    }

    init {
        touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    }
}