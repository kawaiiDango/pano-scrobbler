package com.arn.scrobble.ui

import android.annotation.SuppressLint
import android.view.ScaleGestureDetector
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.wrappedGet
import com.arn.scrobble.utils.UiUtils.toast
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlin.math.roundToInt

class ScalableGrid(
    private val recyclerView: RecyclerView,
) {

    private val context get() = recyclerView.context

    private val mainPrefs = PlatformStuff.mainPrefs

    private val numColumnsList = mutableListOf(0)
    private var defaultNumCols = 2
    private var numColumnsToAdd = 0
    private var isSingleColumn = false

    private val numColumns
        get() = (defaultNumCols + numColumnsToAdd).coerceIn(
            Stuff.MIN_CHARTS_NUM_COLUMNS,
            Stuff.MAX_CHARTS_NUM_COLUMNS
        )

    init {
        setupGridResizing()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGridResizing() {
        val gridScaleGestureDetector = GridScaleGestureDetector(recyclerView) { scaleFactor ->
            if (scaleFactor > 1.0) resize(increase = false, fromMenu = false)
            else if (scaleFactor < 1.0) resize(increase = true, fromMenu = false)
        }

        val scaleGestureDetector = ScaleGestureDetector(context, gridScaleGestureDetector)
        scaleGestureDetector.isStylusScaleEnabled = true


        recyclerView.setOnTouchListener { view, motionEvent ->
            scaleGestureDetector.onTouchEvent(motionEvent)
            gridScaleGestureDetector.inProgress
        }

        initColumnCounts()
    }

    private fun initColumnCounts() {
        val totalWidth = context.resources.displayMetrics.widthPixels
        defaultNumCols = (totalWidth /
                context.resources.getDimension(R.dimen.grid_size)).roundToInt()
        val minNumCols = (totalWidth /
                context.resources.getDimension(R.dimen.max_grid_size)).roundToInt()
            .coerceAtLeast(Stuff.MIN_CHARTS_NUM_COLUMNS)
        val maxNumCols = (totalWidth /
                context.resources.getDimension(R.dimen.min_grid_size)).toInt()
            .coerceAtMost(Stuff.MAX_CHARTS_NUM_COLUMNS)

        val gridColumnsToAdd = runBlocking { mainPrefs.data.map { it.gridColumnsToAdd }.first() }
        val gridSingleColumn = runBlocking { mainPrefs.data.map { it.gridSingleColumn }.first() }

        numColumnsToAdd = gridColumnsToAdd
        numColumnsToAdd = numColumns.coerceIn(minNumCols..maxNumCols) - defaultNumCols

        if (numColumns == 1 || gridSingleColumn)
            isSingleColumn = true

        numColumnsList.clear()
        if (minNumCols > 1)
            numColumnsList.add(1)
        numColumnsList.addAll(minNumCols..maxNumCols)

        updateColumnCounts()
    }

    fun resize(increase: Boolean, fromMenu: Boolean = true) {
        var cols = numColumns
        if (isSingleColumn)
            cols = 1

        var idx = numColumnsList.indexOf(cols)
        if (increase)
            idx++
        else
            idx--

        cols = numColumnsList.wrappedGet(idx)

        runBlocking {
            if (cols == 1) {
                isSingleColumn = true
                numColumnsToAdd = cols - defaultNumCols
                mainPrefs.updateData { it.copy(gridSingleColumn = true) }
            } else {
                isSingleColumn = false
                numColumnsToAdd = cols - defaultNumCols
                mainPrefs.updateData {
                    it.copy(
                        gridSingleColumn = false,
                        gridColumnsToAdd = numColumnsToAdd
                    )
                }
            }

            val gridPinchLearnt = mainPrefs.data.map { it.gridPinchLearnt }.first()

            if (fromMenu && !gridPinchLearnt) {
                mainPrefs.updateData { it.copy(gridPinchLearnt = true) }
                context.toast(R.string.pinch_to_zoom)
            }
        }
        updateColumnCounts()
    }

    private fun updateColumnCounts() {
        var glm = recyclerView.layoutManager as GridLayoutManager?
        val prevColumnCount = glm?.spanCount ?: -1
        val newColumnCount = if (isSingleColumn) 1 else numColumns

        if (glm == null) {
            glm = GridLayoutManager(context, newColumnCount)
            recyclerView.layoutManager = glm
        } else {
            glm.spanCount = newColumnCount
        }

        if (prevColumnCount != -1 &&
            (newColumnCount != prevColumnCount) &&
            (recyclerView.adapter?.itemCount ?: -1) > 0
        ) {
            // relayout and animate?
            recyclerView.adapter?.notifyDataSetChanged()
        }
    }
}