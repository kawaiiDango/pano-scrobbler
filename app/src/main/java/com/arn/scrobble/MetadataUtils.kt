package com.arn.scrobble

import java.util.Locale


object MetadataUtils {
    // in lower case
    private val metaUnknown = arrayOf(
        "unknown",
        "[unknown]",
        "<unknown>",
        "unknown album",
        "[unknown album]",
        "<unknown album>",
        "unknown artist",
        "[unknown artist]",
        "<unknown artist>",
    )
    private val artistSpam = arrayOf("va")

    // spotify:artist:0LyfQWJT6nXafLPZqxe9Of
    private val variousArtists = arrayOf(
        "Various Artists",
        "Verschiedene Interpreten",
        "Varios Artistas",
        "Hainbat Artista",
        "Multi-interprètes",
        "Artisti Vari",
        "Vários intérpretes",
        "Blandade Artister",
        "Çeşitli Sanatçılar",
        "אמנים שונים",
        "Разные исполнители",
        "فنانون متنوعون",
        "รวมศิลปิน",
        "群星",
        "ヴァリアス・アーティスト",
    ).map { it.lowercase() }.toSet()

    // from WebScrobbler https://github.com/web-scrobbler/web-scrobbler

    private val defaultSeparators = listOf(
        " -- ",
        "--",
        " ~ ",
        " \u002d ",
        " \u2013 ",
        " \u2014 ",
        " // ",
        "\u002d",
        "\u2013",
        "\u2014",
        ":",
        "|",
        "///",
        "/",
        "~",
    )

    private data class YoutubeTitleRegex(
        val pattern: String,
        val artistGroup: Int,
        val trackGroup: Int,
    )

    private data class Separator(val index: Int, val length: Int)
    private data class FilterRule(
        val source: String,
        val target: String,
        val caseSensitive: Boolean = true,
        val global: Boolean = false
    )

    private val ytTitleRegExps by lazy {
        listOf(
            // Artist "Track", Artist: "Track", Artist - "Track", etc.
            YoutubeTitleRegex("(.+?)([\\s:—-])+\\s*\"(.+?)\"", 1, 3),
            // Artist「Track」 (Japanese tracks)
            YoutubeTitleRegex("(.+?)[『｢「](.+?)[」｣』]", 1, 2),
            // Track (... by Artist)
            YoutubeTitleRegex("(\\w[\\s\\w]*?)\\s+\\([^)]*\\s*by\\s*([^)]+)+\\)", 2, 1),
        )
    }

    /**
     * Special filter rules to remove leftovers after filtering text using
     * `YOUTUBE_TRACK_FILTER_RULES` filter rules.
     */

    private val trimSymbolsFilterRules = listOf(
        // Leftovers after e.g. (official video)
        FilterRule("\\(+\\s*\\)+", ""),
        // trim starting white chars and dash
        FilterRule("^[/,:;~\\s\"-]+", ""),
        // trim trailing white chars and dash
        FilterRule("[/,:;~\\s\"-]+$", ""),
        // remove multiple spaces
        FilterRule("\\u0020{1,}", " ")
    )

    private val youtubePreprocessFilters = listOf(
        // Remove [genre] or 【genre】 from the beginning of the title
        FilterRule("^((\\[[^\\]]+])|(【[^】]+】))\\s*-*\\s*", "", false),
        // Remove track (CD and vinyl) numbers from the beginning of the title
        FilterRule("^\\s*([a-zA-Z]{1,2}|[0-9]{1,2})[1-9]?\\.\\s+", "", false),
        // Remove - preceding opening bracket
        FilterRule("-\\s*([「【『])", "$1"),
        // 【/(*Music Video/MV/PV*】/)
        FilterRule("[(［【][^(［【]*?((Music Video)|(MV)|(PV)).*?[】］)]", "", false),
        // 【/(東方/オリジナル*】/)
        FilterRule("[(［【]((オリジナル)|(東方)).*?[】］)]+?", ""),
        // MV/PV if followed by an opening/closing bracket
        FilterRule("((?:Music Video)|MV|PV)([「［【『』】］」])", "$2", false),
        // MV/PV if ending and with whitespace in front
        FilterRule("\\s+(MV|PV)\$", "", false),
    )

