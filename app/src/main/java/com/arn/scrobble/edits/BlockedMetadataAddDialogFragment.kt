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
import com.arn.scrobble.utils.Stuff.getSingle
import com.arn.scrobble.utils.Stuff.putSingle
import com.arn.scrobble.databinding.DialogBlockedMetadataBinding
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.db.BlockedMetadataDao.Companion.insertLowerCase
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.ui.UiUtils.trimmedText
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

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setPositiveButton(R.string.block, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        binding.apply {
            blockArtistEdittext.setText(blockedMetadata.artist)
            blockAlbumArtistEdittext.setText(blockedMetadata.albumArtist)
            blockAlbumEdittext.setText(blockedMetadata.album)
            blockTrackEdittext.setText(blockedMetadata.track)
            skip.isChecked = blockedMetadata.skip
            mute.isChecked = blockedMetadata.mute
            ignore.isChecked = !blockedMetadata.skip && !blockedMetadata.mute

            arrayOf(blockArtist, blockAlbumArtist, blockAlbum, blockTrack).forEach {
                it.endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
                it.setEndIconDrawable(R.drawable.vd_cancel)
            }

            if (ignoredArtist != null) {
                useChannel.visibility = View.VISIBLE
                useChannel.setOnCheckedChangeListener { _, checked ->
                    blockArtistEdittext.setText(
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
                    when (activity) {
                        is MainDialogActivity -> {
                            NavDeepLinkBuilder(requireContext())
                                .setComponentName(MainActivity::class.java)
                                .setGraph(R.navigation.nav_graph)
                                .setDestination(R.id.billingFragment)
                                .createPendingIntent()
                                .send()
                        }

                        is MainActivity -> {
                            findNavController().navigate(R.id.billingFragment)
                        }
                    }
                    return@setOnClickListener
                }

                val newBlockedMetadata = blockedMetadata.copy(
                    artist = binding.blockArtistEdittext.trimmedText(),
                    albumArtist = binding.blockAlbumArtistEdittext.trimmedText(),
                    album = binding.blockAlbumEdittext.trimmedText(),
                    track = binding.blockTrackEdittext.trimmedText(),
                    skip = binding.skip.isChecked,
                    mute = binding.mute.isChecked,
                )

                if (listOf(
                        newBlockedMetadata.artist,
                        newBlockedMetadata.albumArtist,
                        newBlockedMetadata.album,
                        newBlockedMetadata.track
                    ).all { it.isEmpty() }
                )
                    return@setOnClickListener

                if (newBlockedMetadata != blockedMetadata || blockedMetadata._id == 0)
                    GlobalScope.launch(Dispatchers.IO) {
                        PanoDb.db.getBlockedMetadataDao()
                            .insertLowerCase(listOf(newBlockedMetadata), ignore = false)
                    }
                if (activity is MainDialogActivity && newBlockedMetadata.skip) {
                    val i = Intent(NLService.iBLOCK_ACTION_S).apply {
                        `package` = requireContext().packageName
                        putSingle(newBlockedMetadata)
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