package com.arn.scrobble.edits

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import coil.imageLoader
import coil.request.ImageRequest
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.databinding.ContentRegexTestBinding
import com.arn.scrobble.ui.PackageName
import com.arn.scrobble.ui.UiUtils.collectLatestLifecycleFlow
import com.arn.scrobble.ui.UiUtils.setupAxisTransitions
import com.arn.scrobble.ui.UiUtils.setupInsets
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.putSingle
import com.google.android.material.chip.Chip
import com.google.android.material.transition.MaterialSharedAxis

class RegexEditsTestFragment : Fragment() {

    private var _binding: ContentRegexTestBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModels<RegexEditsTestVM>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupAxisTransitions(MaterialSharedAxis.X)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ContentRegexTestBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun putScrobbleData() {
        if ((viewModel.hasPkgName.value == true && viewModel.pkgNameSelected.value == null) ||
            (binding.regexTestTrack.text.isNullOrEmpty() &&
                    binding.regexTestAlbum.text.isNullOrEmpty() &&
                    binding.regexTestArtist.text.isNullOrEmpty() &&
                    binding.regexTestAlbumArtist.text.isNullOrEmpty())
        ) {
            viewModel.setScrobbleData(null)
            return
        }

        val sd = ScrobbleData(
            track = binding.regexTestTrack.text.toString(),
            album = binding.regexTestAlbum.text.toString(),
            artist = binding.regexTestArtist.text.toString(),
            albumArtist = binding.regexTestAlbumArtist.text.toString(),
            timestamp = 0,
            duration = null,
        )

        viewModel.setScrobbleData(sd)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.root.setupInsets()

        val extractTextWatcher = object : TextWatcher {
            override fun onTextChanged(cs: CharSequence, arg1: Int, arg2: Int, arg3: Int) {
            }

            override fun beforeTextChanged(s: CharSequence, arg1: Int, arg2: Int, arg3: Int) {
            }

            override fun afterTextChanged(editable: Editable) {
                putScrobbleData()
            }
        }

        binding.regexTestTrack.addTextChangedListener(extractTextWatcher)
        binding.regexTestAlbum.addTextChangedListener(extractTextWatcher)
        binding.regexTestArtist.addTextChangedListener(extractTextWatcher)
        binding.regexTestAlbumArtist.addTextChangedListener(extractTextWatcher)

        binding.regexTestPackage.setOnClickListener {
            val existingPkgName = viewModel.pkgNameSelected.value
            val pkgNameArr = if (existingPkgName != null) arrayOf(existingPkgName) else arrayOf()
            val args = bundleOf(
                Stuff.ARG_ALLOWED_PACKAGES to pkgNameArr,
                Stuff.ARG_SINGLE_CHOICE to true,
            )
            findNavController().navigate(R.id.appListFragment, args)
        }

        binding.regexTestPackage.setOnCloseIconClickListener {
            viewModel.setPkgName(null)
        }

        fun onAppIconResult(drawable: Drawable?) {
            binding.regexTestPackage.chipIcon = drawable
        }

        setFragmentResultListener(Stuff.ARG_ALLOWED_PACKAGES) { key, bundle ->
            if (key == Stuff.ARG_ALLOWED_PACKAGES) {
                val pkgName = bundle.getStringArray(key)?.firstOrNull()
                viewModel.setPkgName(pkgName)
            }
        }

        collectLatestLifecycleFlow(viewModel.regexMatches) {
            if (it == null) {
                binding.regexTestErr.isVisible = true
                binding.regexTestMatchesViewgroup.isVisible = false
                binding.regexTestErr.text = getString(R.string.required_fields_empty)
                return@collectLatestLifecycleFlow
            }

            if (it.values.all { it.isEmpty() }) {
                binding.regexTestErr.isVisible = true
                binding.regexTestMatchesViewgroup.isVisible = false
                binding.regexTestErr.text = resources.getQuantityString(
                    R.plurals.num_matches,
                    0,
                    0
                )
                return@collectLatestLifecycleFlow
            }

            binding.regexTestErr.isVisible = false
            binding.regexTestMatchesViewgroup.isVisible = true

            it.forEach { (field, regexEdits) ->

                val count = regexEdits.size
                val countString = if (count > 0)
                    " (${
                        resources.getQuantityString(
                            R.plurals.num_matches,
                            count,
                            count
                        )
                    })"
                else
                    ""
                val sd = viewModel.scrobbleData.value ?: return@forEach
                when (field) {
                    NLService.B_ARTIST -> {
                        binding.regexTestArtistMatches.text = sd.artist + countString
                    }

                    NLService.B_ALBUM -> {
                        binding.regexTestAlbumMatches.text = sd.album + countString
                    }

                    NLService.B_ALBUM_ARTIST -> {
                        binding.regexTestAlbumArtistMatches.text = sd.albumArtist + countString
                    }

                    NLService.B_TRACK -> {
                        binding.regexTestTrackMatches.text = sd.track + countString
                    }

                    else -> {
                        throw IllegalArgumentException()
                    }
                }

            }

            val matchedRegexEdits = it.values.flatten().toSet()

            binding.regexTestMatchesChipgroup.removeAllViews()

            matchedRegexEdits
                .forEach { regexEdit ->
                    val chipText = if (regexEdit.preset != null)
                        getString(
                            R.string.edit_preset_name,
                            RegexPresets.getString(regexEdit.preset!!)
                        )
                    else
                        (regexEdit.name ?: regexEdit.pattern.toString())

                    val chip = Chip(requireContext()).apply {
                        text = chipText
                        maxWidth =
                            resources.getDimensionPixelSize(R.dimen.regex_test_chip_max_width)
                        ellipsize = TextUtils.TruncateAt.END
                        isCloseIconVisible = false
                        setOnClickListener {
                            val args = Bundle().putSingle(regexEdit)
                            findNavController().navigate(R.id.regexEditsAddFragment, args)
                        }

                    }
                    binding.regexTestMatchesChipgroup.addView(chip)
                }


        }

        collectLatestLifecycleFlow(viewModel.hasPkgName) {
            binding.regexTestPackage.isVisible = it
        }

        collectLatestLifecycleFlow(viewModel.pkgNameSelected) {
            if (it == null) {
                binding.regexTestPackage.text = getString(R.string.choose_an_app)
                binding.regexTestPackage.setChipIconResource(R.drawable.vd_add)
                binding.regexTestPackage.isCloseIconVisible = false
            } else {
                val ai = runCatching { viewModel.pm.getApplicationInfo(it, 0) }.getOrNull()

                if (ai != null) {
                    binding.regexTestPackage.text = viewModel.pm.getApplicationLabel(ai)

                    val request = ImageRequest.Builder(requireContext())
                        .data(PackageName(it))
                        .error(R.drawable.vd_apps)
                        .allowHardware(false)
                        .target(
                            onSuccess = ::onAppIconResult,
                            onError = ::onAppIconResult
                        )
                        .build()

                    requireContext().imageLoader.enqueue(request)


                } else {
                    binding.regexTestPackage.text = it
                    binding.regexTestPackage.setChipIconResource(R.drawable.vd_apps)
                }

                binding.regexTestPackage.isCloseIconVisible = true
            }

            putScrobbleData()
        }
    }
}