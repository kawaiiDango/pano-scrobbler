package com.arn.scrobble.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import com.arn.scrobble.R
import com.google.android.material.textview.MaterialTextView

class OutlineTextView : MaterialTextView {

    private val defaultStrokeWidth = 0f
    private var isDrawing = false

    private var strokeColor = 0
    private var strokeWidth = 0f

    constructor(context: Context) : super(context) {
        initResources(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initResources(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        initResources(context, attrs)
    }

    private fun initResources(context: Context, attrs: AttributeSet?) {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.OutlineTextView)
            strokeColor = a.getColor(
                R.styleable.OutlineTextView_outlineColor, currentTextColor
            )
            strokeWidth = a.getDimension(
                R.styleable.OutlineTextView_outlineWidth, defaultStrokeWidth
            )

            a.recycle()
        } else {
            strokeColor = currentTextColor
            strokeWidth = defaultStrokeWidth
        }
        setStrokeWidth(strokeWidth)
    }

    fun setStrokeColor(color: Int) {
        strokeColor = color
    }

    fun setStrokeWidth(widthPx: Float) {
        strokeWidth = widthPx
    }

    fun setStrokeWidth(unit: Int, width: Float) {
        strokeWidth = TypedValue.applyDimension(
            unit, width, context.resources.displayMetrics
        )
    }

    override fun invalidate() {
        if (isDrawing) return
        super.invalidate()
    }


    override fun onDraw(canvas: Canvas) {
        if (strokeWidth > 0) {
            isDrawing = true

            val prevTextColor = currentTextColor
            val p = paint
            p.style = Paint.Style.STROKE
            p.strokeCap = Paint.Cap.ROUND
            p.strokeWidth = strokeWidth
            setTextColor(strokeColor)
            super.onDraw(canvas)

            setTextColor(prevTextColor)
            p.style = Paint.Style.FILL
            super.onDraw(canvas)

            isDrawing = false
        } else {
            super.onDraw(canvas)
        }
    }

}