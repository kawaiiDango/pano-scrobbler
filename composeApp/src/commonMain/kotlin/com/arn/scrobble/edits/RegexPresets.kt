package com.arn.scrobble.edits

import androidx.compose.runtime.Composable
import com.arn.scrobble.db.RegexEdit
import com.arn.scrobble.db.RegexEditFields
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.not_found
import pano_scrobbler.composeapp.generated.resources.preset_album_version
import pano_scrobbler.composeapp.generated.resources.preset_ep
import pano_scrobbler.composeapp.generated.resources.preset_explicit
import pano_scrobbler.composeapp.generated.resources.preset_remastered
import pano_scrobbler.composeapp.generated.resources.preset_single

object RegexPresets {
    private val presets = mapOf(
        "remastered_track" to (
                Res.string.preset_remastered to
                        RegexEdit(
                            pattern = "^(.+) [(\\[\\/\\-][^()\\[\\]]*?re-?mastere?d?[^)\\[\\]]*?([)\\]\\-\\/]|\$)",
                            replacement = "$1",
                            fields = setOf(RegexEditFields.TRACK, RegexEditFields.ALBUM),
                        )
                ),
        "explicit_track" to (
                Res.string.preset_explicit to
                        RegexEdit(
                            pattern = "^(.*) (- |\\(|\\[|\\/)(explicit|clean)( .*?version| edit(ed)?)?[\\)\\]]?\$",
                            replacement = "$1",
                            fields = setOf(RegexEditFields.TRACK, RegexEditFields.ALBUM),
                        )
                ),
        "album_ver_track" to (
                Res.string.preset_album_version to
                        RegexEdit(
                            pattern = "^(.*) (- |\\(|\\[|\\/)(.+ )?album (.*?version|edit(ed)?).*?[\\)\\]]?\$",
                            replacement = "\$1",
                            fields = setOf(RegexEditFields.TRACK),
                        )
                ),
        "ep" to (
                Res.string.preset_ep to
                        RegexEdit(
                            pattern = " (- )?E\\.?P\\.?$",
                            replacement = "",
                            fields = setOf(RegexEditFields.ALBUM),
                            caseSensitive = true
                        )
                ),
        "single" to (
                Res.string.preset_single to
                        RegexEdit(
                            pattern = "^(.*) (- |\\(|\\[|\\/)single( version| edit(ed)?)?[\\)\\]]?\$",
                            replacement = "$1",
                            fields = setOf(RegexEditFields.TRACK, RegexEditFields.ALBUM),
                        )
                ),
    )

    val presetKeys = presets.keys

    @Composable
    fun getString(key: String): String {
        return stringResource(presets[key]?.first ?: Res.string.not_found)
    }

    fun getPossiblePreset(regexEdit: RegexEdit) = presets[regexEdit.preset]?.second
        ?.copy(
            _id = regexEdit._id,
            order = regexEdit.order,
            preset = regexEdit.preset,
            caseSensitive = regexEdit.caseSensitive,
            continueMatching = true,
        )
        ?: regexEdit
}