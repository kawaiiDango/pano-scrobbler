package com.arn.scrobble.ui

import android.widget.Filter

class NoOpFilter : Filter() {
    private val noOpResult = FilterResults()
    override fun performFiltering(constraint: CharSequence?) = noOpResult
    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {}
}