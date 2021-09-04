package com.arn.scrobble

import java.util.*


object MetadataUtils {

    private val seperators = arrayOf( // in priority order
            "—", " – ", " –", "– ", " _ ", " - ", " | ", " -", "- ", "「", "『", /*"ー", */" • ",

            "【", "〖", "〔",
            "】", "〗", "』", "」", "〕",
            // ":",
            " \"", " / ", "／")
    private val unwantedSeperators = arrayOf("『", "』", "「", "」", "\"", "'", "【", "】", "〖", "〗", "〔", "〕", "\\|")

    // in lower case
    private val metaSpam = arrayOf("downloaded", ".com", ".co.", "www.")
    private val metaUnknown = arrayOf("unknown", "[unknown]", "unknown album", "[unknown album]", "unknown artist", "[unknown artist]")
    private val albumArtistUnknown = arrayOf("va")

    fun parseArtistTitle(titleContentOriginal: String): Array<String> {
        //New detection of trackinformation
        //remove (*) and/or [*] to remove unimportant data
        val titleContent = titleContentOriginal.replace(" *\\([^)]*?\\) *".toRegex(), " ")
                .replace(" *\\[[^)]*?] *".toRegex(), " ")

                //remove HD info
                .replace("\\W* HD|HQ|4K|MV|M/V|Official Music Video|Music Video|Lyric Video|Official Audio( \\W*)?"
                        .toRegex(RegexOption.IGNORE_CASE)
                        , " ")

//        get remix info
        val remixInfo = "\\([^)]*(?:remix|mix|cover|version|edit|booty?leg)\\)".toRegex(RegexOption.IGNORE_CASE).find(titleContentOriginal)

        var musicInfo: Array<String>? = null
        for (s in seperators) {
            //parsing artist - title
            musicInfo = titleContent.split(s).filter { it.isNotBlank() }.toTypedArray()
            if (s == "／")
                musicInfo.reverse()

//            println("musicInfo= "+musicInfo[0] + (if (musicInfo.size >1) "," + musicInfo[1] else "") + "|" + musicInfo.size)
            //got artist, parsing title - audio (cover) [blah]
            if (musicInfo.size > 1) {
                for (j in 0 until seperators.size - 2) {
                    val splits = musicInfo[1].split(seperators[j]).filter { it.isNotEmpty() }
//                    println("splits= $splits |" + splits.size + "|" + seperators[j])
                    if (splits.size > 1) {
                        musicInfo[1] = splits[0]
                        break
                    }
//                    else if (splits.size == 1)
//                        break
                }
                break
            }
        }


        if (musicInfo == null || musicInfo.size < 2) {
            return arrayOf("", titleContent.trim())
        }

        //remove ", ', 」, 』 from musicInfo
        val allUnwantedSeperators = "(" + unwantedSeperators.joinToString("|") + ")"
        for (i in musicInfo.indices) {
            musicInfo[i] = musicInfo[i].replace("^\\s*$allUnwantedSeperators|$allUnwantedSeperators\\s*$".toRegex(), " ")
        }

        musicInfo[1] = musicInfo[1].replace("\\.(avi|wmv|mp4|mpeg4|mov|3gpp|flv|webm)$".toRegex(RegexOption.IGNORE_CASE), " ")
                .replace("Full Album".toRegex(RegexOption.IGNORE_CASE), "")
        //Full Album Video
//        println("mi1="+musicInfo[1])
        //move feat. info from artist to
        musicInfo[0] = musicInfo[0].replace(" (ft\\.?) ".toRegex(), " feat. ")
        if (musicInfo[0].contains(" feat.* .*".toRegex(RegexOption.IGNORE_CASE))) {
            val m = " feat.* .*".toRegex(RegexOption.IGNORE_CASE).find(musicInfo[0])
            musicInfo[1] = musicInfo[1].trim() + " " + (m!!.groups[0]?.value ?: "").trim()
            musicInfo[0] = musicInfo[0].replace(" feat.* .*".toRegex(RegexOption.IGNORE_CASE), "")
        }
//        println("mi2="+musicInfo[1])
        //add remix info
        if (remixInfo?.groups?.isNotEmpty() == true) {
            musicInfo[1] = musicInfo[1].trim() + " " + remixInfo.groups[0]?.value
        }

        //delete spaces
        musicInfo[0] = musicInfo[0].trim()
        musicInfo[1] = musicInfo[1].trim()

        return musicInfo
    }

    fun sanitizeAlbum(albumOrig: String): String {
        val albumLower = albumOrig.lowercase(Locale.ENGLISH)
        if (metaSpam.any { albumLower.contains(it) } || metaUnknown.any { albumLower == it })
            return ""

        return albumOrig
    }

    fun sanitizeArtist(artistOrig: String): String {
        val splits = artistOrig.split("; ").filter { it.isNotBlank() }
        if (splits.isEmpty())
            return ""
        return splits[0]
    }

    fun sanitizeAlbumArtist(artistOrig: String): String {
        val artist = sanitizeAlbum(artistOrig)
        if (artist.lowercase(Locale.ENGLISH) in albumArtistUnknown)
            return ""
        return artist
    }

    fun scrobbleFromNotiExtractMeta(titleStr: String, formatStr: String):Array<String>? {
        val tpos = formatStr.indexOf("%1\$s")
        val apos = formatStr.indexOf("%2\$s")
        val regex = formatStr.replace("(", "\\(")
                .replace(")", "\\)")
                .replace("%1\$s", "(.*)")
                .replace("%2\$s", "(.*)")
        return try {
            val m = regex.toRegex().find(titleStr)!!
            val g = m.groupValues
            if (g.size != 3)
                throw Exception("group size != 3")
            if (tpos > apos)
                arrayOf(g[1],g[2])
            else
                arrayOf(g[2],g[1])

        } catch (e:Exception) {
            print("err in $titleStr $formatStr")
            null
        }
    }
}