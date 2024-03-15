package com.arn.scrobble.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.arn.scrobble.R
import com.arn.scrobble.utils.UiUtils.dp
import kotlin.math.min


// from https://stackoverflow.com/a/16018517/1067596

class SquareFrameLayout(context: Context, attributeSet: AttributeSet?, defStyle: Int) :
    FrameLayout(context, attributeSet, defStyle) {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)

    private var maxSize = 720.dp

    init {
        val a =
            context.obtainStyledAttributes(attributeSet, R.styleable.SquareFrameLayout, defStyle, 0)
        maxSize = a.getDimensionPixelSize(R.styleable.SquareFrameLayout_maxSize, maxSize)
        a.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val size = if (widthMode == MeasureSpec.EXACTLY && widthSize > 0) {
            widthSize
        } else if (heightMode == MeasureSpec.EXACTLY && heightSize > 0) {
            heightSize
        } else if (widthSize < heightSize)
            widthSize
        else {
            heightSize
        }

        val finalMeasureSpec = MeasureSpec.makeMeasureSpec(min(size, maxSize), MeasureSpec.EXACTLY)
        super.onMeasure(finalMeasureSpec, finalMeasureSpec)
    }
}