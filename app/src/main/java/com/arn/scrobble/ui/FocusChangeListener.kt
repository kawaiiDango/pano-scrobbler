package com.arn.scrobble.ui

import android.view.View

interface FocusChangeListener {
    fun onFocus(view: View, position: Int)
}