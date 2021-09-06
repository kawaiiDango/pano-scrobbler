package com.arn.scrobble.edits

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.lifecycleScope
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.arn.scrobble.*
import com.arn.scrobble.db.SimpleEdit
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.RegexEdit
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.umass.lastfm.CallException
import de.umass.lastfm.Session
import de.umass.lastfm.Track
import de.umass.lastfm.scrobble.ScrobbleData
import de.umass.lastfm.scrobble.ScrobbleResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*


class EditDialogFragment: LoginFragment() {

    override val checksLogin = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        arguments?.putString(TEXTF1, getString(R.string.track))
        arguments?.putString(TEXTF2, getString(R.string.album_optional))
        arguments?.putString(TEXTFL, getString(R.string.artist))

        super.onCreateView(layoutInflater, null, null)

        showsDialog = true

        if (arguments?.getBoolean(NLService.B_STANDALONE) == true)
            isStandalone = true

        if (arguments?.getBoolean(NLService.B_FORCEABLE) == true) {
            binding.loginForce.visibility = View.VISIBLE
            binding.loginForce.setOnCheckedChangeListener { compoundButton, checked ->
                if (checked)
                    binding.loginSubmit.setText(R.string.force)
                else
                    binding.loginSubmit.setText(R.string.edit)
            }
        }

        arguments?.getString(NLService.B_TRACK)?.let {
            binding.loginTextfield1.editText!!.setText(it)
            binding.loginTextfield1.editText!!.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        }
        arguments?.getString(NLService.B_ALBUM)?.let {
            binding.loginTextfield2.editText!!.setText(it)
            binding.loginTextfield2.editText!!.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        }
        arguments?.getString(NLService.B_ARTIST)?.let {
            binding.loginTextfieldLast.editText!!.setText(it)
            binding.loginTextfieldLast.editText!!.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        }
        arguments?.getString(NLService.B_ALBUM_ARTIST)?.let {
            binding.loginTextfieldLast2.editText!!.setText(it)
        }

        binding.editSwap.visibility = View.VISIBLE
        binding.editSwap.setOnClickListener {
            val tmp = binding.loginTextfield1.editText?.text
            binding.loginTextfield1.editText!!.text = binding.loginTextfieldLast.editText!!.text
            binding.loginTextfieldLast.editText!!.text = tmp
        }

        binding.editAlbumArtist.visibility = View.VISIBLE
        binding.editAlbumArtist.setOnClickListener {
            it.visibility = View.GONE
            binding.loginTextfieldLast2.hint = getString(R.string.album_artist)
            binding.loginTextfieldLast2.editText!!.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            binding.loginTextfieldLast.editText!!.imeOptions = EditorInfo.IME_NULL
            binding.loginTextfieldLast2.editText?.setOnEditorActionListener { textView, actionId, keyEvent ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                        (actionId == EditorInfo.IME_NULL && keyEvent.action == KeyEvent.ACTION_DOWN)) {
                    binding.loginSubmit.callOnClick()
                    true
                } else
                    false
            }
            TransitionManager.beginDelayedTransition(binding.root, Fade())

