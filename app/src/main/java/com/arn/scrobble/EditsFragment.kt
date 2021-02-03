package com.arn.scrobble

import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.arn.scrobble.databinding.ContentEditsBinding
import com.arn.scrobble.databinding.DialogEditEditsBinding
import com.arn.scrobble.pending.db.Edit
import com.arn.scrobble.pref.MultiPreferences
import com.arn.scrobble.ui.ItemClickListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class EditsFragment: Fragment(), ItemClickListener {

    private lateinit var adapter: EditsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = ContentEditsBinding.inflate(inflater, container, false)

        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        adapter = EditsAdapter(context!!, binding)
        adapter.itemClickListener = this
        binding.editsList.layoutManager = LinearLayoutManager(context!!)
        binding.editsList.adapter = adapter
        binding.editsList.setOnTouchListener{ v, motionEvent ->
            if (binding.searchTerm.editText!!.isFocused) {
                binding.searchTerm.clearFocus()
                imm?.hideSoftInputFromWindow(binding.root.windowToken, 0)
            }
            false
        }

        binding.searchTerm.editText?.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(editable: Editable?) {
                adapter.filter(editable.toString())
            }

        })

        binding.searchTerm.editText?.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH){
                imm?.hideSoftInputFromWindow(binding.root.windowToken, 0)
                textView.clearFocus()
                true
            } else
                false
        }

        if (!binding.root.isInTouchMode)
            binding.searchTerm.requestFocus()

        adapter.loadAll()
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        Stuff.setTitle(activity, R.string.edits)
    }

    override fun onItemClick(view: View, position: Int) {
        if (position != -1) {
            //needed if the user quickly taps on the same item before the animation is done
            val e = adapter.getItem(position)
            if (view.id == R.id.edits_delete) {
                adapter.remove(position)
                AsyncTask.THREAD_POOL_EXECUTOR.execute {
                    adapter.dao.delete(e)
                }
            } else if (e.legacyHash == null) {
                val dialogBinding = DialogEditEditsBinding.inflate(layoutInflater)

                dialogBinding.editTrackOrig.root.hint = getString(R.string.track)
                dialogBinding.editTrackOrig.edittext.setText(e.origTrack)
                dialogBinding.editTrack.root.hint = getString(R.string.track)
                dialogBinding.editTrack.edittext.setText(e.track)
                dialogBinding.editAlbumOrig.root.hint = getString(R.string.album)
                dialogBinding.editAlbumOrig.edittext.setText(e.origAlbum)
                dialogBinding.editAlbum.root.hint = getString(R.string.album)
                dialogBinding.editAlbum.edittext.setText(e.album)
                dialogBinding.editArtistOrig.root.hint = getString(R.string.artist)
                dialogBinding.editArtistOrig.edittext.setText(e.origArtist)
                dialogBinding.editArtist.root.hint = getString(R.string.artist)
                dialogBinding.editArtist.edittext.setText(e.artist)
                dialogBinding.editAlbumArtist.root.hint = getString(R.string.album_artist)
                dialogBinding.editAlbumArtist.edittext.setText(e.albumArtist)

                MaterialAlertDialogBuilder(context!!)
                        .setView(dialogBinding.root)
                        .setPositiveButton(android.R.string.ok){ dialogInterface, i ->
                            val ne = Edit()
                            ne.origTrack = dialogBinding.editTrackOrig.edittext.text.toString().trim()
                            ne.track = dialogBinding.editTrack.edittext.text.toString().trim()
                            ne.origAlbum = dialogBinding.editAlbumOrig.edittext.text.toString().trim()
                            ne.album = dialogBinding.editAlbum.edittext.text.toString().trim()
                            ne.origArtist = dialogBinding.editArtistOrig.edittext.text.toString().trim()
                            ne.artist = dialogBinding.editArtist.edittext.text.toString().trim()
                            ne.albumArtist = dialogBinding.editAlbumArtist.edittext.text.toString().trim()

                            if (
                                    ne.origTrack.isEmpty() ||
                                    ne.artist.isEmpty() ||
                                    ne.track.isEmpty()
                                    ) {
                                Stuff.toast(context, getString(R.string.required_fields_empty))
                                return@setPositiveButton
                            }
                            if (e != ne) {
                                val checkArtist = ne.artist.isNotEmpty() && e.artist.toLowerCase() != ne.artist.toLowerCase()
                                adapter.update(position, ne)
                                AsyncTask.THREAD_POOL_EXECUTOR.execute {
                                    if (checkArtist) {
                                        val allowedSet = MultiPreferences(context!!).getStringSet(Stuff.PREF_ALLOWED_ARTISTS, setOf())
                                        val artist = LFMRequester.getValidArtist(ne.artist, allowedSet)
                                        if (artist == null) {
                                            activity?.runOnUiThread {
                                                if (Main.isOnline)
                                                    Stuff.toast(activity!!, getString(R.string.state_invalid_artist))
                                                else
                                                    Stuff.toast(activity!!, getString(R.string.unavailable_offline))
                                                adapter.update(position, e)
                                            }
                                            return@execute
                                        }
                                    }
                                    if (!(e.origTrack == ne.origTrack.toLowerCase() &&
                                                    e.origAlbum == ne.origAlbum.toLowerCase() &&
                                                    e.origArtist == ne.origArtist.toLowerCase()))
                                        adapter.dao.delete(e)
                                    adapter.dao.insertReplaceLowerCase(ne)
                                }
                            }
                        }
                        .show()
            }
        }
    }
}