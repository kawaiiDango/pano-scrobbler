package com.arn.scrobble.ui

import android.content.Context
import android.util.AttributeSet
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout

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
        //don't call if state is gonna change anyways
        val behaviour = (layoutParams as CoordinatorLayout.LayoutParams)
                .behavior as DisableableAppBarLayoutBehavior
        behaviour.isEnabled = expanded
        if ((!expanded && isCollapsed) || (expanded && isExpanded))
            onStateChangeListener?.invoke(state)
        else
            super.setExpanded(expanded, animate)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        addOnOffsetChangedListener(this)
    }

    override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
        val oldState = state
        state = when {
            verticalOffset == 0 -> EXPANDED
            Math.abs(verticalOffset) >= appBarLayout.totalScrollRange -> COLLAPSED
            else -> IDLE
        }
        if (state != oldState)
            onStateChangeListener?.invoke(state)
    }

    companion object {
        const val COLLAPSED = 0
        const val IDLE = 1
        const val EXPANDED = 2
    }
}