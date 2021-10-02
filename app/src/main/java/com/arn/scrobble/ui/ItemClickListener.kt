package com.arn.scrobble.ui

import android.view.View
import androidx.annotation.RestrictTo

interface ItemClickListener {
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    fun onItemClick(view: View, position: Int)

    fun call(view: View, position: Int) {
        if (position >= 0)
            onItemClick(view, position)
    }
}