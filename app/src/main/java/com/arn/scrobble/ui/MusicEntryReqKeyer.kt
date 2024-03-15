package com.arn.scrobble.ui

import coil3.key.Keyer
import coil3.request.Options
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Track

class MusicEntryReqKeyer : Keyer<MusicEntryImageReq> {
    override fun key(data: MusicEntryImageReq, options: Options) = genKey(data.musicEntry)

    companion object {
        fun genKey(entry: MusicEntry): String {
            val prefix = "MusicEntryReqKeyer|${Scrobblables.current?.userAccount?.type?.name}|"
            return when (entry) {
                is Artist -> prefix + Artist::class.java.name + entry.name
                is Album -> prefix + entry.artist!!.name + Album::class.java.name + entry.name
                is Track -> prefix + entry.artist.name + Track::class.java.name + entry.name
            }
        }
    }
}