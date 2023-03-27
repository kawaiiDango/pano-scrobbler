package com.arn.scrobble.charts


import com.arn.scrobble.BasePagerAdapter
import com.arn.scrobble.BasePagerFragment
import com.arn.scrobble.R
import com.arn.scrobble.TabMetadata

class ChartsPagerAdapter(fragment: BasePagerFragment) : BasePagerAdapter(fragment) {
    override val tabMetadata = listOf(
        TabMetadata(R.string.artists, R.drawable.vd_mic) { ArtistChartsFragment() },
        TabMetadata(R.string.albums, R.drawable.vd_album) { AlbumChartsFragment() },
        TabMetadata(R.string.tracks, R.drawable.vd_note) { TrackChartsFragment() },
    )
}