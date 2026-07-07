package com.arn.scrobble.edits

import androidx.compose.runtime.Composable
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.db.RegexEdit
import com.arn.scrobble.db.RegexEditsDao.Companion.performRegexReplace
import com.arn.scrobble.utils.MetadataUtils
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.preset_album_version
import pano_scrobbler.composeapp.generated.resources.preset_explicit
import pano_scrobbler.composeapp.generated.resources.preset_remastered
import pano_scrobbler.composeapp.generated.resources.preset_single_ep
import pano_scrobbler.composeapp.generated.resources.preset_title_parse
import pano_scrobbler.composeapp.generated.resources.preset_title_parse_with_fallback
import pano_scrobbler.composeapp.generated.resources.remove

enum class RegexPreset {
    parse_title,
    parse_title_with_fallback,
    remastered,
    single_ep,
    explicit,
    album_ver,
//    album_artist_as_artist,
}

data class RegexPresetsResult(
    val scrobbleData: ScrobbleData,
    val appliedPresets: List<RegexPreset>,
)

class TitleParseException : IllegalStateException("Title parsing failed")

object RegexPresets {

    val defaultPresets = listOf(
        RegexPreset.parse_title,
        RegexPreset.parse_title_with_fallback,
        RegexPreset.remastered,
        RegexPreset.explicit,
        RegexPreset.single_ep,
    )

    // app id settings are currently ignored on linux, uses domain name instead
    val hasSettings = if (!PlatformStuff.isDesktop)
        listOf(
            RegexPreset.parse_title,
            RegexPreset.parse_title_with_fallback,
        )
    else
        emptyList()

    private val applyOncePresets = listOf(
        RegexPreset.parse_title,
        RegexPreset.parse_title_with_fallback,
    )

    val filteredPresets by lazy {
        RegexPreset.entries.filterNot {
            if (System.getProperty("os.name").startsWith("windows", ignoreCase = true))
                it in applyOncePresets
            else
                false
        }
    }

    @Throws(TitleParseException::class)
    suspend fun applyAllPresets(
        scrobbleData: ScrobbleData,
        normalizedUrlHost: String?,
        dataIsEdited: Boolean
    ): RegexPresetsResult? {
        var newScrobbleData: ScrobbleData? = null
        val appliedPresets = mutableListOf<RegexPreset>()

        val presetNames = PlatformStuff.mainPrefs.data.map { it.regexPresets }.first()

        filteredPresets
            .filter { it.name in presetNames }
            .filter { dataIsEdited && it !in applyOncePresets || !dataIsEdited }
            .forEach { preset ->
                val sd = applyPreset(
                    newScrobbleData ?: scrobbleData,
                    normalizedUrlHost,
                    preset,
                    PlatformStuff.mainPrefs.data.map { it.getRegexPresetApps(preset) }.first()
                )

                if (sd != null) {
                    appliedPresets.add(preset)
                    newScrobbleData = sd
                }
            }

        return newScrobbleData?.let {
            RegexPresetsResult(
                scrobbleData = it,
                appliedPresets = appliedPresets,
            )
        }

    }

    private fun removeEdgeSymbol(input: String, symbol: String): String? {
        val first = input.indexOf(symbol)
        val last = input.lastIndexOf(symbol)

        return when {
            first == -1 || first != last -> null // none, or more than one

            first == 0 && input.length > symbol.length &&
                    input[symbol.length].isWhitespace() ->
                input.removeRange(0, symbol.length)

            first == input.length - symbol.length && first > 0 &&
                    input[first - 1].isWhitespace() ->
                input.removeRange(first, input.length)

            else -> null
        }
    }

