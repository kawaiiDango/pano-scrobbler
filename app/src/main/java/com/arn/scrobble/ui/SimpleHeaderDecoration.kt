package com.arn.scrobble.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.dp


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
        val lm = (parent.adapter as LoadMoreGetter).loadMoreListener
        if (lm.loading && !lm.isAllPagesLoaded) {
            for (i in (parent.childCount - 1) downTo 0) {
                val view = parent.getChildAt(i) ?: break
                if (parent.getChildAdapterPosition(view) + 1 == parent.adapter?.itemCount){
                    val center = parent.width/2f
                    val step = 20.dp
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