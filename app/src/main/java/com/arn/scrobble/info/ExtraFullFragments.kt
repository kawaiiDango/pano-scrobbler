package com.arn.scrobble.info

import com.arn.scrobble.utils.Stuff

class ArtistExtraFragment : InfoExtraFullFragment() {
    override val type = Stuff.TYPE_ARTISTS
}

class AlbumExtraFragment : InfoExtraFullFragment() {
    override val type = Stuff.TYPE_ALBUMS
}

class TrackExtraFragment : InfoExtraFullFragment() {
    override val type = Stuff.TYPE_TRACKS
}