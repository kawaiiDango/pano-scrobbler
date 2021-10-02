package com.arn.scrobble.ui

import android.view.View
import androidx.annotation.RestrictTo

interface ItemLongClickListener {
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    fun onItemLongClick(view: View, position: Int)

    fun call(view: View, position: Int) {
        if (position >= 0)
            onItemLongClick(view, position)
    }
}