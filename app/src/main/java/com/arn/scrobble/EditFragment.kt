package com.arn.scrobble

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.AsyncTask
import android.os.Bundle
import android.text.InputType
import android.transition.Fade
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.arn.scrobble.pending.db.Edit
import com.arn.scrobble.pending.db.PendingScrobblesDb
import de.umass.lastfm.CallException
import de.umass.lastfm.Session
import de.umass.lastfm.Track
import de.umass.lastfm.scrobble.ScrobbleData
import de.umass.lastfm.scrobble.ScrobbleResult
import kotlinx.android.synthetic.main.content_login.*
import kotlinx.android.synthetic.main.content_login.view.*
import java.util.*

class EditFragment: LoginFragment() {
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

        if (arguments?.getBoolean(NLService.B_FORCEABLE) == true) {
            view.login_force.visibility = View.VISIBLE
            view.login_force.setOnCheckedChangeListener { compoundButton, checked ->
                if (checked)
                    login_submit.setText(R.string.force)
                else
                    login_submit.setText(R.string.menu_edit)
            }
        }

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
        setStyle(STYLE_NO_TITLE, R.style.AppTheme_Transparent)
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    override fun validateAsync(): String? {
        val args = arguments ?: return null
        val track = login_textfield1.editText!!.text.toString()
        val origTrack = args.getString(NLService.B_TITLE) ?: ""
        var album = login_textfield2.editText!!.text.toString()
        var albumArtist = ""//login_textfield2.editText!!.text.toString()
        val origAlbum = args.getString(NLService.B_ALBUM) ?: ""
        val artist = login_textfield_last.editText!!.text.toString()
        val origArtist = args.getString(NLService.B_ARTIST) ?: ""
        val timeMillis = args.getLong(NLService.B_TIME, System.currentTimeMillis())
        var errMsg: String? = null

        fun saveEdit(context: Context) {
            if(!(track == origTrack && artist == origArtist && album == origAlbum)) {
                val dao = PendingScrobblesDb.getDb(context).getEditsDao()
                val e = Edit()
                e.artist = artist
                e.album = album
                e.albumArtist = albumArtist
                e.track = track
                e.hash = origArtist.hashCode().toString() + origAlbum.hashCode().toString() + origTrack.hashCode().toString()
                dao.upsert(e)
            }
        }

        if (track.isBlank() || artist.isBlank()) {
            errMsg = getString(R.string.required_fields_empty)
            return errMsg
        }

        if(!standalone && track == origTrack &&
                artist == origArtist && album == origAlbum && album != "") {
            return errMsg
        }

        try {
            var validArtist:String? = null
            var validTrack:Track? = null

            Stuff.initCaller(context!!)

            if (!login_force.isChecked) {
                if (album.isBlank() && origAlbum.isBlank())
                    validTrack =
                            try {
                                Track.getInfo(artist, track, Stuff.LAST_KEY)
                            } catch (e: Exception) {
                                null
                            }
                if (validTrack == null)
                    validArtist = LFMRequester.getValidArtist(artist, pref.getStringSet(Stuff.PREF_ALLOWED_ARTISTS, null))
                else {
                    if (album.isBlank() && validTrack.album != null) {
                        album = validTrack.album
                        activity!!.runOnUiThread { login_textfield2.editText!!.setText(validTrack.album) }
                    }
                    if (albumArtist.isBlank() && validTrack.albumArtist != null) {
                        albumArtist = validTrack.albumArtist
                    }
                }
            }
            if (validTrack == null && validArtist == null && !login_force.isChecked) {
                errMsg = getString(R.string.state_invalid_artist)
            } else {
                val lastfmSessKey: String? = pref.getString(Stuff.PREF_LASTFM_SESS_KEY, null)
                val lastfmSession = Session.createSession(Stuff.LAST_KEY, Stuff.LAST_SECRET, lastfmSessKey)
                val scrobbleData = ScrobbleData(artist, track, (timeMillis / 1000).toInt())
                scrobbleData.album = album
                scrobbleData.albumArtist = albumArtist
                val result = Track.scrobble(scrobbleData, lastfmSession)
                val activity = activity!!

                if (result?.isSuccessful == true && !result.isIgnored) {
                    AsyncTask.THREAD_POOL_EXECUTOR.execute {
                        if (!standalone) {
                            if (args.getLong(NLService.B_TIME) == 0L)
                                Track.updateNowPlaying(scrobbleData, lastfmSession)
                            else {
                                val origTrackObj = Track(origTrack, null, origArtist)
                                origTrackObj.playedWhen = Date(timeMillis)
                                LFMRequester(activity).delete(origTrackObj) { succ ->
                                    if (succ) {
                                        //editing just the album is a noop, scrobble again
                                        if (track == origTrack && artist == origArtist)
                                            Track.scrobble(scrobbleData, lastfmSession)
                                    }
                                }
                                        .asSerialAsyncTask()
                            }
                        }

                        //scrobble everywhere else
                        fun getScrobbleResult(scrobbleData: ScrobbleData, session: Session): ScrobbleResult {
                            return try {
                                Track.scrobble(scrobbleData, session)
                            } catch (e: CallException) {
                                ScrobbleResult.createHttp200OKResult(0, e.cause?.message, "")
                            }
                        }

                        pref.getString(Stuff.PREF_LIBREFM_SESS_KEY, null)?.let {
                            val librefmSession: Session = Session.createCustomRootSession(Stuff.LIBREFM_API_ROOT,
                                    Stuff.LIBREFM_KEY, Stuff.LIBREFM_KEY, it)
                            getScrobbleResult(scrobbleData, librefmSession)
                        }

                        pref.getString(Stuff.PREF_GNUFM_SESS_KEY, null)?.let {
                            val gnufmSession: Session = Session.createCustomRootSession(
                                    pref.getString(Stuff.PREF_GNUFM_ROOT, null) + "2.0/",
                                    Stuff.LIBREFM_KEY, Stuff.LIBREFM_KEY, it)
                            getScrobbleResult(scrobbleData, gnufmSession)
                        }

                        pref.getString(Stuff.PREF_LISTENBRAINZ_TOKEN, null)?.let {
                            ListenBrainz(it)
                                    .scrobble(scrobbleData)
                        }

                        pref.getString(Stuff.PREF_LB_CUSTOM_TOKEN, null)?.let {
                            ListenBrainz(it)
                                    .setApiRoot(pref.getString(Stuff.PREF_LB_CUSTOM_ROOT, null))
                                    .scrobble(scrobbleData)
                        }
                    }
                    saveEdit(context!!)
                    if (login_force.isChecked){
                        val oldSet = pref.getStringSet(Stuff.PREF_ALLOWED_ARTISTS, setOf())
                        pref.putStringSet(Stuff.PREF_ALLOWED_ARTISTS, oldSet + artist)
                    }
                } else if (result.isIgnored) {
                    if (System.currentTimeMillis() - timeMillis < Stuff.LASTFM_MAX_PAST_SCROBBLE)
                        errMsg = getString(R.string.scrobble_ignored_or_old)
                    else {
                        errMsg = ""
                        activity.runOnUiThread {
                            AlertDialog.Builder(context!!, R.style.DarkDialog)
                                    .setMessage(R.string.scrobble_ignored_save_edit)
                                    .setPositiveButton(android.R.string.yes) { dialogInterface, i ->
                                        dismiss()
                                        AsyncTask.THREAD_POOL_EXECUTOR.execute {
                                            saveEdit(activity)
                                        }
                                    }
                                    .setNegativeButton(android.R.string.no, null)
                                    .show()
                        }
                    }
                } else if (!standalone)
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
            if (args.getLong(NLService.B_TIME) == 0L) { //now playing
                context?.sendBroadcast(Intent(NLService.pCANCEL))
            }
        }
        return errMsg

    }
}