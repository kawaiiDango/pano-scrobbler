package com.arn.scrobble.ui

import android.content.Context
import android.util.AttributeSet
import com.arn.scrobble.R
import com.google.android.material.textview.MaterialTextView
import kotlin.math.roundToInt


class ScalableDrawableTextView(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int
) : MaterialTextView(
    context,
    attrs,
    defStyleAttr,
    defStyleRes
) {
    private var mDrawableWidth = 0
    private var mDrawableHeight = 0

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(
        context,
        attrs,
        defStyleAttr,
        0
    )

    init {
        val array = context.obtainStyledAttributes(
            attrs,
            R.styleable.ScalableDrawableTextView,
            defStyleAttr,
            defStyleRes
        )
        try {
            mDrawableWidth = array.getDimensionPixelSize(
                R.styleable.ScalableDrawableTextView_compoundDrawableWidth,
                -1
            )
            mDrawableHeight = array.getDimensionPixelSize(
                R.styleable.ScalableDrawableTextView_compoundDrawableHeight,
                -1
            )
        } finally {
            array.recycle()
        }
        if (mDrawableWidth > 0 || mDrawableHeight > 0) {
            initCompoundDrawableSize()
        }
    }

    private fun initCompoundDrawableSize() {
        val drawables = compoundDrawablesRelative
        for (drawable in drawables) {
            if (drawable == null) {
                continue
            }
            val realBounds = drawable.bounds
            val scaleFactor = realBounds.height().toFloat() / realBounds.width()
            var drawableWidth = realBounds.width().toFloat()
            var drawableHeight = realBounds.height().toFloat()
            if (mDrawableWidth > 0) {
                // save scale factor of image
                if (drawableWidth > mDrawableWidth) {
                    drawableWidth = mDrawableWidth.toFloat()
                    drawableHeight = drawableWidth * scaleFactor
                }
            }
            if (mDrawableHeight > 0) {
                // save scale factor of image
                if (drawableHeight > mDrawableHeight) {
                    drawableHeight = mDrawableHeight.toFloat()
                    drawableWidth = drawableHeight / scaleFactor
                }
            }
            realBounds.right = realBounds.left + drawableWidth.roundToInt()
            realBounds.bottom = realBounds.top + drawableHeight.roundToInt()
            drawable.bounds = realBounds
        }
        setCompoundDrawablesRelative(drawables[0], drawables[1], drawables[2], drawables[3])
    }

    override fun setCompoundDrawablesRelativeWithIntrinsicBounds(
        start: Int,
        top: Int,
        end: Int,
        bottom: Int
    ) {
        super.setCompoundDrawablesRelativeWithIntrinsicBounds(start, top, end, bottom)
        initCompoundDrawableSize()
    }
}