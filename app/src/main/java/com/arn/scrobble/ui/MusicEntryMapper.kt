package com.arn.scrobble.ui

import coil.map.Mapper
import coil.request.Options
import com.arn.scrobble.api.lastfm.MusicEntry

class MusicEntryMapper : Mapper<MusicEntry, MusicEntryImageReq> {
    override fun map(data: MusicEntry, options: Options) = MusicEntryImageReq(data)
}