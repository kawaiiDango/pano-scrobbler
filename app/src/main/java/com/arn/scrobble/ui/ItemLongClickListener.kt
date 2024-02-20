package com.arn.scrobble.ui

import android.view.View
import androidx.annotation.RestrictTo

interface ItemLongClickListener<T> {
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    fun onItemLongClick(view: View, position: Int, item: T)

    fun call(view: View, position: Int, item: () -> T) {
        if (position >= 0)
            onItemLongClick(view, position, item())
    }
}