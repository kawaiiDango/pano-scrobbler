package com.arn.scrobble.edits

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.arn.scrobble.*
import com.arn.scrobble.billing.BillingFragment
import com.arn.scrobble.databinding.DialogBlockedTagsBinding
import com.arn.scrobble.databinding.TextInputEditBinding
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.pref.MainPrefs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class BlockedMetadataAddDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogBlockedTagsBinding.inflate(layoutInflater)
        val prefs = MainPrefs(context!!)

        val blockedMetadata =
            arguments?.getParcelable<BlockedMetadata>(Stuff.ARG_DATA)?.copy() ?: BlockedMetadata()

        val ignoredArtist = arguments?.getString(NLService.B_IGNORED_ARTIST)

        fun trimmedText(tib: TextInputEditBinding) = tib.edittext.text.toString().trim()

        val dialog = MaterialAlertDialogBuilder(context!!)
            .setView(binding.root)
            .setPositiveButton(R.string.block, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        binding.apply {
            blockArtist.root.hint = getString(R.string.artist_channel)
            blockAlbumArtist.root.hint = getString(R.string.album_artist)
            blockAlbum.root.hint = getString(R.string.album)
            blockTrack.root.hint = getString(R.string.track)

            blockArtist.edittext.setText(blockedMetadata.artist)
            blockAlbumArtist.edittext.setText(blockedMetadata.albumArtist)
            blockAlbum.edittext.setText(blockedMetadata.album)
            blockTrack.edittext.setText(blockedMetadata.track)

            arrayOf(blockArtist, blockAlbumArtist, blockAlbum, blockTrack).forEach {
                it.root.endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
                it.root.setEndIconDrawable(R.drawable.vd_cancel)
            }

            if (ignoredArtist != null) {
                useChannel.visibility = View.VISIBLE
                useChannel.setOnCheckedChangeListener { _, checked ->
                    blockArtist.edittext.setText(
                        if (checked)
                            ignoredArtist
                        else
                            blockedMetadata.artist
                    )
                }
            }
        }
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {

            if (!prefs.proStatus) {
                dialog.dismiss()
                if (activity is MainDialogActivity) {
                    val intent = Intent(context, MainActivity::class.java).apply {
                        putExtra(Stuff.DIRECT_OPEN_KEY, Stuff.DL_PRO)
                    }
                    context!!.startActivity(intent)
                } else if (parentFragment is BlockedMetadataFragment) {
                    parentFragment!!.parentFragmentManager
                        .beginTransaction()
                        .replace(R.id.frame, BillingFragment())
                        .addToBackStack(null)
                        .commit()
                }
                return@setOnClickListener
            }

            val prevBlockedTag = blockedMetadata.copy()
            blockedMetadata.apply {
                artist = trimmedText(binding.blockArtist)
                albumArtist = trimmedText(binding.blockAlbumArtist)
                album = trimmedText(binding.blockAlbum)
                track = trimmedText(binding.blockTrack)

                if (listOf(artist, albumArtist, album, track)
                        .all { it == "" }
                )
                    return@setOnClickListener
            }
            if (prevBlockedTag != blockedMetadata)
                GlobalScope.launch(Dispatchers.IO) {
                    PanoDb.getDb(activity!!.application).getBlockedMetadataDao()
                        .insertLowerCase(listOf(blockedMetadata), ignore = false)
                }

            dialog.dismiss()
        }
        return dialog
    }
}