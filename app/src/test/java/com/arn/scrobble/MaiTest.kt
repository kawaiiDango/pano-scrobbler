package com.arn.scrobble

/**
 * Created by arn on 20/09/2017.
 */

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URL

class MaiTest {

    @Test
    fun testParseTitle() {
        val title =
//                "Lauren Aquilina | Sinners - Official MUsic Video (Download 'Sinners' EP on iTunes now!) "
//                 "[MV] REOL - ちるちる HQ / ChiruChiru HD"
//                "REOL -「mede:mede」 "
//                "Sia - Cheap Thrills Ft. Sean Paul (Remix)"
                "【kradness×reol】Jitter Doll"
//                "kradness - 零の位相 [Official Music Video]"
//                "Lindsey Stirling Feat. Becky G - Christmas c' mon (official audio) .avi"
        val splits = Stuff.sanitizeTitle(title)
        splits.forEach { print(it + ", ") }
        println()
//        assertEquals(8, 8)
    }

    @Test
    fun testSanitizeAlbum() {
        val txt = "http://pop/tmpe"
        val url = URL(txt)
        println(url.host)
    }



}