package com.arn.scrobble.ui

import android.view.View
import androidx.annotation.RestrictTo

interface ItemClickListener<T> {
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    fun onItemClick(view: View, position: Int, item: T)

    fun call(view: View, position: Int, item: () -> T) {
        if (position >= 0)
            onItemClick(view, position, item())
    }
}