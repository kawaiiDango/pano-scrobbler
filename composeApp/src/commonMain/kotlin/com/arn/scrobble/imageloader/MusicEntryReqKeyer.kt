package com.arn.scrobble.imageloader

import coil3.key.Keyer
import coil3.request.Options
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.Track

class MusicEntryReqKeyer : Keyer<MusicEntryImageReq> {
    override fun key(data: MusicEntryImageReq, options: Options) = genKey(data)

    companion object {
        fun genKey(data: MusicEntryImageReq): String {
            val prefix =
                "MusicEntryReqKeyer|accountType=${data.accountType}"
            return when (data.musicEntry) {
                is Artist -> prefix + "|artist=" + data.musicEntry.name
                is Album -> prefix + "|artist=" + data.musicEntry.artist!!.name + "|album=" + data.musicEntry.name
                is Track -> prefix + "|artist=" + data.musicEntry.artist.name + "|album=" + data.musicEntry.album?.name + "|track=" + data.musicEntry.name
            }
        }
    }
}