package com.arn.scrobble.friends

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.UserCached.Companion.toUserCached

class FriendsPagingSource(
    private val username: String,
    private val onSetLastFriendsRefreshTime: (Long?) -> Unit,
    private val setTotal: (Int) -> Unit,
) : PagingSource<Int, UserCached>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, UserCached> {
        val result = Scrobblables.current!!.getFriends(
            params.key ?: 1,
            username,
//                cached = !loadedInitialCachedVersion
        )

        return if (result.isSuccess) {
            val pr = result.getOrNull()!!
            val prevKey = if (pr.attr.page <= 1) null else pr.attr.page - 1
            val nextKey = if (pr.attr.totalPages <= pr.attr.page) null else pr.attr.page + 1
            val total = pr.attr.total ?: 0


            onSetLastFriendsRefreshTime(
                if (pr.attr.page == 1)
                    System.currentTimeMillis()
                else
                    null
            )

            setTotal(total)

            LoadResult.Page(
                data = pr.entries.map { it.toUserCached() },
                prevKey = prevKey,
                nextKey = nextKey,
                itemsAfter = if (nextKey == null) 0 else 2
            )
        } else {
            LoadResult.Error(result.exceptionOrNull()!!)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, UserCached>): Int {
        return 1
    }

}