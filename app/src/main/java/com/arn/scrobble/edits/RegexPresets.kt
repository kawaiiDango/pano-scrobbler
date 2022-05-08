package com.arn.scrobble.edits

import android.content.Context
import androidx.annotation.StringRes
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.db.RegexEdit

object RegexPresets {
    private val presets = mapOf(
        "remastered_track" to (
                R.string.preset_remastered to
                        RegexEdit(
                            pattern = " [(\\[][^()\\[\\]]*?remastere?d?[^()\\[\\]]*[)\\]]| ([/-] )?([(\\[]?\\d+[)\\]]?)? ?remastere?d? ?(version)?([(\\[]?\\d+[)\\]]?)?",
                            replacement = "",
                            fields = setOf(NLService.B_TRACK, NLService.B_ALBUM),
                        )
                ),
        "explicit_track" to (
                R.string.preset_explicit to
                        RegexEdit(
                            pattern = " ([/-] )? ?explicit ?(.*?version)?| [(\\[][^()\\[\\]]*?explicit[^()\\[\\]]*[)\\]]",
                            replacement = "",
                            fields = setOf(NLService.B_TRACK, NLService.B_ALBUM),
                        )
                ),
        "album_ver_track" to (
                R.string.preset_album_version to
                        RegexEdit(
                            pattern = " ([/-] .*)? ?album version.*| [(\\[][^()\\[\\]]*?album version[^()\\[\\]]*[)\\]]",
                            replacement = "",
                            fields = setOf(NLService.B_TRACK),
                        )
                )
    )

    val presetKeys = presets.keys

    fun getString(context: Context, key: String): String {
        return context.getString(presets[key]?.first ?: R.string.not_found)
    }

    fun getPossiblePreset(regexEdit: RegexEdit) = presets[regexEdit.preset]?.second
        ?.copy(
            _id = regexEdit._id,
            order = regexEdit.order,
            preset = regexEdit.preset,
            caseSensitive = false,
            continueMatching = true,
        )
        ?: regexEdit
}