    private fun applyPreset(
        scrobbleData: ScrobbleData,
        normalizedUrlHost: String?,
        regexPreset: RegexPreset,
        regexPresetsApps: Set<String>,
    ): ScrobbleData? {
        val regexEdits = mutableListOf<RegexEdit>()
        var newScrobbleData: ScrobbleData? = null

        when (regexPreset) {
//            RegexPreset.album_artist_as_artist -> {
//                if (scrobbleData.appId == Stuff.PACKAGE_SPOTIFY &&
//                    !scrobbleData.albumArtist.isNullOrEmpty() &&
//                    scrobbleData.albumArtist != scrobbleData.artist &&
//                    !MetadataUtils.isVariousArtists(scrobbleData.albumArtist)
//                ) {
//                    newScrobbleData = scrobbleData.copy(
//                        artist = scrobbleData.albumArtist,
//                        albumArtist = null,
//                    )
//                }
//            }

            RegexPreset.parse_title,
            RegexPreset.parse_title_with_fallback -> {
                if (scrobbleData.appId in regexPresetsApps ||
                    regexPreset == RegexPreset.parse_title &&
                    normalizedUrlHost == Stuff.HOST_YOUTUBE ||
                    regexPreset == RegexPreset.parse_title_with_fallback &&
                    normalizedUrlHost == Stuff.HOST_YOUTUBE_MUSIC
                ) {
                    val shouldParseTitle = (scrobbleData.album.isNullOrEmpty() &&
                            !scrobbleData.artist.endsWith("- Topic")) ||
                            scrobbleData.appId == Stuff.PACKAGE_YMUSIC && scrobbleData.album == "YMusic"

                    if (shouldParseTitle) {
                        val (parsedArtist, parsedTitle) =
                            MetadataUtils.parseYoutubeTitle(scrobbleData.track)
                        if (!parsedArtist.isNullOrEmpty() && !parsedTitle.isNullOrEmpty()) {
                            newScrobbleData = scrobbleData.copy(
                                artist = parsedArtist,
                                track = parsedTitle,
                                album = null,
                                albumArtist = null,
                            )
                        } else if (regexPreset == RegexPreset.parse_title) {
                            // no fallback
                            throw TitleParseException()
                        }
                    } else if (scrobbleData.artist.endsWith("- Topic")) {
                        // remove "- Topic" suffix from artist
                        newScrobbleData = scrobbleData.copy(
                            artist = scrobbleData.artist.removeSuffix("- Topic"),
                            albumArtist = scrobbleData.albumArtist?.removeSuffix("- Topic")
                        )
                    }
                }
            }

            RegexPreset.remastered -> {
                val pattern =
                    "^(.+) [(\\[\\/\\-][^()\\[\\]]*?re-?mastere?d?[^)\\[\\]]*?([)\\]\\-\\/]|\$)"
                val replacement = "$1"

                regexEdits += RegexEdit(
                    name = regexPreset.name,
                    search = RegexEdit.SearchPatterns(
                        searchTrack = pattern,
                        searchAlbum = "",
                        searchArtist = "",
                        searchAlbumArtist = "",
                    ),
                    replacement = RegexEdit.ReplacementPatterns(
                        replacementTrack = replacement,
                        replacementAlbum = "",
                        replacementArtist = "",
                        replacementAlbumArtist = "",
                    ),
                )

                regexEdits += RegexEdit(
                    name = regexPreset.name,
                    search = RegexEdit.SearchPatterns(
                        searchTrack = "",
                        searchAlbum = pattern,
                        searchArtist = "",
                        searchAlbumArtist = "",
                    ),
                    replacement = RegexEdit.ReplacementPatterns(
                        replacementTrack = "",
                        replacementAlbum = replacement,
                        replacementArtist = "",
                        replacementAlbumArtist = "",
                    ),
                )
            }

            RegexPreset.single_ep -> {
                val pattern = " - (Single|EP)$"
                val replacement = ""
                val isAppleMusic = scrobbleData.appId == Stuff.PACKAGE_APPLE_MUSIC ||
                        scrobbleData.appId == Stuff.PACKAGE_APPLE_MUSIC_WIN_EXE ||
                        scrobbleData.appId.equals(
                            Stuff.PACKAGE_APPLE_MUSIC_WIN_STORE,
                            ignoreCase = true
                        ) ||
                        scrobbleData.appId == Stuff.PACKAGE_ITUNES_SMTC_WIN_EXE ||
                        scrobbleData.appId.equals(
                            Stuff.PACKAGE_ITUNES_SMTC_WIN_STORE,
                            ignoreCase = true
                        ) ||
                        normalizedUrlHost == Stuff.HOST_APPLE_MUSIC

                if (isAppleMusic) {
                    regexEdits += RegexEdit(
                        name = regexPreset.name,
                        search = RegexEdit.SearchPatterns(
                            searchTrack = "",
                            searchAlbum = pattern,
                            searchArtist = "",
                            searchAlbumArtist = "",
                        ),
                        replacement = RegexEdit.ReplacementPatterns(
                            replacementTrack = "",
                            replacementAlbum = replacement,
                            replacementArtist = "",
                            replacementAlbumArtist = "",
                        )
                    )
                }
            }

            RegexPreset.explicit -> {
                val pattern =
                    "^(.*) (- |\\(|\\[|\\/)(explicit|clean)( .*?version| edit(ed)?)?[\\)\\]]?\$"
                val replacement = "$1"

                regexEdits += RegexEdit(
                    name = regexPreset.name,
                    search = RegexEdit.SearchPatterns(
                        searchTrack = pattern,
                        searchAlbum = "",
                        searchArtist = "",
                        searchAlbumArtist = "",
                    ),
                    replacement = RegexEdit.ReplacementPatterns(
                        replacementTrack = replacement,
                        replacementAlbum = "",
                        replacementArtist = "",
                        replacementAlbumArtist = "",
                    ),
                )

                regexEdits += RegexEdit(
                    name = regexPreset.name,
                    search = RegexEdit.SearchPatterns(
                        searchTrack = "",
                        searchAlbum = pattern,
                        searchArtist = "",
                        searchAlbumArtist = "",
                    ),
                    replacement = RegexEdit.ReplacementPatterns(
                        replacementTrack = "",
                        replacementAlbum = replacement,
                        replacementArtist = "",
                        replacementAlbumArtist = "",
                    ),
                )

            }

            RegexPreset.album_ver -> {
                val pattern =
                    "^(.*) (- |\\(|\\[|\\/)(.+ )?album (.*?version|edit(ed)?).*?[\\)\\]]?\$"
                val replacement = "$1"

                regexEdits += RegexEdit(
                    name = regexPreset.name,
                    search = RegexEdit.SearchPatterns(
                        searchTrack = pattern,
                        searchAlbum = "",
                        searchArtist = "",
                        searchAlbumArtist = "",
                    ),
                    replacement = RegexEdit.ReplacementPatterns(
                        replacementTrack = replacement,
                        replacementAlbum = "",
                        replacementArtist = "",
                        replacementAlbumArtist = "",
                    ),
                )

            }
        }

        if (regexEdits.isNotEmpty()) {
            newScrobbleData = performRegexReplace(scrobbleData, regexEdits).scrobbleData
        }

        if (regexPreset == RegexPreset.explicit) {
            val sd = newScrobbleData ?: scrobbleData

            val track = removeEdgeSymbol(sd.track, "🅴")
            val album = sd.album?.let { removeEdgeSymbol(it, "🅴") }

            if (track != null || album != null) {
                newScrobbleData = sd.copy(
                    track = track ?: sd.track,
                    album = album ?: sd.album,
                )
            }
        }

        return newScrobbleData
    }

    @Composable
    fun getString(regexPreset: RegexPreset) = when (regexPreset) {
        RegexPreset.remastered -> stringResource(
            Res.string.remove,
            stringResource(Res.string.preset_remastered)
        )

        RegexPreset.explicit -> stringResource(
            Res.string.remove,
            stringResource(Res.string.preset_explicit)
        )

        RegexPreset.album_ver -> stringResource(
            Res.string.remove,
            stringResource(Res.string.preset_album_version)
        )

        RegexPreset.single_ep -> stringResource(
            Res.string.remove,
            stringResource(Res.string.preset_single_ep)
        )
//        RegexPreset.album_artist_as_artist -> stringResource(Res.string.preset_album_artist_as_artist)
        RegexPreset.parse_title -> stringResource(Res.string.preset_title_parse)
        RegexPreset.parse_title_with_fallback -> stringResource(Res.string.preset_title_parse_with_fallback)
    }
}