            binding.loginTextfieldLast2.visibility = View.VISIBLE
            binding.loginTextfieldLast2.requestFocus()
        }
        binding.loginSubmit.text = getString(R.string.edit)

        return MaterialAlertDialogBuilder(context!!)
        .setView(binding.root)
        .create()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (isStandalone)
            activity?.finish()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        if (isStandalone)
            activity?.finish()
    }

    override suspend fun validateAsync(): String? {
        val args = arguments ?: return null
        val track = binding.loginTextfield1.editText!!.text.toString().trim()
        val origTrack = args.getString(NLService.B_TRACK) ?: ""
        var album = binding.loginTextfield2.editText!!.text.toString().trim()
        var albumArtist = binding.loginTextfieldLast2.editText!!.text.toString().trim()
        val origAlbum = args.getString(NLService.B_ALBUM) ?: ""
        val artist = binding.loginTextfieldLast.editText!!.text.toString().trim()
        val origArtist = args.getString(NLService.B_ARTIST) ?: ""
        val timeMillis = args.getLong(NLService.B_TIME, System.currentTimeMillis())
        var errMsg: String? = null

        fun saveEdit(context: Context) {
            if(!(track == origTrack && artist == origArtist && album == origAlbum && albumArtist == "")) {
                val dao = PanoDb.getDb(context).getSimpleEditsDao()
                val e = SimpleEdit(
                    artist = artist,
                    album = album,
                    albumArtist = albumArtist,
                    track = track,
                    origArtist = origArtist,
                    origAlbum = origAlbum,
                    origTrack = origTrack,
                )
                dao.insertReplaceLowerCase(e)
                dao.deleteLegacy(origArtist.hashCode().toString() + origAlbum.hashCode().toString() + origTrack.hashCode().toString())
            }
        }

        if (track.isBlank() || artist.isBlank()) {
            errMsg = getString(R.string.required_fields_empty)
            return errMsg
        }

        if(!isStandalone && track == origTrack &&
                artist == origArtist && album == origAlbum && album != "" && albumArtist == "") {
            return errMsg
        }

        try {
            var validArtist:String? = null
            var validAlbumArtist:String? = ""
            var validTrack:Track? = null

            if (!binding.loginForce.isChecked) {
                if (album.isBlank() && origAlbum.isBlank())
                    validTrack =
                            try {
                                Track.getInfo(artist, track, Stuff.LAST_KEY)
                            } catch (e: Exception) {
                                null
                            }
                if (validTrack == null) {
                    validArtist = LFMRequester.getValidArtist(
                        artist,
                        prefs.allowedArtists
                    )
                    if (albumArtist.isNotEmpty())
                        validAlbumArtist = LFMRequester.getValidArtist(
                            albumArtist,
                            prefs.allowedArtists
                        )

                } else {
                    if (album.isBlank() && validTrack.album != null) {
                        album = validTrack.album
                        withContext(Dispatchers.Main) {
                            binding.loginTextfield2.editText!!.setText(validTrack.album)
                        }
                    }
                    if (albumArtist.isBlank() && validTrack.albumArtist != null) {
                        albumArtist = validTrack.albumArtist
                        withContext(Dispatchers.Main) {
                            binding.loginTextfieldLast2.editText!!.setText(validTrack.albumArtist)
                        }
                    }
                }
            }
            if (validTrack == null && (validArtist == null || validAlbumArtist == null) && !binding.loginForce.isChecked) {
                errMsg = getString(R.string.state_unrecognised_artist)
            } else {
                val lastfmSessKey: String? = prefs.lastfmSessKey
                val lastfmSession = Session.createSession(
                    Stuff.LAST_KEY,
                    Stuff.LAST_SECRET, lastfmSessKey)
                val scrobbleData = ScrobbleData(artist, track, (timeMillis / 1000).toInt())
                scrobbleData.album = album
                scrobbleData.albumArtist = albumArtist
                val isLastfmDisabled = prefs.lastfmDisabled
                val result = if (isLastfmDisabled && isStandalone)
                    null
                else
                    Track.scrobble(scrobbleData, lastfmSession)
                val activity = activity!!

                if ((isLastfmDisabled && isStandalone) ||
                    (result?.isSuccessful == true && !result.isIgnored)) {
                    coroutineScope {
                        if (!isStandalone) {
                            launch {
                                if (args.getLong(NLService.B_TIME) == 0L)
                                    Track.updateNowPlaying(scrobbleData, lastfmSession)
                                else {
                                    val origTrackObj = Track(origTrack, null, origArtist)
                                    origTrackObj.playedWhen = Date(timeMillis)
                                    LFMRequester(activity, this).delete(origTrackObj) { succ ->
                                        if (succ) {
                                            //editing just the album is a noop, scrobble again
                                            if (track.equals(origTrack, ignoreCase = true) &&
                                                artist.equals(origArtist, ignoreCase = true)
                                            )
                                                withContext(Dispatchers.IO) {
                                                    Track.scrobble(scrobbleData, lastfmSession)
                                                }
                                        }
                                    }
                                }
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

                        launch {
                            prefs.librefmSessKey?.let {
                                val librefmSession: Session = Session.createCustomRootSession(
                                    Stuff.LIBREFM_API_ROOT,
                                    Stuff.LIBREFM_KEY, Stuff.LIBREFM_KEY, it
                                )
                                getScrobbleResult(scrobbleData, librefmSession)
                            }
                        }
                        launch {
                            prefs.gnufmSessKey?.let {
                                val gnufmSession: Session = Session.createCustomRootSession(
                                    prefs.gnufmRoot + "2.0/",
                                    Stuff.LIBREFM_KEY, Stuff.LIBREFM_KEY, it
                                )
                                getScrobbleResult(scrobbleData, gnufmSession)
                            }
                        }
                        launch {
                            prefs.listenbrainzToken?.let {
                                ListenBrainz(it)
                                    .scrobble(scrobbleData)
                            }
                        }
                        launch {
                            prefs.customListenbrainzToken?.let {
                                ListenBrainz(it)
                                    .setApiRoot(prefs.customListenbrainzRoot!!)
                                    .scrobble(scrobbleData)
                            }
                        }
                    }
                    saveEdit(context!!)
                    if (binding.loginForce.isChecked){
                        val oldSet = prefs.allowedArtists.toSet()
                        prefs.allowedArtists = oldSet + artist
                    }
                } else if (result?.isIgnored == true) {
                    if (System.currentTimeMillis() - timeMillis < Stuff.LASTFM_MAX_PAST_SCROBBLE)
                        errMsg = getString(R.string.lastfm) + ": " + getString(R.string.scrobble_ignored)
                    else {
                        errMsg = ""
                        withContext(Dispatchers.Main) {
                            MaterialAlertDialogBuilder(context!!)
                                    .setMessage(R.string.scrobble_ignored_save_edit)
                                    .setPositiveButton(android.R.string.yes) { dialogInterface, i ->
                                        launch(Dispatchers.IO) {
                                            saveEdit(activity)
                                            withContext(Dispatchers.Main) {
                                                dismiss()
                                            }
                                        }
                                    }
                                    .setNegativeButton(android.R.string.no, null)
                                    .show()
                        }
                    }
                } else {
                    Stuff.log("edit scrobble err: $result")
                    if (!isStandalone)
                        errMsg = getString(R.string.network_error)
                }

                if (result?.isIgnored == false &&
                    !prefs.regexEditsLearnt &&
                        !isStandalone) {
                    val originalScrobbleData = ScrobbleData().apply {
                        this.artist = origArtist
                        this.album = origAlbum
                        this.track = origTrack
                    }

                    val dao = PanoDb.getDb(context!!).getRegexEditsDao()
                    val existingRegexReplacements = dao.performRegexReplace(originalScrobbleData)

                    if (existingRegexReplacements.values.sum() == 0) {
                        val allPresets = RegexPresets.presetKeys.mapIndexed { index, key ->
                            RegexPresets.getPossiblePreset(
                                RegexEdit(
                                    order = index,
                                    preset = key
                                )
                            )
                        }
                        val matchedRegexEdits = mutableListOf<RegexEdit>()
                        val suggestedRegexReplacements = dao.performRegexReplace(originalScrobbleData, allPresets, matchedRegexEdits)
                        val replacementsInEdit = dao.performRegexReplace(scrobbleData, allPresets)

                        if (suggestedRegexReplacements.values.sum() > 0 && replacementsInEdit.values.sum() == 0) {
                            withContext(Dispatchers.Main) {
                                val presetName = RegexPresets.getString(context!!,
                                    matchedRegexEdits.first().preset!!)

                                MaterialAlertDialogBuilder(context!!)
                                    .setMessage(
                                        getString(
                                            R.string.regex_edits_suggestion,
                                            presetName
                                        )
                                    )
                                    .setPositiveButton(android.R.string.yes) { _, _ ->
                                        dismiss()
                                        activity.supportFragmentManager
                                            .beginTransaction()
                                            .replace(R.id.frame, RegexEditsFragment().apply {
                                                arguments = Bundle().apply {
                                                    putBoolean(
                                                        Stuff.ARG_SHOW_DIALOG,
                                                        true
                                                    )
                                                }
                                            })
                                            .addToBackStack(null)
                                            .commit()
                                    }
                                    .setNegativeButton(android.R.string.no) { _, _ ->
                                        prefs.regexEditsLearnt = true
                                    }
                                    .show()
                            }
                        }
                    }
                }
            }
        } catch (e: Exception){
            errMsg = e.message
        }
        if (errMsg == null) {
            (activity as? MainActivity)?.let {
                it.mainNotifierViewModel.editData.postValue(
                        Track(track, null, album, artist).apply {
                            if (args.getLong(NLService.B_TIME) != 0L)
                                playedWhen = Date(timeMillis)
                    }
                )
            }
            if (args.getLong(NLService.B_TIME) == 0L /* now playing */) {
                context?.sendBroadcast(Intent(NLService.iCANCEL))
            }
        }
        return errMsg

    }
}