package com.arn.scrobble.ui

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.animation.DecelerateInterpolator
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.updateLayoutParams
import com.arn.scrobble.R
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.UiUtils
import com.arn.scrobble.utils.UiUtils.getDimenFromAttr
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import kotlin.math.abs


/**
 * Created by arn on 03/01/2018.
 */
// https://gist.github.com/skimarxall/863585dcd7abde8f4153

class StatefulAppBar : AppBarLayout, AppBarLayout.OnOffsetChangedListener {
    var state = EXPANDED //important. it always starts as expanded
    var onStateChangeListener: ((Int) -> Unit)? = null

    val isExpanded: Boolean
        get() = state == EXPANDED

    val isCollapsed: Boolean
        get() = state == COLLAPSED

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun setExpanded(expanded: Boolean, animate: Boolean) {

        updateHeight(expanded)

        if ((!expanded && isCollapsed) || (expanded && isExpanded))
            onStateChangeListener?.invoke(state)

        super.setExpanded(expanded, animate)
    }

    private fun setScrollEnabled(enabled: Boolean) {
        val behaviour = (layoutParams as CoordinatorLayout.LayoutParams)
            .behavior as? DisableableAppBarLayoutBehavior
        behaviour?.isEnabled = enabled
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        addOnOffsetChangedListener(this)
    }

    override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
        val oldState = state
        state = when {
            verticalOffset == 0 -> EXPANDED
            abs(verticalOffset) >= appBarLayout.totalScrollRange -> COLLAPSED
            else -> IDLE
        }
        if (state != oldState)
            onStateChangeListener?.invoke(state)
    }

    fun expandTillToolbar(animate: Boolean = true) {
        if (!Stuff.isTv) {
            updateHeight(false)
            super.setExpanded(true, animate)
        } else {
            super.setExpanded(false, animate)
        }
    }

    fun updateHeight(large: Boolean) {
        val ctl = getChildAt(0) as CollapsingToolbarLayout
        val ctlHeight = if (large) {
            if (UiUtils.isTabletUi && (parent as CoordinatorLayout).height <
                1.5 * context.resources.getDimensionPixelSize(R.dimen.app_bar_height)
            )
                context.getDimenFromAttr(com.google.android.material.R.attr.collapsingToolbarLayoutMediumSize)
            else
                context.resources.getDimensionPixelSize(R.dimen.app_bar_height)
        } else {
            context.getDimenFromAttr(com.google.android.material.R.attr.collapsingToolbarLayoutMediumSize)
        }

        val prevHeight = ctl.layoutParams.height

        if (prevHeight != ctlHeight) {
            if (ctl.getTag(R.id.inited) == null) { // prevent visual jerk
                ctl.updateLayoutParams {
                    height = ctlHeight
                }
                ctl.setTag(R.id.inited, true)
                return
            }

            ctl.animation?.cancel()
            ValueAnimator.ofInt(prevHeight, ctlHeight).apply {
                interpolator = DecelerateInterpolator()
                addUpdateListener { value ->
                    ctl.updateLayoutParams {
                        height = value.animatedValue as Int
                    }
                }
                start()
            }
        }
    }

    companion object {
        const val COLLAPSED = 0
        const val IDLE = 1
        const val EXPANDED = 2
    }
}