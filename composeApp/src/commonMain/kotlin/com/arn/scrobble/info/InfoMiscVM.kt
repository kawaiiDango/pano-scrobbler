package com.arn.scrobble.info

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.utils.Stuff

private const val limit = 90
private val pagingConfig = PagingConfig(
    pageSize = limit,
    initialLoadSize = limit,
    prefetchDistance = 0,
    enablePlaceholders = true
)

class SimilarTracksVM(track: Track) : ViewModel() {
    val similarTracks = Pager(
        config = pagingConfig,
        pagingSourceFactory = { InfoMiscPagingSource(track, Stuff.TYPE_TRACKS) }
    ).flow
        .cachedIn(viewModelScope)
}

class ArtistMiscVM(artist: Artist) : ViewModel() {
    val topAlbums = Pager(
        config = pagingConfig,
        pagingSourceFactory = { InfoMiscPagingSource(artist, Stuff.TYPE_ALBUMS) }
    ).flow
        .cachedIn(viewModelScope)

    val topTracks = Pager(
        config = pagingConfig,
        pagingSourceFactory = { InfoMiscPagingSource(artist, Stuff.TYPE_TRACKS) }
    ).flow
        .cachedIn(viewModelScope)

    val similarArtists = Pager(
        config = pagingConfig,
        pagingSourceFactory = { InfoMiscPagingSource(artist, Stuff.TYPE_ARTISTS) }
    ).flow
        .cachedIn(viewModelScope)
}