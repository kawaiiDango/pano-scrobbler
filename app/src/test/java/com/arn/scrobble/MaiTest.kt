package com.arn.scrobble

/**
 * Created by arn on 20/09/2017.
 */

import de.umass.lastfm.Session
import de.umass.lastfm.Track
import de.umass.lastfm.scrobble.ScrobbleData
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URL

class MaiTest {

    @Test
    fun testParseTitle() {
        val title =
                "Lauren Aquilina | Sinners - Official MUsic Video (Download 'Sinners' EP on iTunes now!) "
//                 "[MV] REOL - ちるちる HQ / ChiruChiru HD"
//                "REOL -「mede:mede」 "
//                "Sia - Cheap Thrills Ft. Sean Paul (Remix)"
//                "【kradness×reol】Jitter Doll"
//                "kradness - 零の位相 [Official Music Video]"
//                "Lindsey Stirling Feat. Becky G - Christmas c' mon (official audio) .avi"
//                "INNA - Tropical | Lyric Video"
        val splits = Stuff.sanitizeTitle(title)
        splits.forEach { print(it + ", ") }
        println()
//        assertEquals(8, 8)
    }

    @Test
    fun testSanitizeAlbum() {
        val txt = "op.i:"
        val matches = txt.matches(".*\\w+\\.\\w+.*".toRegex())
        println("regex: " + matches)
        val url = URL(txt)
        println(url.host)
    }

    @Test
    fun testScrobble() {
        val key = ""
        val session = Session.createSession(Stuff.LAST_KEY, Stuff.LAST_SECRET, key)

        val scrobbleData = ScrobbleData()
        scrobbleData.artist = "Ben Macklin & Yota"
//        scrobbleData.album = "Unknown"
        scrobbleData.track = "Controller (Cassette Club Night Remix)"
        scrobbleData.timestamp = (System.currentTimeMillis()/1000).toInt() // in secs
        scrobbleData.duration = 0 // in secs

        val scrobbleResult = Track.updateNowPlaying(scrobbleData, session)
        println(scrobbleResult)
    }



}