package com.arn.scrobble.edits

import android.app.Dialog
import android.content.DialogInterface
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
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDeepLinkBuilder
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.arn.scrobble.App
import com.arn.scrobble.LoginFragment
import com.arn.scrobble.LoginFragmentArgs
import com.arn.scrobble.MainActivity
import com.arn.scrobble.MainDialogActivity
import com.arn.scrobble.MainNotifierViewModel
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.db.CachedTracksDao
import com.arn.scrobble.db.DirtyUpdate
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.RegexEdit
import com.arn.scrobble.db.RegexEditsDao.Companion.performRegexReplace
import com.arn.scrobble.db.ScrobbleSource
import com.arn.scrobble.db.SimpleEdit
import com.arn.scrobble.db.SimpleEditsDao.Companion.insertReplaceLowerCase
import com.arn.scrobble.scrobbleable.AccountType
import com.arn.scrobble.scrobbleable.Lastfm
import com.arn.scrobble.scrobbleable.ListenBrainz
import com.arn.scrobble.scrobbleable.Scrobblables
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import de.umass.lastfm.Track
import de.umass.lastfm.scrobble.ScrobbleData
import de.umass.lastfm.scrobble.ScrobbleResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date


class EditDialogFragment : LoginFragment() {

    override val checksLogin = false
    private val args by navArgs<EditDialogFragmentArgs>()
    private val mainNotifierViewModel by activityViewModels<MainNotifierViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        lockScrobble(true)

        val placeholderTextArgs = LoginFragmentArgs.Builder(
            App.context.getString(R.string.edit),
            App.context.getString(R.string.artist)
        ).apply {
            textField1 = App.context.getString(R.string.track)
            textField2 = App.context.getString(R.string.album_optional)
        }
            .build()
            .toBundle()

        requireArguments().putAll(placeholderTextArgs)

        super.onCreateView(layoutInflater, null, null)

        showsDialog = true

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

