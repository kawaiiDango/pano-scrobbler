package com.arn.scrobble

/**
 * Created by arn on 20/09/2017.
 */

import com.arn.scrobble.utils.MetadataUtils
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertContentEquals

class MaiTest {
    val testTitles = arrayOf(
        "Lauren Aquilina | Sinners - Official MUsic Video (Download 'Sinners' EP on iTunes now!) ",//
        "[MV] REOL - ちるちる HQ / ChiruChiru HD",
        "REOL -「mede:mede」 ",
        "Sia - Cheap Thrills Ft. Sean Paul (Remix)",
        "【kradness×reol】Jitter Doll",
        "kradness - 零の位相 [Official Music Video]",
        "Lindsey Stirling Feat. Becky G - Christmas c' mon (official audio) .avi",
        "INNA - Tropical | Lyric Video",
        "Klaypex - Stars (feat. Sara Kay)",

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

    @Serializable
    data class YoutubeArtistTrackData(
        val description: String,
        val args: List<String?>,
        val expected: Expected,
    )

    @Serializable
    data class Expected(
        val artist: String?,
        val track: String?,
    )

    @Serializable
    data class YoutubeTrackData(
        val description: String,
        val funcParameter: String,
        val expectedValue: String,
    )

    val json = Json { ignoreUnknownKeys = true }

    @Test
    fun parseYoutubeTitles() {
        val youtubeTitlesStream =
            javaClass.classLoader!!.getResourceAsStream("youtubeArtistTracks.json")!!

        val got = mutableListOf<Pair<String?, String?>>()
        val expected = mutableListOf<Pair<String?, String?>>()

        val youtubeTitles = json.decodeFromStream<List<YoutubeArtistTrackData>>(youtubeTitlesStream)
        youtubeTitles.forEachIndexed { i, it ->
            print("${it.args[0]} -> ")
            val then = System.currentTimeMillis()
            val (artist, track) = MetadataUtils.parseYoutubeTitle(it.args[0] ?: "")
            val now = System.currentTimeMillis()
            println("$artist - $track in ${now - then}ms")

            got += artist to track
            expected += it.expected.artist to it.expected.track
        }

        assertContentEquals(expected, got)
    }

    @Test
    fun cleanYoutubeTracks() {
        val youtubeTracksStream =
            javaClass.classLoader!!.getResourceAsStream("youtubeTracks.json")!!

        val got = mutableListOf<String?>()
        val expected = mutableListOf<String?>()

        val youtubeTitles = json.decodeFromStream<List<YoutubeTrackData>>(youtubeTracksStream)
        youtubeTitles.forEachIndexed { i, it ->
            print("${it.funcParameter} -> ")
            val then = System.currentTimeMillis()
            val track = MetadataUtils.cleanYoutubeTrack(it.funcParameter)
            val now = System.currentTimeMillis()
            println("$track in ${now - then}ms")

            got += track
            expected += it.expectedValue
        }

        assertContentEquals(expected, got)
    }


    @Test
    fun parseTitle() {

        testTitles.forEachIndexed { i, it ->
            print("$i-> ")
            val then = System.currentTimeMillis()
            val (artist, track) = MetadataUtils.parseYoutubeTitle(it)
            val now = System.currentTimeMillis()
            println("$artist - $track in ${now - then}ms")
        }
    }

    @Test
    fun parseTitleSingle() {
        val title = "【東方ヴォーカルPV】Let’s Ghost【暁Records公式】"
        val (artist, track) = MetadataUtils.parseYoutubeTitle(title)
        println("$artist - $track")
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

}