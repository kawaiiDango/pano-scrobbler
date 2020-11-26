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
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.arn.scrobble.pending.db.Edit
import com.arn.scrobble.pref.MultiPreferences
import com.arn.scrobble.ui.ItemClickListener
import com.google.android.material.textfield.TextInputLayout
import kotlinx.android.synthetic.main.content_edits.view.*
import kotlinx.android.synthetic.main.dialog_edit_edits.view.*
import kotlinx.android.synthetic.main.text_input_edit.view.*


class EditsFragment: Fragment(), ItemClickListener {

    lateinit var adapter: EditsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.content_edits, container, false)

        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        adapter = EditsAdapter(context!!, view)
        adapter.itemClickListener = this
        view.edits_list.layoutManager = LinearLayoutManager(context!!)
        view.edits_list.adapter = adapter
        view.edits_list.setOnTouchListener{ v, motionEvent ->
            if (view.search_term.editText!!.isFocused) {
                view.search_term.clearFocus()
                imm?.hideSoftInputFromWindow(view.windowToken, 0)
            }
            false
        }

        view.search_term.editText?.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(editable: Editable?) {
                adapter.filter(editable.toString())
            }

        })

        view.search_term.editText?.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH){
                imm?.hideSoftInputFromWindow(view.windowToken, 0)
                textView.clearFocus()
                true
            } else
                false
        }

        if (!view.isInTouchMode)
            view.search_term.requestFocus()

        adapter.loadAll()
        return view
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
                val layout = layoutInflater.inflate(R.layout.dialog_edit_edits, null)

                (layout.edit_track_orig as TextInputLayout).hint = getString(R.string.track)
                layout.edit_track_orig.edittext.setText(e.origTrack)
                (layout.edit_track as TextInputLayout).hint = getString(R.string.track)
                layout.edit_track.edittext.setText(e.track)
                (layout.edit_album_orig as TextInputLayout).hint = getString(R.string.album)
                layout.edit_album_orig.edittext.setText(e.origAlbum)
                (layout.edit_album as TextInputLayout).hint = getString(R.string.album)
                layout.edit_album.edittext.setText(e.album)
                (layout.edit_artist_orig as TextInputLayout).hint = getString(R.string.artist)
                layout.edit_artist_orig.edittext.setText(e.origArtist)
                (layout.edit_artist as TextInputLayout).hint = getString(R.string.artist)
                layout.edit_artist.edittext.setText(e.artist)
                (layout.edit_album_artist as TextInputLayout).hint = getString(R.string.album_artist)
                layout.edit_album_artist.edittext.setText(e.albumArtist)

                AlertDialog.Builder(context!!, R.style.DarkDialog)
                        .setView(layout)
                        .setPositiveButton(android.R.string.ok){ dialogInterface, i ->
                            val ne = Edit()
                            ne.origTrack = layout.edit_track_orig.edittext.text.toString()
                            ne.track = layout.edit_track.edittext.text.toString()
                            ne.origAlbum = layout.edit_album_orig.edittext.text.toString()
                            ne.album = layout.edit_album.edittext.text.toString()
                            ne.origArtist = layout.edit_artist_orig.edittext.text.toString()
                            ne.artist = layout.edit_artist.edittext.text.toString()
                            ne.albumArtist = layout.edit_album_artist.edittext.text.toString()

                            if (
                                    ne.origTrack.trim().isEmpty() ||
                                    ne.artist.trim().isEmpty() ||
                                    ne.track.trim().isEmpty()
                                    ) {
                                Stuff.toast(context, getString(R.string.required_fields_empty))
                                return@setPositiveButton
                            }
                            if (e != ne) {
                                val checkArtist = e.artist.toLowerCase() != ne.artist.toLowerCase()
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