package com.arn.scrobble.info


import com.arn.scrobble.BasePagerAdapter
import com.arn.scrobble.BasePagerFragment
import com.arn.scrobble.R
import com.arn.scrobble.TabMetadata

class InfoPagerAdapter(fragment: BasePagerFragment) : BasePagerAdapter(fragment) {
    override val tabMetadata = listOf(
        TabMetadata(R.string.top_tracks, R.drawable.vd_note) { TrackExtraFragment().also {it.arguments = fragment.arguments} },
        TabMetadata(R.string.top_albums, R.drawable.vd_album) { AlbumExtraFragment().also {it.arguments = fragment.arguments} },
        TabMetadata(R.string.similar_artists, R.drawable.vd_mic) { ArtistExtraFragment().also {it.arguments = fragment.arguments} },
    )
}