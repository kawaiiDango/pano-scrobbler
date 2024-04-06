package com.arn.scrobble.ui

import coil.key.Keyer
import coil.request.Options
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.Track

class MusicEntryReqKeyer : Keyer<MusicEntryImageReq> {
    override fun key(data: MusicEntryImageReq, options: Options) = genKey(data)

    companion object {
        fun genKey(data: MusicEntryImageReq): String {
            val prefix = "MusicEntryReqKeyer|${data.accountType}|"
            return when (data.musicEntry) {
                is Artist -> prefix + Artist::class.qualifiedName + "|" + data.musicEntry.name
                is Album -> prefix + Album::class.qualifiedName + "|" + data.musicEntry.artist!!.name + "|" + data.musicEntry.name
                is Track -> prefix + Track::class.qualifiedName + data.musicEntry.artist.name + "|" + data.musicEntry.album?.name + "|" + data.musicEntry.name
            }
        }
    }
}