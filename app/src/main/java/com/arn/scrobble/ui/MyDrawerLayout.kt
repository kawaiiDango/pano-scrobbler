package com.arn.scrobble.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.drawerlayout.widget.DrawerLayout

class MyDrawerLayout : DrawerLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    private var m_disallowIntercept = false
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean { // as the drawer intercepts all touches when it is opened
// we need this to let the content beneath the drawer to be touchable
        return !m_disallowIntercept && super.onInterceptTouchEvent(ev)
    }

    override fun setDrawerLockMode(lockMode: Int) {
        super.setDrawerLockMode(lockMode)
        // if the drawer is locked, then disallow interception
        m_disallowIntercept = lockMode == LOCK_MODE_LOCKED_OPEN
        if (lockMode == LOCK_MODE_LOCKED_OPEN)
            setScrimColor(0)
    }
}