    /**
     * Filter rules to remove YouTube suffixes and prefixes from a text.
     */
    private val youtubeTrackFilterRules = listOf(
        // Trim whitespaces
        FilterRule("^\\s+|\\s+$", "", global = true),
        // **NEW**
        FilterRule("\\*+\\s?\\S+\\s?\\*+$", ""),
        // [Whatever]
        FilterRule("\\[[^\\]]+]", ""),
        // 【Whatever】
        FilterRule("【[^】]+】", ""),
        // （Whatever）
        FilterRule("（[^）]+）", ""),
        // (Whatever Version)
        FilterRule("\\([^)]*version\\)$", "", false),
        // Video extensions
        FilterRule("\\.(avi|wmv|mpg|mpeg|flv)$", "", false),
        // (Lyrics Video)
        FilterRule("\\(.*lyrics?\\s*(video)?\\)", "", false),
        // ((Official)? (Track)? Stream)
        FilterRule("\\((of+icial\\s*)?(track\\s*)?stream\\)", "", false),
        // ((Official)? (Music)? Video|Audio)
        FilterRule("\\((of+icial\\s*)?(music\\s*)?(video|audio)\\)", "", false),
        // - (Official)? (Music)? Video|Audio
        FilterRule("-\\s(of+icial\\s*)?(music\\s*)?(video|audio)$", "", false),
        // ((Whatever)? Album Track)
        FilterRule("\\(.*Album\\sTrack\\)", "", false),
        // (Official)
        FilterRule("\\(\\s*of+icial\\s*\\)", "", false),
        // (1999)
        FilterRule("\\(\\s*[0-9]{4}\\s*\\)", "", false),
        // (HD) / (HQ)
        FilterRule("\\(\\s*(HD|HQ)\\s*\\)$", ""),
        // HD / HQ
        FilterRule("(HD|HQ)\\s?$", ""),
        // Video Clip Officiel / Video Clip Official
        FilterRule("(vid[\\u00E9e]o)?\\s?clip\\sof+ici[ae]l", "", false),
        // Offizielles
        FilterRule("of+iziel+es\\s*video", "", false),
        // Video Clip
        FilterRule("vid[\\u00E9e]o\\s?clip", "", false),
        // Clip
        FilterRule("\\sclip", "", false),
        // Full Album
        FilterRule("full\\s*album", "", false),
        // (Live)
        FilterRule("\\(live.*?\\)$", "", false),
        // | Something
        FilterRule("\\|.*$", "", false),
        // Artist - The new "Track title" featuring someone
        FilterRule("^(|.*\\s)\"(.{5,})\"(\\s.*|)$", "$2"),
        // 'Track title'
        FilterRule("^(|.*\\s)'(.{5,})'(\\s.*|)$", "$2"),
        // (*01/01/1999*)
        FilterRule("\\(.*[0-9]{1,2}\\/[0-9]{1,2}\\/[0-9]{2,4}.*\\)", "", false),
        // Sub Español
        FilterRule("sub\\s*español", "", false),
        // (Letra)
        FilterRule("\\s\\(Letra\\)", "", false),
        // (En vivo)
        FilterRule("\\s\\(En\\svivo\\)", "", false),
        // Sub Español
        FilterRule("sub\\s*español", "", false)
    )

    private val removeLtrRtlChars = listOf(
        FilterRule("\\u200e", "", global = true),
        FilterRule("\\u200f", "", global = true)
    )

    private val removeNumericPrefix = listOf(
        // `NN.` or `NN)`
        FilterRule("^\\d{1,2}[.)]\\s?", ""),
        /* `(NN).` Ref: https://www.youtube.com/watch?v=KyabZRQeQgk
                * NOTE Initial tracklist format is (NN)  dd:dd  Artist - Track
                * YouTube adds a dot symbol after the numeric prefix.
        */
        FilterRule("^\\(\\d{1,2}\\)\\.", "")
    )

    private fun String.applyFilter(filterRule: FilterRule): String {
        val regex = if (filterRule.caseSensitive)
            filterRule.source.toRegex()
        else
            filterRule.source.toRegex(RegexOption.IGNORE_CASE)
        return if (filterRule.global) {
            replace(regex, filterRule.target)
        } else {
            replaceFirst(regex, filterRule.target)
        }
    }

