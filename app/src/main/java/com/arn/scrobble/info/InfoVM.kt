package com.arn.scrobble.info

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.LFMRequester
import com.arn.scrobble.NLService
import com.hadilq.liveevent.LiveEvent
import de.umass.lastfm.Album
import de.umass.lastfm.Artist
import de.umass.lastfm.MusicEntry
import de.umass.lastfm.Track


class InfoVM : ViewModel() {

    val infoMapReceiver = LiveEvent<Map<String, MusicEntry>>()
    val infoMap = mutableMapOf<String, MusicEntry>()
    val picExpandedMap = mutableMapOf<String, Boolean>()
    val infoExtraExpandedMap = mutableMapOf(
        NLService.B_TRACK to false,
        NLService.B_ARTIST to false,
        NLService.B_ALBUM to false,
        NLService.B_ALBUM_ARTIST to false,
    )

    fun loadInfo(artist: String, album: String?, track: String?, username: String?) {
        val albumFirst = track == null && album != null

        // setInitialInfo
        if (track != null)
            infoMap[NLService.B_TRACK] = Track(track, null, artist)

        if (!albumFirst)
            infoMap[NLService.B_ARTIST] = Artist(artist, null)

        if (album != null)
            infoMap[NLService.B_ALBUM] = Album(album, null, artist)

        if (albumFirst)
            infoMap[NLService.B_ARTIST] = Artist(artist, null)

        LFMRequester(viewModelScope, infoMapReceiver).apply {
            getInfos(artist, album, track, username)
        }
    }
}