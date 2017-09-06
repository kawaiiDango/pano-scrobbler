package com.arn.scrobble.ui

/**
 * Created by arn on 17/07/2017.
 */

import android.widget.AbsListView

abstract class EndlessScrollListener : AbsListView.OnScrollListener {
    // The minimum number of items to have below your current scroll position
    // before loading more.
    private var visibleThreshold = 3
    // The current offset index of data you have loaded
    private var currentPage = 0
    // The total number of items in the dataset after the last load
    private var previousTotalItemCount = 2 //// the header and the footer goddamit
    // True if we are still waiting for the last set of data to load.
    private var loading = true
    // Sets the starting page index
    private var startingPageIndex = 1

    constructor() {}

    constructor(visibleThreshold: Int) {
        this.visibleThreshold = visibleThreshold
    }

    constructor(visibleThreshold: Int, startPage: Int) {
        this.visibleThreshold = visibleThreshold
        this.startingPageIndex = startPage
        this.currentPage = startPage
    }


    override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
        // If the total item count is zero and the previous isn't, assume the
        // list is invalidated and should be reset back to initial state
        if (totalItemCount < previousTotalItemCount) {
            this.currentPage = this.startingPageIndex
            this.previousTotalItemCount = totalItemCount
            if (totalItemCount == 0) {
                this.loading = true
            }
        }
        // If it's still loading, we check to see if the dataset count has
        // changed, if so we conclude it has finished loading and update the current page
        // number and total item count.
        if (loading && totalItemCount > previousTotalItemCount) {
            loading = false
            previousTotalItemCount = totalItemCount
            currentPage++
        }

        // If it isn't currently loading, we check to see if we have breached
        // the visibleThreshold and need to reload more data.
        // If we do need to reload some more data, we execute onLoadMore to fetch the data.
        if (!loading && firstVisibleItem + visibleItemCount + visibleThreshold >= totalItemCount) {
            loading = onLoadMore(currentPage + 1, totalItemCount)
        }
    }

    // Defines the process for actually loading more data based on page
    // Returns true if more data is being loaded; returns false if there is no more data to load.
    abstract fun onLoadMore(page: Int, totalItemsCount: Int): Boolean

    override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
        // Don't take any action on changed
    }
}