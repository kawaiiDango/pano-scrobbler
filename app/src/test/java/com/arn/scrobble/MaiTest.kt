package com.arn.scrobble

/**
 * Created by arn on 20/09/2017.
 */

import org.junit.Test
import java.net.URL

class MaiTest {

    @Test
    fun testParseTitle() {
        val title = arrayOf(

                "Lauren Aquilina | Sinners - Official MUsic Video (Download 'Sinners' EP on iTunes now!) ",//
                "[MV] REOL - ちるちる HQ / ChiruChiru HD",
                "REOL -「mede:mede」 ",
                "Sia - Cheap Thrills Ft. Sean Paul (Remix)",
                "【kradness×reol】Jitter Doll",
                "kradness - 零の位相 [Official Music Video]",
                "Lindsey Stirling Feat. Becky G - Christmas c' mon (official audio) .avi",
                "INNA - Tropical | Lyric Video",

                "【東方Jazz／Chillout】 Trip To Mourning 「C-CLAYS」",//
                "【MIX300曲】世界中のパリピをブチアゲた洋楽たち大集結！Mushup Remix BGM 2018 #2",
                "【東方Piano／Traditional】 Resentment 「流派未階堂／流派華劇団」",//
                "[Future Core] Srav3R feat. shully - Hereafter",
                "BEATLESS OP/Opening Full「Error - GARNiDELiA」cover by Kami",//
                "RΞOL - New type Tokyo (ニュータイプトーキョー) 「 Reol - Endless EP 」",
                "[東方Vocal]Resolution[Poplica]",//
                "【東方ボーカル】 「Resolution」 【 Poplica＊】",
                "【東方ボーカル】 「背徳のAgape」 【幽閉サテライト】",
                "【東方ボーカル】「幽閉サテライト」 - 背徳のAgape",//
                "【東方ボーカル】 「Please kiss my love」 【Syrufit】",
                "【東方Vocalアレンジ】 Syrufit - Please kiss my love",

                "[MV] 이달의 소녀/츄 (LOONA/Chuu) \"Heart Attack\"",
                "【macaroom | Halozy】Song of an Anxious Galley【Subbed】",
                "[Electro] - Au5 & Fractal - Smoke [Secret Weapon EP]",
                "M|O|O|N - M|O|O|N",
                "【NORISTRY】シニカルナイトプラン【歌ってみた】"

        )
        title.forEachIndexed { i, it ->
            print("\n $i-> ")
            val then = System.currentTimeMillis()
            val splits = MetadataUtils.sanitizeTitle(it)
            val now = System.currentTimeMillis()
           print(" ("+(now-then)+") ")
            splits.forEach { print("$it, ") }
        }
    }

    @Test
    fun testSanitizeAlbum() {
        val txt = "http://99.9%"
        val matches = txt.matches(".*\\w+\\.[\\w]{2,}".toRegex())
        println("regex: $matches")
        val url = URL(txt)
        println(url.host)
        println(MetadataUtils.sanitizeAlbum(txt))
    }

    @Test
    fun librefmArtistInfo(){
        val a = LFMRequester.getArtistInfoLibreFM("れをる/ギガP")
        println(a)
    }

    @Test
    fun spotifyArtistInfo(){
        val a = LFMRequester.getArtistInfoSpotify("MYTH & ROID")
        println(a)
    }

}