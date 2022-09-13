package com.arn.scrobble.edits

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.transition.Fade
import android.transition.TransitionManager
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.arn.scrobble.*
import com.arn.scrobble.db.*
import com.arn.scrobble.scrobbleable.Scrobblable
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import de.umass.lastfm.Session
import de.umass.lastfm.Track
import de.umass.lastfm.scrobble.ScrobbleData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.IOException
import java.util.*


class EditDialogFragment : LoginFragment() {

    override val checksLogin = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        arguments?.putString(TEXTF1, getString(R.string.track))
        arguments?.putString(TEXTF2, getString(R.string.album_optional))
        arguments?.putString(TEXTFL, getString(R.string.artist))

        super.onCreateView(layoutInflater, null, null)

        showsDialog = true

        if (arguments?.getBoolean(NLService.B_STANDALONE_DIALOG) == true)
            isStandalone = true

        fun moveImeActionToLast2() {
            binding.loginTextfieldLast.editText!!.imeOptions = EditorInfo.IME_NULL
            binding.loginTextfieldLast.editText!!.setOnEditorActionListener(null)
            binding.loginTextfieldLast2.editText?.setOnEditorActionListener { textView, actionId, keyEvent ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (actionId == EditorInfo.IME_NULL && keyEvent.action == KeyEvent.ACTION_DOWN)
                ) {
                    binding.loginSubmit.callOnClick()
                    true
                } else
                    false
            }
        }

        arrayOf(
            binding.loginTextfield1,
            binding.loginTextfield2,
            binding.loginTextfieldLast,
            binding.loginTextfieldLast2
        ).forEach {
            it.editText!!.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        }

        binding.loginTextfieldLast2.hint = getString(R.string.album_artist)

        arguments?.getString(NLService.B_TRACK)?.let {
            binding.loginTextfield1.editText!!.setText(it)
        }
        arguments?.getString(NLService.B_ALBUM)?.let {
            binding.loginTextfield2.editText!!.setText(it)
        }
        arguments?.getString(NLService.B_ARTIST)?.let {
            binding.loginTextfieldLast.editText!!.setText(it)
        }
        arguments?.getString(NLService.B_ALBUM_ARTIST)?.let {
            binding.loginTextfieldLast2.visibility = View.VISIBLE
            binding.loginTextfieldLast2.editText!!.setText(it)
            moveImeActionToLast2()
        }

        binding.loginTextfield2.endIconMode = TextInputLayout.END_ICON_CUSTOM
        binding.loginTextfield2.setEndIconDrawable(R.drawable.vd_swap)
        binding.loginTextfield2.setEndIconContentDescription(R.string.swap)

        binding.loginTextfield2.setEndIconOnClickListener {
            val tmp = binding.loginTextfield1.editText?.text
            binding.loginTextfield1.editText!!.text = binding.loginTextfieldLast.editText!!.text
            binding.loginTextfieldLast.editText!!.text = tmp
        }

        if (arguments?.getString(NLService.B_ALBUM_ARTIST).isNullOrEmpty()) {

            binding.loginTextfieldLast.endIconMode = TextInputLayout.END_ICON_CUSTOM
            binding.loginTextfieldLast.setEndIconDrawable(R.drawable.vd_album_artist)
            binding.loginTextfieldLast.setEndIconContentDescription(R.string.album_artist)

            binding.loginTextfieldLast.setEndIconOnClickListener {
                binding.loginTextfieldLast.isEndIconVisible = false
                moveImeActionToLast2()
                TransitionManager.beginDelayedTransition(binding.root, Fade())

                binding.loginTextfieldLast2.visibility = View.VISIBLE
                binding.loginTextfieldLast2.requestFocus()
            }
        }

        binding.loginSubmit.text = getString(R.string.edit)

        return MaterialAlertDialogBuilder(context!!)
            .setView(binding.root)
            .create()
    }

    override suspend fun validateAsync(): Boolean {
        val args = arguments ?: return false
        val track = binding.loginTextfield1.editText!!.text.toString().trim()
        val origTrack = args.getString(NLService.B_TRACK) ?: ""
        var album = binding.loginTextfield2.editText!!.text.toString().trim()
        var albumArtist = binding.loginTextfieldLast2.editText!!.text.toString().trim()
        val origAlbum = args.getString(NLService.B_ALBUM) ?: ""
        val artist = binding.loginTextfieldLast.editText!!.text.toString().trim()
        val origArtist = args.getString(NLService.B_ARTIST) ?: ""
        val timeMillis = args.getLong(NLService.B_TIME, System.currentTimeMillis())
        val forceScrobble = binding.loginCheckbox.isChecked
        var success = true

        fun saveEdit(context: Context) {
            if (!(track == origTrack && artist == origArtist && album == origAlbum && albumArtist == "")) {
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
                dao.deleteLegacy(
                    origArtist.hashCode().toString() + origAlbum.hashCode()
                        .toString() + origTrack.hashCode().toString()
                )
            }
        }

        if (track.isBlank() || artist.isBlank()) {
            throw IllegalArgumentException(getString(R.string.required_fields_empty))
        }

        if (!isStandalone && track == origTrack &&
            artist == origArtist && album == origAlbum && album != "" && albumArtist == ""
        ) {
            return true
        }

        var validArtist: String? = null
        var validAlbumArtist: String? = ""
        var validTrack: Track? = null

        if (!forceScrobble) {
            if (album.isBlank() && origAlbum.isBlank())
                validTrack =
                    kotlin.runCatching { Track.getInfo(artist, track, Stuff.LAST_KEY) }
                        .getOrNull()
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
        if (validTrack == null && (validArtist == null || validAlbumArtist == null) && !forceScrobble) {
            if (!isStandalone)
                withContext(Dispatchers.Main) {
                    binding.loginCheckbox.text = getString(R.string.force_remembered)
                    binding.loginCheckbox.visibility = View.VISIBLE
                }
            throw IllegalArgumentException(getString(R.string.state_unrecognised_artist))
        } else {
            val lastfmSessKey: String? = prefs.lastfmSessKey
            val lastfmSession = Session.createSession(
                Stuff.LAST_KEY,
                Stuff.LAST_SECRET, lastfmSessKey
            )
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
                (result?.isSuccessful == true && !result.isIgnored)
            ) {
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
                                        //editing just the album or albumArtist is a noop, scrobble again
                                        if (track.equals(origTrack, ignoreCase = true) &&
                                            artist.equals(origArtist, ignoreCase = true)
                                        )
                                            withContext(Dispatchers.IO) {
                                                Track.scrobble(scrobbleData, lastfmSession)
                                            }

                                        val trackObj = Track(track, null, artist).also {
                                            it.playedWhen = Date(timeMillis)
                                            it.album = album
                                        }

                                        withContext(Dispatchers.IO) {
                                            CachedTracksDao.deltaUpdateAll(context!!, trackObj, 1)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // track player
                    arguments?.getString(NLService.B_PACKAGE_NAME)?.let {
                        val scrobbleSource = ScrobbleSource(
                            timeMillis = scrobbleData.timestamp * 1000L,
                            pkg = it
                        )
                        PanoDb.getDb(context!!).getScrobbleSourcesDao().insert(scrobbleSource)
                    }

                    //scrobble everywhere else
                    Scrobblable.getScrobblablesMap(prefs).forEach { (strId, scrobblable) ->
                        if (strId != R.string.lastfm && scrobblable != null)
                            launch { scrobblable.scrobble(scrobbleData) }
                    }

                }
                saveEdit(context!!)
                if (forceScrobble) {
                    val oldSet = prefs.allowedArtists.toSet()
                    prefs.allowedArtists = oldSet + artist
                }
            } else if (result?.isIgnored == true) {
                if (System.currentTimeMillis() - timeMillis < Stuff.LASTFM_MAX_PAST_SCROBBLE)
                    throw IllegalArgumentException(
                        getString(R.string.lastfm) + ": " + getString(R.string.scrobble_ignored)
                    )
                else {
                    success = false
                    withContext(Dispatchers.Main) {
                        MaterialAlertDialogBuilder(context!!)
                            .setMessage(R.string.scrobble_ignored_save_edit)
                            .setPositiveButton(R.string.yes) { _, _ ->
                                launch(Dispatchers.IO) {
                                    saveEdit(activity)
                                    withContext(Dispatchers.Main) {
                                        dismiss()
                                    }
                                }
                            }
                            .setNegativeButton(R.string.no, null)
                            .show()
                    }
                }
            } else {
                Stuff.log("edit scrobble err: $result")
                if (!isStandalone)
                    throw IOException(getString(R.string.network_error))
            }

            if (result?.isIgnored == false &&
                !prefs.regexEditsLearnt &&
                !isStandalone
            ) {
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
                    val suggestedRegexReplacements = dao.performRegexReplace(
                        originalScrobbleData,
                        allPresets,
                        matchedRegexEdits
                    )
                    val replacementsInEdit = dao.performRegexReplace(scrobbleData, allPresets)

                    if (suggestedRegexReplacements.values.sum() > 0 && replacementsInEdit.values.sum() == 0) {
                        withContext(Dispatchers.Main) {
                            val presetName = RegexPresets.getString(
                                context!!,
                                matchedRegexEdits.first().preset!!
                            )

                            MaterialAlertDialogBuilder(context!!)
                                .setMessage(
                                    getString(
                                        R.string.regex_edits_suggestion,
                                        presetName
                                    )
                                )
                                .setPositiveButton(R.string.yes) { _, _ ->
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
                                .setNegativeButton(R.string.no) { _, _ ->
                                    prefs.regexEditsLearnt = true
                                }
                                .show()
                        }
                    }
                }
            }
        }

        if (success) {
            (activity as? MainActivity)?.let {
                it.mainNotifierViewModel.editData.postValue(
                    Track(track, null, album, artist).apply {
                        if (args.getLong(NLService.B_TIME) != 0L)
                            playedWhen = Date(timeMillis)
                    }
                )
            }
            if (args.getLong(NLService.B_TIME) == 0L /* now playing */) {
                context?.sendBroadcast(Intent(NLService.iCANCEL), NLService.BROADCAST_PERMISSION)
            }
        }
        
        return success
    }
}