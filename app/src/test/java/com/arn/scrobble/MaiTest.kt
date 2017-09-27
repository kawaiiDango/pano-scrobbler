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
        val title = "Lauren Aquilina | Sinners - Official Music Video (Download 'Sinners' EP on iTunes now!) "
//        val title = "[MV] REOL - ちるちる / ChiruChiru"
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