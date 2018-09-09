package com.arn.scrobble.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.RecentsAdapter
import com.arn.scrobble.Stuff


/**
 * https://gist.github.com/uhfath/368804ce8fe08274e019bcaeab501783
 * An empty header (or footer) decoration for RecyclerView, since RecyclerView can't clipToPadding
 */
class SimpleHeaderDecoration(private val headerHeight: Int, private val footerHeight: Int) : RecyclerView.ItemDecoration() {

    private val paint = Paint()
    private val path = Path()
    init {
        paint.setARGB(255, 255, 255, 255)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
//        paint.pathEffect = DashPathEffect(floatArrayOf(20f, 40f), 0f)
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if ((parent.adapter as RecentsAdapter).getLoading()) {
            for (i in 0..parent.childCount) {
                val view = parent.getChildAt(i)
                if (parent.getChildAdapterPosition(view) + 1 == parent.adapter?.itemCount){
                    val center = view.width/2f
                    val step = Stuff.dp2px(20, parent.context)
                    val y = (view.bottom + footerHeight / 2).toFloat()

                    path.reset()
                    path.addCircle(center - step, y, step/6f, Path.Direction.CCW)
                    path.addCircle(center, y, step/6f, Path.Direction.CCW)
                    path.addCircle(center + step, y, step/6f, Path.Direction.CCW)
                    c.drawPath(path, paint)
                    break
                }

            }
        }
//        c.drawText("...", )
    }
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView,
                                state: RecyclerView.State) {
        val childAdapterPosition = parent.getChildAdapterPosition(view)
        when (childAdapterPosition) {
            0 -> outRect.top = headerHeight
            parent.adapter!!.itemCount - 1 -> outRect.bottom = footerHeight
            else -> outRect.set(0, 0, 0, 0)
        }
    }
}