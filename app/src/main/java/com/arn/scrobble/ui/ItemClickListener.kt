package com.arn.scrobble.ui

import android.view.View

interface ItemClickListener {
    fun onItemClick(view: View, position: Int)
}