        args.track?.let {
            binding.loginTextfield1.editText!!.setText(it)
        }
        args.album?.let {
            binding.loginTextfield2.editText!!.setText(it)
        }
        args.artist?.let {
            binding.loginTextfieldLast.editText!!.setText(it)
        }
        args.albumArtist?.let {
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

        if (args.albumArtist.isNullOrEmpty()) {

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

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        lockScrobble(false)
    }

    override suspend fun validateAsync(): Boolean {
        val track = binding.loginTextfield1.editText!!.text.toString().trim()
        val origTrack = args.track ?: ""
        var album = binding.loginTextfield2.editText!!.text.toString().trim()
        var albumArtist = binding.loginTextfieldLast2.editText!!.text.toString().trim()
        val origAlbum = args.album ?: ""
        val artist = binding.loginTextfieldLast.editText!!.text.toString().trim()
        val origArtist = args.artist ?: ""
        val msid = args.msid
        val timeMillis = args.timeMillis
        val fetchAlbumAndAlbumArtist = album.isBlank() && origAlbum.isBlank() && prefs.fetchAlbum
        val rescrobbleRequired = !args.nowPlaying && (fetchAlbumAndAlbumArtist ||
                (track.equals(origTrack, ignoreCase = true) &&
                        artist.equals(origArtist, ignoreCase = true)
                        && (album != origAlbum || albumArtist.isNotBlank())))
        var success = true

        val origTrackObj = Track(origTrack, null, origArtist).apply {
            playedWhen = Date(timeMillis)
            this.msid = msid
        }

        fun saveEdit() {
            if (!(track == origTrack && artist == origArtist && album == origAlbum && albumArtist == "")) {
                val dao = PanoDb.db.getSimpleEditsDao()
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

        if (!args.nowPlaying && track == origTrack &&
            artist == origArtist && album == origAlbum && albumArtist == "" &&
            !(album == "" && prefs.fetchAlbum)
        ) {
            return true
        }


        val scrobbleData = ScrobbleData().also {
            it.artist = artist
            it.track = track
            it.timestamp = (timeMillis / 1000).toInt()
            it.album = album
            it.albumArtist = albumArtist
        }

        if (fetchAlbumAndAlbumArtist) {
            val fetchedTrack = kotlin.runCatching { Track.getInfo(artist, track, Stuff.LAST_KEY) }
                .getOrNull()
            if (album.isBlank() && fetchedTrack?.album != null) {
                album = fetchedTrack.album
                scrobbleData.album = fetchedTrack.album
                withContext(Dispatchers.Main) {
                    binding.loginTextfield2.editText!!.setText(fetchedTrack.album)
                }
            }
            if (albumArtist.isBlank() && fetchedTrack?.albumArtist != null) {
                albumArtist = fetchedTrack.albumArtist
                scrobbleData.albumArtist = fetchedTrack.albumArtist
                withContext(Dispatchers.Main) {
                    binding.loginTextfieldLast2.editText!!.setText(fetchedTrack.albumArtist)
                }
            }
        }

        val lastfmScrobblable = Scrobblables.byType(AccountType.LASTFM)
        var lastfmScrobbleResult: ScrobbleResult? = null
        // edit lastfm first
        if (lastfmScrobblable != null) {
            lastfmScrobbleResult = lastfmScrobblable.scrobble(scrobbleData)
            if (lastfmScrobbleResult.isIgnored) {
                if (System.currentTimeMillis() - timeMillis < Stuff.LASTFM_MAX_PAST_SCROBBLE)
                    throw IllegalArgumentException(
                        getString(R.string.lastfm) + ": " + getString(R.string.scrobble_ignored)
                    )
                else {
                    success = false
                    withContext(Dispatchers.Main) {
                        MaterialAlertDialogBuilder(requireContext())
                            .setMessage(R.string.scrobble_ignored_save_edit)
                            .setPositiveButton(R.string.yes) { _, _ ->
                                lifecycleScope.launch(Dispatchers.IO) {
                                    saveEdit()
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
                if (!args.nowPlaying) {
                    // The user might submit the edit after it has been scrobbled, so delete anyways
                    val deleteSucc = lastfmScrobblable.delete(origTrackObj)
                    if (deleteSucc)
                        CachedTracksDao.deltaUpdateAll(origTrackObj, -1, DirtyUpdate.BOTH)
                }
                if (rescrobbleRequired)
                    lastfmScrobblable.scrobble(scrobbleData)
//                }

                if (args.nowPlaying)
                    lastfmScrobblable.updateNowPlaying(scrobbleData)
            }

            val trackObj = Track(track, null, artist).also {
                it.playedWhen = Date(timeMillis)
                it.album = album
            }

            CachedTracksDao.deltaUpdateAll(trackObj, 1, DirtyUpdate.BOTH)
        }


        // scrobble everywhere else (delete first)
        Scrobblables.all
            .filter { it !is Lastfm }
            .forEach {
                if (!args.nowPlaying)
                    it.delete(origTrackObj)
                // ListenBrainz cannot have two scrobbles with the same timestamp and delete is not immediate
                // so add 1 sec
                if (it is ListenBrainz)
                    it.scrobble(ScrobbleData(scrobbleData).also { it.timestamp += 1 })
                else
                    it.scrobble(scrobbleData)
                if (args.nowPlaying)
                    it.updateNowPlaying(scrobbleData)
            }

        // track player
        args.packageName?.let {
            val scrobbleSource =
                ScrobbleSource(timeMillis = scrobbleData.timestamp * 1000L, pkg = it)
            PanoDb.db.getScrobbleSourcesDao().insert(scrobbleSource)
        }

        saveEdit()

        // suggest regex edit
        if (lastfmScrobbleResult == null || !lastfmScrobbleResult.isIgnored &&
            !prefs.regexEditsLearnt
        ) {
            val originalScrobbleData = ScrobbleData().apply {
                this.artist = origArtist
                this.album = origAlbum
                this.track = origTrack
            }

            val dao = PanoDb.db.getRegexEditsDao()
            val existingRegexReplacements = dao.performRegexReplace(originalScrobbleData)

            if (existingRegexReplacements.values.sum() == 0) {
                val allPresets = RegexPresets.presetKeys.mapIndexed { index, key ->
                    RegexPresets.getPossiblePreset(
                        RegexEdit(order = index, preset = key)
                    )
                }
                val matchedRegexEdits = mutableListOf<RegexEdit>()
                val suggestedRegexReplacements = dao.performRegexReplace(
                    originalScrobbleData,
                    null,
                    allPresets,
                    matchedRegexEdits
                )
                val replacementsInEdit = dao.performRegexReplace(scrobbleData, null, allPresets)

                if (suggestedRegexReplacements.values.sum() > 0 && replacementsInEdit.values.sum() == 0) {
                    withContext(Dispatchers.Main) {
                        val presetName = RegexPresets.getString(matchedRegexEdits.first().preset!!)

                        MaterialAlertDialogBuilder(requireContext())
                            .setMessage(
                                getString(
                                    R.string.regex_edits_suggestion,
                                    presetName
                                )
                            )
                            .setPositiveButton(R.string.yes) { _, _ ->
                                dismiss()
                                val args = bundleOf(Stuff.ARG_SHOW_DIALOG to true)

                                if (activity is MainActivity)
                                    findNavController().navigate(R.id.regexEditsFragment, args)
                                else if (activity is MainDialogActivity) {
                                    NavDeepLinkBuilder(requireContext())
                                        .setComponentName(MainActivity::class.java)
                                        .setGraph(R.navigation.nav_graph)
                                        .setDestination(R.id.regexEditsFragment)
                                        .setArguments(args)
                                        .createPendingIntent()
                                        .send()
                                }
                            }
                            .setNegativeButton(R.string.no) { _, _ ->
                                prefs.regexEditsLearnt = true
                            }
                            .show()
                    }
                }
            }
        }

        if (success) {
            // notify the edit
            (activity as? MainActivity)?.let {
                mainNotifierViewModel.editData.postValue(
                    Track(track, null, album, artist).apply {
                        if (!args.nowPlaying)
                            playedWhen = Date(timeMillis)
                    }
                )
            }
            if (args.nowPlaying) {
                lockScrobble(false)
                context?.sendBroadcast(
                    Intent(NLService.iCANCEL)
                        .putExtra(NLService.B_HASH, args.hash)
                        .setPackage(requireContext().packageName),
                    NLService.BROADCAST_PERMISSION
                )
            }
        }

        return success
    }

    private fun lockScrobble(lock: Boolean) {
        if (args.hash == -1) return

        // do not scrobble until the dialog is dismissed

        val intent = Intent(NLService.iSCROBBLE_SUBMIT_LOCK_S)
            .setPackage(requireContext().packageName)
            .putExtra(NLService.B_LOCKED, lock)
            .putExtra(NLService.B_HASH, args.hash)

        requireContext().sendBroadcast(intent, NLService.BROADCAST_PERMISSION)
    }
}