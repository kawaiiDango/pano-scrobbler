package com.arn.scrobble.info

import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest

class InfoMiscVM : ViewModel() {
    private val limit = 90

    private val artist = MutableStateFlow<Artist?>(null)
    private val track = MutableStateFlow<Track?>(null)

    private val pagingConfig = PagingConfig(
        pageSize = limit,
        initialLoadSize = limit,
        prefetchDistance = 0,
        enablePlaceholders = true
    )

    val similarArtists = artist.filterNotNull().flatMapLatest {
        Pager(
            config = pagingConfig,
            pagingSourceFactory = { InfoMiscPagingSource(it, Stuff.TYPE_ARTISTS) }
        ).flow
    }

    val topAlbums = artist.filterNotNull().flatMapLatest {
        Pager(
            config = pagingConfig,
            pagingSourceFactory = { InfoMiscPagingSource(it, Stuff.TYPE_ALBUMS) }
        ).flow
    }

    val topTracks = artist.filterNotNull().flatMapLatest {
        Pager(
            config = pagingConfig,
            pagingSourceFactory = { InfoMiscPagingSource(it, Stuff.TYPE_TRACKS) }
        ).flow
    }

    val similarTracks = track.filterNotNull().flatMapLatest {
        Pager(
            config = pagingConfig,
            pagingSourceFactory = { InfoMiscPagingSource(it, Stuff.TYPE_TRACKS) }
        ).flow
    }

    fun setArtist(artistp: Artist) {
        artist.value = artistp
    }

    fun setTrack(trackp: Track) {
        track.value = trackp
    }
}