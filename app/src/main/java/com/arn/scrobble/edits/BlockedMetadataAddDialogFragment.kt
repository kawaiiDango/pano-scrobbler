package com.arn.scrobble.edits

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.navigation.NavDeepLinkBuilder
import androidx.navigation.fragment.findNavController
import com.arn.scrobble.App
import com.arn.scrobble.MainActivity
import com.arn.scrobble.MainDialogActivity
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.Stuff.getSingle
import com.arn.scrobble.Stuff.putSingle
import com.arn.scrobble.databinding.DialogBlockedMetadataBinding
import com.arn.scrobble.databinding.TextInputEditBinding
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.db.BlockedMetadataDao.Companion.insertLowerCase
import com.arn.scrobble.db.PanoDb
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class BlockedMetadataAddDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogBlockedMetadataBinding.inflate(layoutInflater)
        val prefs = App.prefs

        val blockedMetadata =
            arguments?.getSingle<BlockedMetadata>()?.copy()
                ?: BlockedMetadata(skip = true)

        val ignoredArtist = arguments?.getString(NLService.B_IGNORED_ARTIST)

        fun trimmedText(tib: TextInputEditBinding) = tib.edittext.text.toString().trim()

        val dialog = MaterialAlertDialogBuilder(requireContext())
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
            skip.isChecked = blockedMetadata.skip
            mute.isChecked = blockedMetadata.mute

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

        dialog.setOnShowListener {

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {

                if (!prefs.proStatus) {
                    dialog.dismiss()
                    if (activity is MainDialogActivity) {
                        NavDeepLinkBuilder(requireContext())
                            .setComponentName(MainActivity::class.java)
                            .setGraph(R.navigation.nav_graph)
                            .setDestination(R.id.billingFragment)
                            .createPendingIntent()
                            .send()
                    } else if (parentFragment is BlockedMetadataFragment) {
                        findNavController().navigate(R.id.billingFragment)
                    }
                    return@setOnClickListener
                }

                val prevBlockedTag = blockedMetadata.copy()
                blockedMetadata.apply {
                    artist = trimmedText(binding.blockArtist)
                    albumArtist = trimmedText(binding.blockAlbumArtist)
                    album = trimmedText(binding.blockAlbum)
                    track = trimmedText(binding.blockTrack)
                    skip = binding.skip.isChecked
                    mute = binding.mute.isChecked

                    if (listOf(artist, albumArtist, album, track)
                            .all { it == "" }
                    )
                        return@setOnClickListener
                }
                if (prevBlockedTag != blockedMetadata || activity is MainDialogActivity)
                    GlobalScope.launch(Dispatchers.IO) {
                        PanoDb.db.getBlockedMetadataDao()
                            .insertLowerCase(listOf(blockedMetadata), ignore = false)
                    }
                if (activity is MainDialogActivity && blockedMetadata.skip) {
                    val i = Intent(NLService.iBLOCK_ACTION_S).apply {
                        `package` = requireContext().packageName
                        putSingle(blockedMetadata)
                        putExtra(NLService.B_HASH, requireArguments().getInt(NLService.B_HASH))
                    }
                    requireContext().sendBroadcast(i, NLService.BROADCAST_PERMISSION)
                }
                dialog.dismiss()
            }
        }
        return dialog
    }
}