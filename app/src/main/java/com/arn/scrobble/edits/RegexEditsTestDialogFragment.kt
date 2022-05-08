package com.arn.scrobble.edits

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.databinding.DialogRegexTestBinding
import com.arn.scrobble.db.PanoDb
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.umass.lastfm.scrobble.ScrobbleData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class RegexEditsTestDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogRegexTestBinding.inflate(layoutInflater)
        val mutex = Mutex()
        val dao = PanoDb.getDb(context!!).getRegexEditsDao()
        binding.matches.text = resources.getQuantityString(R.plurals.num_matches, 0, 0)
        binding.text.addTextChangedListener(object : TextWatcher {

            override fun onTextChanged(cs: CharSequence, arg1: Int, arg2: Int, arg3: Int) {
            }

            override fun beforeTextChanged(s: CharSequence, arg1: Int, arg2: Int, arg3: Int) {
            }

            override fun afterTextChanged(editable: Editable) {
                lifecycleScope.launch(Dispatchers.IO) {
                    mutex.withLock {
                        val text = editable.toString()
                        val sd = ScrobbleData()
                        sd.artist = text
                        sd.album = text
                        sd.albumArtist = text
                        sd.track = text
                        val numMatches = dao.performRegexReplace(sd)

                        withContext(Dispatchers.Main) {
                            if (numMatches.all { it.value == 0 })
                                binding.matches.text =
                                    resources.getQuantityString(R.plurals.num_matches, 0, 0)
                            else {
                                val lines = mutableListOf<String>()
                                numMatches.forEach { (field, count) ->
                                    if (count > 0) {
                                        val replacement = when (field) {
                                            NLService.B_ARTIST -> sd.artist
                                            NLService.B_ALBUM -> sd.album
                                            NLService.B_ALBUM_ARTIST -> sd.albumArtist
                                            NLService.B_TRACK -> sd.track
                                            else -> throw IllegalArgumentException()
                                        }
                                        lines += "<b>${getString(RegexEditsFragment.localizedFieldsMap[field]!!)}</b> " +
                                                "<i>(${
                                                    resources.getQuantityString(
                                                        R.plurals.num_matches,
                                                        count,
                                                        count
                                                    )
                                                }):</i>" +
                                                "<br/>$replacement"
                                    }
                                }
                                binding.matches.text = Html.fromHtml(lines.joinToString("<br/>"))
                            }
                        }
                    }
                }
            }

        })

        return MaterialAlertDialogBuilder(context!!)
            .setNegativeButton(R.string.close, null)
            .setView(binding.root)
            .create()
    }
}