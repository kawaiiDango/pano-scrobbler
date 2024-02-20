package com.arn.scrobble.edits

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.arn.scrobble.R
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.putSingle
import com.arn.scrobble.databinding.DialogEditEditsBinding
import com.arn.scrobble.db.SimpleEdit
import com.arn.scrobble.ui.UiUtils.toast
import com.arn.scrobble.ui.UiUtils.trimmedText
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SimpleEditsEditFragment : DialogFragment(), DialogInterface.OnShowListener {
    private var _binding: DialogEditEditsBinding? = null
    private val binding
        get() = _binding!!
    private var editId = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onShow(di: DialogInterface?) {
        (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val newEdit = getEditedEdit()
            if (
                newEdit.origTrack.isEmpty() ||
                newEdit.artist.isEmpty() ||
                newEdit.track.isEmpty()
            ) {
                requireContext().toast(R.string.required_fields_empty)
                return@setOnClickListener
            }
            setFragmentResult(Stuff.ARG_EDIT, bundleOf(Stuff.ARG_EDIT to newEdit))
            di?.dismiss()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogEditEditsBinding.inflate(layoutInflater)

        val edit = arguments?.getParcelable(Stuff.ARG_EDIT) ?: SimpleEdit()
        editId = edit._id

        if (savedInstanceState == null)
            binding.apply {
                editTrackOrigEdittext.setText(edit.origTrack)
                editTrackEdittext.setText(edit.track)
                editAlbumOrigEdittext.setText(edit.origAlbum)
                editAlbumEdittext.setText(edit.album)
                editArtistOrigEdittext.setText(edit.origArtist)
                editArtistEdittext.setText(edit.artist)
                editAlbumArtistEdittext.setText(edit.albumArtist)
            }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .apply {
                setOnShowListener(this@SimpleEditsEditFragment)
            }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun getEditedEdit() = SimpleEdit(
        _id = editId,
        origTrack = binding.editTrackOrigEdittext.trimmedText(),
        track = binding.editTrackEdittext.trimmedText(),
        origAlbum = binding.editAlbumOrigEdittext.trimmedText(),
        album = binding.editAlbumEdittext.trimmedText(),
        origArtist = binding.editArtistOrigEdittext.trimmedText(),
        artist = binding.editArtistEdittext.trimmedText(),
        albumArtist = binding.editAlbumArtistEdittext.trimmedText(),
    )

    override fun onSaveInstanceState(outState: Bundle) {
        requireArguments().putSingle(getEditedEdit())
        super.onSaveInstanceState(outState)
    }
}
