package com.arn.scrobble.charts

import com.arn.scrobble.Stuff

class ArtistChartsFragment : ChartsBaseFragment() {
    override val type = Stuff.TYPE_ARTISTS
}

class AlbumChartsFragment : ChartsBaseFragment() {
    override val type = Stuff.TYPE_ALBUMS
}

class TrackChartsFragment : ChartsBaseFragment() {
    override val type = Stuff.TYPE_TRACKS
}