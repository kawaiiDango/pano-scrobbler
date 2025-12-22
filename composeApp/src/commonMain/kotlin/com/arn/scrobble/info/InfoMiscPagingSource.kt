package com.arn.scrobble.info

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.utils.Stuff

class InfoMiscPagingSource(
    private val entry: MusicEntry,
    private val type: Int,
) : PagingSource<Int, MusicEntry>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MusicEntry> {
        val requester = Requesters.lastfmUnauthedRequester
        val page = params.key ?: 1

        val result = when (entry) {
            is Artist if type == Stuff.TYPE_ARTISTS -> {
                requester.artistGetSimilar(entry, limit = params.loadSize)
            }

            is Artist if type == Stuff.TYPE_ALBUMS -> {
                requester.artistGetTopAlbums(entry, page = page, limit = params.loadSize)
                    .map { it.entries }
            }

            is Artist if type == Stuff.TYPE_TRACKS -> {
                requester.artistGetTopTracks(entry, page = page, limit = params.loadSize)
                    .map { it.entries }
            }

            is Track if type == Stuff.TYPE_TRACKS -> {
                requester.trackGetSimilar(entry, limit = params.loadSize)
            }

            else -> {
                throw IllegalArgumentException("Unknown type $type")
            }
        }


        return if (result.isSuccess) {
//            val pr = result.getOrNull()!!
//            val prevKey = if (pr.attr.page <= 1) null else pr.attr.page - 1
//            val nextKey =
//                if (firstPageOnly || pr.attr.totalPages <= pr.attr.page) null else pr.attr.page + 1
//            val total = pr.attr.total ?: 0
//            setTotal(total)

            LoadResult.Page(
                data = result.getOrNull()!!,
                prevKey = null,
                nextKey = null,
            )
        } else {
            LoadResult.Error(result.exceptionOrNull()!!)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, MusicEntry>): Int {
        return 1
    }

}