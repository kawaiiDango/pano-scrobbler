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
                        pattern = " ([/-] )?([(\\[]?\\d+[)\\]]?)? ?remastere?d? ?(version)?([(\\[]?\\d+[)\\]]?)?| [(\\[][^()\\[\\]]*?remastere?d?[^()\\[\\]]*[)\\]]",
                        replacement = "",
                        field = NLService.B_TRACK,
                    )
                ),
        "remastered_album" to (
                R.string.preset_remastered to
                        RegexEdit(
                        pattern = " ([/-] )?([(\\[]?\\d+[)\\]]?)? ?remastere?d? ?(version)?([(\\[]?\\d+[)\\]]?)?| [(\\[][^()\\[\\]]*?remastere?d?[^()\\[\\]]*[)\\]]",
                        replacement = "",
                        field = NLService.B_ALBUM,
                    )
                ),
        "explicit_track" to (
                R.string.preset_explicit to
                        RegexEdit(
                        pattern = " ([/-] )? ?explicit ?(.*?version)?| [(\\[][^()\\[\\]]*?explicit[^()\\[\\]]*[)\\]]",
                        replacement = "",
                        field = NLService.B_TRACK,
                    )
                ),
        "explicit_album" to (
                R.string.preset_explicit to
                        RegexEdit(
                        pattern = " ([/-] )? ?explicit ?(.*?version)?| [(\\[][^()\\[\\]]*?explicit[^()\\[\\]]*[)\\]]",
                        replacement = "",
                        field = NLService.B_ALBUM,
                    )
                ),
        "album_ver_track" to (
                R.string.preset_album_version to
                        RegexEdit(
                        pattern = " ([/-] .*)? ?album version.*| [(\\[][^()\\[\\]]*?album version[^()\\[\\]]*[)\\]]",
                        replacement = "",
                        field = NLService.B_TRACK,
                    )
                )
    )

    val presetKeys = presets.keys

    fun getString(context: Context, key: String): String {
        @StringRes
        val fieldRes = when (presets[key]?.second?.field) {
            NLService.B_ARTIST -> R.string.artist
            NLService.B_ALBUM -> R.string.album
            NLService.B_TRACK -> R.string.track
            NLService.B_ALBUM_ARTIST -> R.string.album_artist

            else -> return "invalid field"
        }

        return context.getString(presets[key]?.first ?: R.string.edit_presets) +
                " (${context.getString(fieldRes).lowercase()})"
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