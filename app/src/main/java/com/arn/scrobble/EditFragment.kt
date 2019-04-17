package com.arn.scrobble

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.InputType
import android.transition.Fade
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import de.umass.lastfm.Session
import de.umass.lastfm.Track
import de.umass.lastfm.scrobble.ScrobbleData
import kotlinx.android.synthetic.main.content_login.*
import kotlinx.android.synthetic.main.content_login.view.*

class EditFragment: LoginFragment() {

    var standalone = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        arguments?.putString(TEXTF1, getString(R.string.track))
        arguments?.putString(TEXTF2, getString(R.string.album))
        arguments?.putString(TEXTFL, getString(R.string.artist))
        val view = super.onCreateView(inflater, container, savedInstanceState)

        checksLogin = false
        returnTransition = Fade()
        if (arguments?.getBoolean(NLService.B_STANDALONE) != true)
            showsDialog = true
        else
            standalone = true


        arguments?.getString(NLService.B_TITLE)?.let {
            view.login_textfield1.editText!!.setText(it)
            view.login_textfield1.editText!!.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        }
        arguments?.getString(NLService.B_ALBUM)?.let {
            view.login_textfield2.editText!!.setText(it)
            view.login_textfield2.editText!!.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        }
        arguments?.getString(NLService.B_ARTIST)?.let {
            view.login_textfield_last.editText!!.setText(it)
            view.login_textfield_last.editText!!.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        }

        view.edit_swap.visibility = View.VISIBLE
        view.edit_swap.setOnClickListener {
            val tmp = view.login_textfield1.editText?.text
            view.login_textfield1.editText!!.text = view.login_textfield_last.editText!!.text
            view.login_textfield_last.editText!!.text = tmp
        }
        view.login_submit.text = getString(R.string.menu_edit)
        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.AppTheme_Transparent)
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    override fun validateAsync(): String? {
        val args = arguments ?: return null
        val track = login_textfield1.editText!!.text.toString()
        val origTrack = args.getString(NLService.B_TITLE)
        val album = login_textfield2.editText!!.text.toString()
        val origAlbum = args.getString(NLService.B_ALBUM)
        val artist = login_textfield_last.editText!!.text.toString()
        val origArtist = args.getString(NLService.B_ARTIST)
        val timeMillis = args.getLong(NLService.B_TIME, System.currentTimeMillis())
        var errMsg: String? = null

        if (track.isBlank() || artist.isBlank()) {
            errMsg = getString(R.string.required_fields_empty)
            return errMsg
        }

        if(!standalone && track == origTrack &&
                artist == origArtist && album == origAlbum) {
            return errMsg
        }

        try {
            val validArtist = LFMRequester.getValidArtist(artist, track, 4)
            if (validArtist == null) {
                errMsg = getString(R.string.state_invalid_artist)
            } else {
                val lastfmSessKey: String? = pref.getString(Stuff.PREF_LASTFM_SESS_KEY, null)
                val lastfmSession = Session.createSession(Stuff.LAST_KEY, Stuff.LAST_SECRET, lastfmSessKey)
                val scrobbleData = ScrobbleData(artist, track, (timeMillis / 1000).toInt())
                scrobbleData.album = album
                val result = Track.scrobble(scrobbleData, lastfmSession)

                if (result?.isSuccessful == true && !result.isIgnored && !standalone) {
                    val unscrobbler = LastfmUnscrobbler(context!!)
                    val csrfExists = unscrobbler.checkCsrf(pref.getString(Stuff.PREF_LASTFM_USERNAME, null)!!)
                    if (csrfExists)
                        unscrobbler.unscrobble(origArtist, origTrack, timeMillis)

                    //editing just the album is a noop, scrobble again
                    if (track == origTrack && artist == origArtist && album != origAlbum)
                        Track.scrobble(scrobbleData, lastfmSession)
                } else if (result.isIgnored)
                    errMsg = getString(R.string.scrobble_ignored_or_old)
                else if (!standalone)
                    errMsg = getString(R.string.network_error)
            }
        } catch (e: Exception){
            errMsg = e.message
        }
        if (errMsg == null) {
            val i = Intent(NLService.iEDITED)
            i.putExtra(NLService.B_ARTIST, artist)
            i.putExtra(NLService.B_ALBUM, album)
            i.putExtra(NLService.B_TITLE, track)
            i.putExtra(NLService.B_TIME, timeMillis)
            context?.sendBroadcast(i)
        }
        return errMsg

    }
}