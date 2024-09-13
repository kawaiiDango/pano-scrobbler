package com.arn.scrobble.edits

import com.arn.scrobble.NLService
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R
import com.arn.scrobble.db.RegexEdit

object RegexPresets {
    private val presets = mapOf(
        "remastered_track" to (
                R.string.preset_remastered to
                        RegexEdit(
                            pattern = "^(.+) [(\\[\\/\\-][^()\\[\\]]*?re-?mastere?d?[^)\\[\\]]*?([)\\]\\-\\/]|\$)",
                            replacement = "$1",
                            fields = setOf(NLService.B_TRACK, NLService.B_ALBUM),
                        )
                ),
        "explicit_track" to (
                R.string.preset_explicit to
                        RegexEdit(
                            pattern = "^(.*) (- |\\(|\\[|\\/)(explicit|clean)( .*?version| edit(ed)?)?[\\)\\]]?\$",
                            replacement = "$1",
                            fields = setOf(NLService.B_TRACK, NLService.B_ALBUM),
                        )
                ),
        "album_ver_track" to (
                R.string.preset_album_version to
                        RegexEdit(
                            pattern = "^(.*) (- |\\(|\\[|\\/)(.+ )?album (.*?version|edit(ed)?).*?[\\)\\]]?\$",
                            replacement = "\$1",
                            fields = setOf(NLService.B_TRACK),
                        )
                ),
        "ep" to (
                R.string.preset_ep to
                        RegexEdit(
                            pattern = " (- )?E\\.?P\\.?$",
                            replacement = "",
                            fields = setOf(NLService.B_ALBUM),
                            caseSensitive = true
                        )
                ),
        "single" to (
                R.string.preset_single to
                        RegexEdit(
                            pattern = "^(.*) (- |\\(|\\[|\\/)single( version| edit(ed)?)?[\\)\\]]?\$",
                            replacement = "$1",
                            fields = setOf(NLService.B_TRACK, NLService.B_ALBUM),
                        )
                ),
    )

    val presetKeys = presets.keys

    fun getString(key: String): String {
        return PlatformStuff.application.getString(presets[key]?.first ?: R.string.not_found)
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