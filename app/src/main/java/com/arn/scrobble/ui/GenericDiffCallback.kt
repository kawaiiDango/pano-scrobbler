package com.arn.scrobble.ui

import androidx.recyclerview.widget.DiffUtil

class GenericDiffCallback<T : Any>(
    private val areContentsSame: ((o: T, n: T) -> Boolean) = { o, n -> o == n },
    private val areItemsSame: ((o: T, n: T) -> Boolean)
) :
    DiffUtil.ItemCallback<T>() {
    override fun areItemsTheSame(
        oldItem: T,
        newItem: T
    ) = areItemsSame(oldItem, newItem)

    override fun areContentsTheSame(
        oldItem: T,
        newItem: T
    ) = areContentsSame(oldItem, newItem)
}