    /**
     * Split string to two ones using array of separators.
     * @param str - Any string
     * @param separators - Array of separators
     * @param swap - Swap values
     * @returns Array of strings splitted by separator
     */
    private fun splitString(
        str: String,
        separators: List<String> = defaultSeparators,
        swap: Boolean = false
    ): Pair<String?, String?> {
        var first: String? = null
        var second: String? = null

        if (str.isNotEmpty()) {
            val separator = findSeparator(str, separators)

            if (separator != null) {
                first = str.substring(0, separator.index)
                second = str.substring(separator.index + separator.length)

                if (swap) {
                    val temp = second
                    second = first
                    first = temp
                }
            }
        }

        return Pair(first, second)
    }

    /**
     * Find first occurence of possible separator in given string
     * and return separator's position and size in chars or null.
     * @param str - String containing separator
     * @param separators - Array of separators
     * @returns Object containing position and width of separator
     */

    private fun findSeparator(
        str: String,
        separators: List<String> = defaultSeparators
    ): Separator? {
        if (str.isEmpty()) {
            return null
        }

        separators.forEach { sep ->
            val index = str.indexOf(sep)
            if (index > -1) {
                return Separator(index, sep.length)
            }
        }

        return null
    }

    fun cleanYoutubeTrack(track: String): String? {
        var cleanedTrack: String? = track
        (youtubeTrackFilterRules + trimSymbolsFilterRules).forEach { rule ->
            cleanedTrack = cleanedTrack?.applyFilter(rule)
        }
        return cleanedTrack
    }


    // from WebScrobbler
    fun parseYoutubeTitle(videoTitle: String): Pair<String?, String?> {
        var artist: String? = null
        var track: String? = null

        fun isArtistTrackEmpty() = artist.isNullOrEmpty() || track.isNullOrEmpty()

        if (videoTitle.isEmpty()) {
            return Pair(artist, track)
        }

        var title = videoTitle

        // Preprocess title
        youtubePreprocessFilters.forEach { rule ->
            title = title.applyFilter(rule)
        }

        // Try to match one of the regexps
        for (regExp in ytTitleRegExps) {
            val artistTrack = regExp.pattern.toRegex().find(title)
            if (artistTrack != null) {
                artist = artistTrack.groupValues[regExp.artistGroup]
                track = artistTrack.groupValues[regExp.trackGroup]
                break
            }
        }

        // No match? Try splitting, then.
        if (isArtistTrackEmpty()) {
            val (splitArtist, splitTrack) = splitString(title)
            artist = splitArtist
            track = splitTrack
        }

        // No match? Check for 【】
        if (isArtistTrackEmpty()) {
            val artistTrack = Regex("(.+?)【(.+?)】").find(title)
            if (artistTrack != null) {
                artist = artistTrack.groupValues[1]
                track = artistTrack.groupValues[2]
            }
        }

        // clean up
        if (!isArtistTrackEmpty()) {
            track = cleanYoutubeTrack(track!!)

            (removeLtrRtlChars + removeNumericPrefix).forEach { rule ->
                artist = artist?.applyFilter(rule)
                track = track?.applyFilter(rule)
            }

        }

        if (isArtistTrackEmpty()) {
            track = title
        }

        return Pair(artist, track)
    }

    fun sanitizeAlbum(albumOrig: String): String {
        val albumLower = albumOrig.lowercase(Locale.ENGLISH)
        if (metaUnknown.any { albumLower == it })
            return ""

        return albumOrig
    }

    fun sanitizeArtist(artist: String): String {
        if (artist.lowercase(Locale.ENGLISH) in artistSpam)
            return ""
        val splits = artist.split("; ").filter { it.isNotBlank() }
        if (splits.isEmpty())
            return ""
        return splits[0]
    }

    fun sanitizeAlbumArtist(artistOrig: String): String {
        val artist = sanitizeAlbum(artistOrig)
        if (artist.lowercase(Locale.ENGLISH) in artistSpam)
            return ""
        return artist
    }

    fun scrobbleFromNotiExtractMeta(titleStr: String, formatStr: String): Pair<String, String>? {
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
                throw IllegalArgumentException("group size != 3")
            if (tpos > apos)
                g[1] to g[2]
            else
                g[2] to g[1]

        } catch (e: Exception) {
            print("err in $titleStr $formatStr")
            null
        }
    }

    fun isVariousArtists(artist: String) = artist.lowercase() in variousArtists
}