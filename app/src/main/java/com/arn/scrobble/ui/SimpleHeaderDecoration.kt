package com.arn.scrobble.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.ui.UiUtils.dp
import com.google.android.material.color.MaterialColors


/**
 * https://gist.github.com/uhfath/368804ce8fe08274e019bcaeab501783
 * An empty header (or footer) decoration for RecyclerView, since RecyclerView can't clipToPadding
 */
class SimpleHeaderDecoration(
    private val headerHeight: Int = 0,
    private val footerHeight: Int = 25.dp
) : RecyclerView.ItemDecoration() {

    private val step = 20.dp
    private val paint = Paint()
    private val path = Path()
    private var paintInited = false

    private fun initPaint(context: Context) {
        paint.color = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnBackground, null)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3.dp.toFloat()

        paintInited = true
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (!paintInited)
            initPaint(parent.context)

        val lm = (parent.adapter as LoadMoreGetter).loadMoreListener
        if (lm.loading && !lm.isAllPagesLoaded) {
            for (i in (parent.childCount - 1) downTo 0) {
                val view = parent.getChildAt(i) ?: break
                if (parent.getChildAdapterPosition(view) + 1 == parent.adapter?.itemCount) {
                    val center = parent.width / 2f
                    val y = (view.bottom + footerHeight / 2).toFloat()

                    path.reset()
                    path.addCircle(center - step, y, step / 6f, Path.Direction.CCW)
                    path.addCircle(center, y, step / 6f, Path.Direction.CCW)
                    path.addCircle(center + step, y, step / 6f, Path.Direction.CCW)
                    c.drawPath(path, paint)
                    break
                }

            }
        }
//        c.drawText("...", )
    }

    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val childAdapterPosition = parent.getChildAdapterPosition(view)
        val span =
            if (parent.layoutManager is GridLayoutManager)
                (parent.layoutManager as GridLayoutManager).spanCount
            else
                1
        when {
            childAdapterPosition < span -> outRect.top = headerHeight
            childAdapterPosition >= parent.adapter!!.itemCount - span ->
                outRect.bottom = footerHeight
            else -> outRect.set(0, 0, 0, 0)
        }
    }
}