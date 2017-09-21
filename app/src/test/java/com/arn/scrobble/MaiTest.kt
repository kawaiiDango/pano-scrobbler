package com.arn.scrobble

/**
 * Created by arn on 20/09/2017.
 */

import org.junit.Assert.assertEquals
import org.junit.Test

class MaiTest {

    @Test
    fun testParseTitle() {
        val title = "Lauren Aquilina | Sinners - Official Music Video (Download 'Sinners' EP on iTunes now!) "
//        val title = "[MV] REOL - ちるちる / ChiruChiru"
        val splits = Stuff.sanitizeTitle(title)
        splits.forEach { print(it + ", ") }
//        assertEquals(8, 8)
    }


}