package com.arn.scrobble.charts

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.MusicEntry

class ChartsPagingSource(
    private val input: ChartsLoaderInput,
    private val type: Int,
    private val networkOnly: Boolean,
    private val setTotal: (Int) -> Unit,
) : PagingSource<Int, MusicEntry>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MusicEntry> {
        val result = Scrobblables.current!!
            .getChartsWithStonks(
                type = type,
                timePeriod = input.timePeriod,
                prevTimePeriod = input.prevPeriod,
                page = params.key ?: 1,
                networkOnly = networkOnly,
                username = input.username,
            )

        return if (result.isSuccess) {
            val pr = result.getOrNull()!!
            val prevKey = if (pr.attr.page <= 1) null else pr.attr.page - 1
            val nextKey =
                if (input.firstPageOnly || pr.attr.totalPages <= pr.attr.page) null else pr.attr.page + 1
            val total = pr.attr.total ?: 0
            setTotal(total)

            LoadResult.Page(
                data = pr.entries,
                prevKey = prevKey,
                nextKey = nextKey,
                itemsAfter = if (nextKey == null) 0 else 2
            )
        } else {
            LoadResult.Error(result.exceptionOrNull()!!)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, MusicEntry>): Int {
        return 1
    }

}