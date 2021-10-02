package com.arn.scrobble.ui

import android.view.View
import androidx.annotation.RestrictTo

interface FocusChangeListener {
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    fun onFocus(view: View, position: Int)

    fun call(view: View, position: Int) {
        if (position >= 0)
            onFocus(view, position)
    }
}