package com.arn.scrobble.ui

import android.view.View

interface ItemLongClickListener {
    fun onItemLongClick(view: View, position: Int)
}