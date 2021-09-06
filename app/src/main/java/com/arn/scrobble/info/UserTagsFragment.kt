package com.arn.scrobble.info

import android.app.Dialog
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.annotation.DrawableRes
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.VMFactory
import com.arn.scrobble.databinding.DialogUserTagsBinding
import com.arn.scrobble.pref.HistoryPref
import com.arn.scrobble.pref.MainPrefs
import com.frybits.harmony.getHarmonySharedPreferences
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.umass.lastfm.Album
import de.umass.lastfm.Artist
import de.umass.lastfm.MusicEntry
import de.umass.lastfm.Track

class UserTagsFragment: DialogFragment() {
    private val viewModel by lazy { VMFactory.getVM(this, UserTagsVM::class.java) }
    private val historyPref by lazy { HistoryPref(
            MainPrefs(context!!).sharedPreferences,
            MainPrefs.PREF_ACTIVITY_TAG_HISTORY,
            20
    ) }
    private val historyAdapter by lazy { ArrayAdapter(context!!, R.layout.list_item_history, historyPref.history) }
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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

        binding.userTagsInputEdittext.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (actionId == EditorInfo.IME_NULL && keyEvent.action == KeyEvent.ACTION_DOWN)) {
                binding.userTagsAdd.callOnClick()
                true
            } else
                false
        }
        binding.userTagsAdd.setOnClickListener {
            val tags = binding.userTagsInputEdittext.text.toString().trim()
            if (tags.isNotEmpty()) {
                viewModel.splitTags(tags).forEach{
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
            .setTitle(Stuff.getColoredTitle(context!!, entry.name))
            .setIcon(icon)
            .setView(binding.root)
            .create()
    }

    override fun onDestroyView() {
        _binding = null
        historyPref.save()
        super.onDestroyView()
    }

    private fun addChip(tag: String) {
        val chip = Chip(context!!)
        chip.text = tag
        chip.isCloseIconVisible = true
        chip.setOnCloseIconClickListener {
            viewModel.deleteTag(tag)
            binding.userTagsChipGroup.removeView(it)
            if (binding.userTagsChipGroup.childCount == 0)
                binding.userTagsStatus.visibility = View.VISIBLE
        }
        binding.userTagsChipGroup.addView(chip)
        binding.userTagsStatus.visibility = View.GONE
    }
}