package com.arn.scrobble.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import com.arn.scrobble.R
import com.google.android.material.textview.MaterialTextView

class OutlineTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
): MaterialTextView(context, attrs, defStyle) {

    private val defaultStrokeWidth = 0f
    private var isDrawing = false

    var strokeColor = 0
    var strokeWidth = 0f

    init {
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