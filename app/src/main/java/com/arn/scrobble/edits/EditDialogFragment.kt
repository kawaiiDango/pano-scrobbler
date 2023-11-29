package com.arn.scrobble.edits

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.InputType
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
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.arn.scrobble.App
import com.arn.scrobble.MainActivity
import com.arn.scrobble.MainDialogActivity
import com.arn.scrobble.MainNotifierViewModel
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.ScrobbleIgnored
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.LastFm
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.listenbrainz.ListenBrainz
import com.arn.scrobble.db.CachedTracksDao
import com.arn.scrobble.db.DirtyUpdate
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.RegexEdit
import com.arn.scrobble.db.RegexEditsDao.Companion.performRegexReplace
import com.arn.scrobble.db.ScrobbleSource
import com.arn.scrobble.db.SimpleEdit
import com.arn.scrobble.db.SimpleEditsDao.Companion.insertReplaceLowerCase
import com.arn.scrobble.onboarding.LoginFragment
import com.arn.scrobble.onboarding.LoginFragmentArgs
import com.arn.scrobble.utils.Stuff
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


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

        binding.loginTextfield1.editText!!.setText(args.data.track)
        binding.loginTextfield2.editText!!.setText(args.data.album)
        binding.loginTextfieldLast.editText!!.setText(args.data.artist)

        args.data.albumArtist?.let {
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

        if (args.data.albumArtist.isNullOrEmpty()) {

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

    private fun showRegexRecommendation(regexRecommendation: RegexEdit) {
        val presetName = RegexPresets.getString(regexRecommendation.preset!!)
        val navController = findNavController()
        val activity = activity ?: return

        MaterialAlertDialogBuilder(activity)
            .setMessage(
                getString(
                    R.string.regex_edits_suggestion,
                    presetName
                )
            )
            .setPositiveButton(R.string.yes) { _, _ ->
                val args = bundleOf(Stuff.ARG_SHOW_DIALOG to true)

                if (activity is MainActivity)
                    navController.navigate(R.id.regexEditsFragment, args)
                else if (activity is MainDialogActivity) {
                    NavDeepLinkBuilder(activity)
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

    override suspend fun validateAsync(): Boolean {
        val track = binding.loginTextfield1.editText!!.text.toString().trim()
        val origTrack = args.data.track
        var album = binding.loginTextfield2.editText!!.text.toString().trim()
        var albumArtist = binding.loginTextfieldLast2.editText!!.text.toString().trim()
        val origAlbum = args.data.album ?: ""
        val artist = binding.loginTextfieldLast.editText!!.text.toString().trim()
        val origArtist = args.data.artist
        val msid = args.msid
        val timeSecs = args.data.timestamp
        val isNowPlaying = args.data.timestamp > 0

        val fetchAlbumAndAlbumArtist = album.isBlank() && origAlbum.isBlank() && prefs.fetchAlbum
        val rescrobbleRequired = !isNowPlaying && (fetchAlbumAndAlbumArtist ||
                (track.equals(origTrack, ignoreCase = true) &&
                        artist.equals(origArtist, ignoreCase = true)
                        && (album != origAlbum || albumArtist.isNotBlank())))
        var success = true
        val scrobbleData = args.data
        val lastfmScrobblable = Scrobblables.byType(AccountType.LASTFM)
        val lastfmScrobbleResult: Result<ScrobbleIgnored>

        val origTrackObj = Track(
            origTrack,
            null,
            Artist(origArtist),
            date = timeSecs,
            msid = msid
        )

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

        if (isNowPlaying && track == origTrack &&
            artist == origArtist && album == origAlbum && albumArtist == "" &&
            !(album == "" && prefs.fetchAlbum)
        ) {
            return true
        }

        if (fetchAlbumAndAlbumArtist) {
            val newTrack = Track(track, null, Artist(artist))

            val fetchedTrack = Requesters.lastfmUnauthedRequester
                .getInfo(newTrack)
                .getOrNull()

            if (album.isBlank() && fetchedTrack?.album != null) {
                album = fetchedTrack.album.name
                scrobbleData.album = fetchedTrack.album.name
                withContext(Dispatchers.Main) {
                    binding.loginTextfield2.editText!!.setText(fetchedTrack.album.name)
                }
            }
            if (albumArtist.isBlank() && fetchedTrack?.album?.artist != null) {
                albumArtist = fetchedTrack.album.artist.name
                scrobbleData.albumArtist = albumArtist
                withContext(Dispatchers.Main) {
                    binding.loginTextfieldLast2.editText!!.setText(albumArtist)
                }
            }
        }

        // edit lastfm first
        if (lastfmScrobblable != null) {
            lastfmScrobbleResult = lastfmScrobblable.scrobble(scrobbleData)
            if (lastfmScrobbleResult.map { it.ignored }.getOrNull() == true) {
                if (System.currentTimeMillis() - timeSecs < Stuff.LASTFM_MAX_PAST_SCROBBLE)
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
                if (!isNowPlaying) {
                    // The user might submit the edit after it has been scrobbled, so delete anyways
                    val deleteSucc = lastfmScrobblable.delete(origTrackObj)
                    if (deleteSucc)
                        CachedTracksDao.deltaUpdateAll(origTrackObj, -1, DirtyUpdate.BOTH)
                }
                if (rescrobbleRequired)
                    lastfmScrobblable.scrobble(scrobbleData)
//                }

                if (isNowPlaying)
                    lastfmScrobblable.updateNowPlaying(scrobbleData)
            }

            val _artist = Artist(artist)

            val trackObj = Track(
                track,
                Album(album, _artist),
                _artist,
                date = timeSecs
            )

            CachedTracksDao.deltaUpdateAll(trackObj, 1, DirtyUpdate.BOTH)
        }


        // scrobble everywhere else (delete first)
        Scrobblables.all
            .filter { it !is LastFm }
            .forEach {
                if (!isNowPlaying)
                    it.delete(origTrackObj)
                // ListenBrainz cannot have two scrobbles with the same timestamp and delete is not immediate
                // so add 1 sec
                if (it is ListenBrainz)
                    it.scrobble(scrobbleData.copy(timestamp = scrobbleData.timestamp + 1))
                else
                    it.scrobble(scrobbleData)
                if (isNowPlaying)
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
        if (success && !prefs.regexEditsLearnt) {
            val dao = PanoDb.db.getRegexEditsDao()

            val presetsAvailable = (RegexPresets.presetKeys - dao.allPresets()
                .map { it.preset }.toSet())
                .mapIndexed { index, key ->
                    RegexPresets.getPossiblePreset(
                        RegexEdit(order = index, preset = key)
                    )
                }

            if (presetsAvailable.isNotEmpty()) {
                val suggestedRegexReplacements = dao.performRegexReplace(
                    scrobbleData,
                    null,
                    presetsAvailable,
                )

                val firstSuggestion =
                    suggestedRegexReplacements.values.firstOrNull { it.isNotEmpty() }?.firstOrNull()

                val replacementsInEdit =
                    dao.performRegexReplace(scrobbleData, null, presetsAvailable)

                if (firstSuggestion != null && replacementsInEdit.values.all { it.isEmpty() }) {
                    withContext(Dispatchers.Main) {
                        showRegexRecommendation(firstSuggestion)
                    }
                }
            }
        }

        if (success) {
            // notify the edit
            (activity as? MainActivity)?.let {
                val _artist = Artist(artist)
                mainNotifierViewModel.notifyEdit(
                    Track(
                        track,
                        Album(album, _artist),
                        _artist,
                        date = if (!isNowPlaying) null else timeSecs,
                    )
                )
            }
            if (!isNowPlaying) {
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