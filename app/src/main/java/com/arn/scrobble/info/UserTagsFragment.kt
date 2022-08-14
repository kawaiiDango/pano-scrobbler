package com.arn.scrobble.info

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.databinding.DialogUserTagsBinding
import com.arn.scrobble.pref.HistoryPref
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.ui.UiUtils
import com.arn.scrobble.ui.UiUtils.hideKeyboard
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.umass.lastfm.Album
import de.umass.lastfm.Artist
import de.umass.lastfm.MusicEntry
import de.umass.lastfm.Track

class UserTagsFragment : DialogFragment(), DialogInterface.OnShowListener {
    private val viewModel by viewModels<UserTagsVM>()
    private val historyPref by lazy {
        HistoryPref(
            MainPrefs(context!!).sharedPreferences,
            MainPrefs.PREF_ACTIVITY_TAG_HISTORY,
            20
        )
    }
    private val historyAdapter by lazy {
        object : ArrayAdapter<String>(
            context!!,
            R.layout.list_item_history,
            historyPref.history
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val historyTextView = super.getView(position, convertView, parent)
                if (convertView == null) {
                    historyTextView.setOnClickListener {
                        val item = getItem(position)!!
                        hideKeyboard()
                        binding.userTagsInputEdittext.setText(item, false)
                        binding.userTagsInputEdittext.clearFocus()
                    }
                    historyTextView.setOnLongClickListener {
                        MaterialAlertDialogBuilder(context)
                            .setMessage(R.string.clear_history_specific)
                            .setPositiveButton(R.string.yes) { dialogInterface, i ->
                                val item = getItem(position)!!
                                historyPref.remove(item)
                                remove(item)
                                notifyDataSetChanged()
                            }
                            .setNegativeButton(R.string.no, null)
                            .setNeutralButton(R.string.clear_all_history) { dialogInterface, i ->
                                historyPref.removeAll()
                                clear()
                                notifyDataSetChanged()
                            }
                            .show()
                        false
                    }
                }
                return historyTextView
            }
        }
    }
    private var _binding: DialogUserTagsBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onShow(p0: DialogInterface?) {
        viewModel.tags.observe(viewLifecycleOwner, object : Observer<MutableSet<String>> {
            override fun onChanged(it: MutableSet<String>?) {
                it ?: return
                binding.userTagsProgress.hide()
                if (it.isEmpty())
                    binding.userTagsStatus.visibility = View.VISIBLE
                it.forEach {
                    addChip(it)
                }
                viewModel.tags.removeObserver(this)
            }
        })

        val addButton = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)

        binding.userTagsInputEdittext.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (actionId == EditorInfo.IME_NULL && keyEvent.action == KeyEvent.ACTION_DOWN)
            ) {
                addButton.callOnClick()
                true
            } else
                false
        }
        addButton.setOnClickListener {
            val tags = binding.userTagsInputEdittext.text.toString().trim()
            if (tags.isNotEmpty()) {
                viewModel.splitTags(tags).forEach {
                    if (viewModel.tags.value?.contains(it) == false)
                        addChip(it.trim())
                    historyPref.add(it.trim())
                }
                viewModel.addTag(tags)
                binding.userTagsInputEdittext.text.clear()
            }
        }

        viewModel.tags.value ?: viewModel.loadTags()
        historyPref.load()

        binding.userTagsInputEdittext.setAdapter(historyAdapter)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val entry: MusicEntry
        @DrawableRes val icon: Int
        val b = arguments!!
        val track = b.getString(NLService.B_TRACK)
        val album = b.getString(NLService.B_ALBUM)
        val artist = b.getString(NLService.B_ARTIST)

        when {
            track != null -> {
                entry = Track(track, null, artist)
                icon = R.drawable.vd_note
            }
            album != null -> {
                entry = Album(album, null, artist)
                icon = R.drawable.vd_album
            }
            else -> {
                entry = Artist(artist, null)
                icon = R.drawable.vd_mic
            }
        }

        viewModel.entry = entry
        viewModel.historyPref = historyPref
        _binding = DialogUserTagsBinding.inflate(layoutInflater)

        return MaterialAlertDialogBuilder(context!!)
            .setTitle(UiUtils.getColoredTitle(context!!, entry.name))
            .setIcon(icon)
            .setView(binding.root)
            .setPositiveButton(R.string.add, null)
            .create()
            .apply {
                setOnShowListener(this@UserTagsFragment)
            }
    }

    override fun onDestroyView() {
        _binding = null
        historyPref.save()
        super.onDestroyView()
    }

    private fun addChip(tag: String) {
        val chip = Chip(context!!).apply {
            text = tag
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                viewModel.deleteTag(tag)
                binding.userTagsChipGroup.removeView(it)
                if (binding.userTagsChipGroup.childCount == 0)
                    binding.userTagsStatus.visibility = View.VISIBLE
            }
        }
        binding.userTagsChipGroup.addView(chip)
        binding.userTagsStatus.visibility = View.GONE
    }
}