package com.arn.scrobble.charts

import com.arn.scrobble.utils.Stuff

class ArtistChartsFragment : ChartsBaseFragment() {
    override val chartsType = Stuff.TYPE_ARTISTS
}

class AlbumChartsFragment : ChartsBaseFragment() {
    override val chartsType = Stuff.TYPE_ALBUMS
}

class TrackChartsFragment : ChartsBaseFragment() {
    override val chartsType = Stuff.TYPE_TRACKS
}