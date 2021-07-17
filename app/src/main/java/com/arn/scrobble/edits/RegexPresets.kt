package com.arn.scrobble.edits

import android.content.Context
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.db.RegexEdit

object RegexPresets {
    private val presets = mapOf(
        "remastered_track" to (
                R.string.preset_remastered_track to
                    RegexEdit(
                        pattern = "([/-] )?([(\\[]?\\d+[)\\]]?)? ?remastere?d? ?(version)?([(\\[]?\\d+[)\\]]?)?| [(\\[].*remastere?d?.*[)\\]]",
                        replacement = "",
                        field = NLService.B_TRACK,
                    )
                ),
        "remastered_album" to (
                R.string.preset_remastered_album to
                        RegexEdit(
                        pattern = "([/-] )?([(\\[]?\\d+[)\\]]?)? ?remastere?d? ?(version)?([(\\[]?\\d+[)\\]]?)?| [(\\[].*remastere?d?.*[)\\]]",
                        replacement = "",
                        field = NLService.B_ALBUM,
                    )
                ),
        "explicit_track" to (
                R.string.preset_explicit_track to
                        RegexEdit(
                        pattern = " ([/-] )? ?explicit ?(.*?version)?| [(\\[].*?explicit.*?[)\\]]",
                        replacement = "",
                        field = NLService.B_TRACK,
                    )
                ),
        "explicit_album" to (
                R.string.preset_explicit_album to
                        RegexEdit(
                        pattern = " ([/-] )? ?explicit ?(.*?version)?| [(\\[].*?explicit.*?[)\\]]",
                        replacement = "",
                        field = NLService.B_ALBUM,
                    )
                ),
        "album_ver_track" to (
                R.string.preset_album_ver_track to
                        RegexEdit(
                        pattern = " ([/-] .*)? ?album version.*| [(\\[].*?album version.*?[)\\]]",
                        replacement = "",
                        field = NLService.B_TRACK,
                    )
                )
    )

    val presetKeys = presets.keys

    fun getString(context: Context, key: String) = context.getString(presets[key]?.first ?: R.string.edit_presets)

    fun getPossiblePreset(regexEdit: RegexEdit) = presets[regexEdit.preset]?.second
        ?.apply {
            _id = regexEdit._id
            order = regexEdit.order
            preset = regexEdit.preset
            caseSensitive = false
            continueMatching = true
        }
            ?: regexEdit
}