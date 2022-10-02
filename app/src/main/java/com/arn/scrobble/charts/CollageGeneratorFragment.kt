package com.arn.scrobble.charts

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.core.content.FileProvider
import androidx.core.graphics.ColorUtils
import androidx.core.view.drawToBitmap
import androidx.core.view.setPadding
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import coil.imageLoader
import coil.load
import coil.request.ImageRequest
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.LFMRequester
import com.arn.scrobble.MainNotifierViewModel
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.getSingle
import com.arn.scrobble.Stuff.mapConcurrently
import com.arn.scrobble.databinding.DialogCollageGeneratorBinding
import com.arn.scrobble.databinding.FooterCollageBinding
import com.arn.scrobble.databinding.GridItemCollageBinding
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.ui.UiUtils.expandIfNeeded
import com.arn.scrobble.ui.UiUtils.getTintedDrawable
import com.arn.scrobble.ui.UiUtils.toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textview.MaterialTextView
import de.umass.lastfm.Album
import de.umass.lastfm.Artist
import de.umass.lastfm.MusicEntry
import de.umass.lastfm.PaginatedResult
import de.umass.lastfm.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream


class CollageGeneratorFragment : BottomSheetDialogFragment() {

    private var _binding: DialogCollageGeneratorBinding? = null
    private val binding get() = _binding!!
    private val activityViewModel by activityViewModels<MainNotifierViewModel>()
    private val timePeriod get() = arguments!!.getSingle<TimePeriod>()!!
    private val prefs by lazy { MainPrefs(context!!) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogCollageGeneratorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val minSize = 3
        val maxSize = 6
        val sizes = (minSize..maxSize).map { "$it" }.toTypedArray()
        val types = arrayOf(
            getString(R.string.edit_all),
            getString(R.string.top_artists),
            getString(R.string.top_albums),
            getString(R.string.top_tracks),
        )

        binding.collageTypeText.setSimpleItems(types)
        binding.collageTypeText.setText(types[arguments!!.getInt(Stuff.ARG_TYPE)], false)

        binding.collageSizeText.setSimpleItems(sizes)
        val savedIndex = prefs.collageSize - minSize
        binding.collageSizeText.setText(sizes[savedIndex], false)

        binding.collageIncludeCaptions.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.collageSkipMissingImages.isEnabled = true
            } else {
                binding.collageSkipMissingImages.isEnabled = false
                binding.collageSkipMissingImages.isChecked = true
            }
        }

        binding.collageSkipMissingImages.isChecked = prefs.collageSkipMissing
        binding.collageIncludeUsername.isChecked = prefs.collageUsername
        binding.collageIncludeCaptions.isChecked = prefs.collageCaptions
        binding.collageIncludeText.isChecked = prefs.collageText

        binding.collageGenerateButton.setOnClickListener { v ->

            binding.collageGenerateProgress.show()
            v.visibility = View.INVISIBLE

            prefs.collageSkipMissing = binding.collageSkipMissingImages.isChecked
            prefs.collageCaptions = binding.collageIncludeCaptions.isChecked
            prefs.collageUsername = binding.collageIncludeUsername.isChecked
            prefs.collageText = binding.collageIncludeText.isChecked
            prefs.collageSize =
                binding.collageSizeText.text.toString().let { sizes.indexOf(it) + minSize }

            val type = binding.collageTypeText.text.toString().let { types.indexOf(it) }

            viewLifecycleOwner.lifecycleScope.launch {
                if (type == 0) { // digest
                    val results = listOf(
                        Stuff.TYPE_ARTISTS,
                        Stuff.TYPE_ALBUMS,
                        Stuff.TYPE_TRACKS
                    ).mapConcurrently(3) {
                        kotlin.runCatching { fetchCharts(it, timePeriod) }
                    }
                    if (results.all { it.isSuccess && it.getOrNull()?.pageResults != null }) {
                        drawCollage(
                            Stuff.TYPE_ALL,
                            results.map { it.getOrNull()!!.pageResults.toList() })
                    } else {
                        context!!.toast(R.string.network_error)
                    }
                } else { // normal
                    val result = kotlin.runCatching { fetchCharts(type, timePeriod) }
                    if (result.isSuccess && result.getOrNull()?.pageResults?.isNotEmpty() == true) {
                        drawCollage(type, listOf(result.getOrNull()!!.pageResults.toList()))
                    } else {
                        context!!.toast(R.string.network_error)
                    }
                }
            }
        }
    }

    private suspend fun fetchCharts(type: Int, timePeriod: TimePeriod) =
        withContext(Dispatchers.IO) {
            LFMRequester(context!!, viewLifecycleOwner.lifecycleScope)
                .execHere<PaginatedResult<out MusicEntry>> {
                    getCharts(
                        type,
                        timePeriod,
                        1,
                        activityViewModel.currentUser.name
                    )
                }
        }

    private suspend fun drawCollage(type: Int, musicEntriesAll: List<List<MusicEntry>>) {
        val collageSize = prefs.collageSize
        val textScaler = collageSize / 5f
        val isDigest = type == Stuff.TYPE_ALL

        val collageRoot = LinearLayout(context, null, R.style.Theme_Material3_Dark).apply {
            orientation = LinearLayout.VERTICAL
        }
        val collageFooter = FooterCollageBinding.inflate(layoutInflater, collageRoot, false)

        fun createAddImagesContainer() =
            ChipGroup(context).apply {
                collageRoot.addView(this)
                updateLayoutParams<LinearLayout.LayoutParams> {
                    width = LinearLayout.LayoutParams.MATCH_PARENT
                    height = LinearLayout.LayoutParams.WRAP_CONTENT
                }
                setChipSpacing(0)
            }

        fun createAddHeader(headerText: String) =
            MaterialTextView(context!!).apply {
                collageRoot.addView(this)
                updateLayoutParams<LinearLayout.LayoutParams> {
                    width = LinearLayout.LayoutParams.MATCH_PARENT
                    height = LinearLayout.LayoutParams.WRAP_CONTENT
                }
                setTextSize(TypedValue.COMPLEX_UNIT_PX, 50f * textScaler)
                setPadding(16)
                text = headerText
                setTextColor(ColorUtils.setAlphaComponent(0xffffff, 200))
            }

        var text: String? = null

        // prepare share title
        var shareTitle = timePeriod.name
        if (prefs.collageUsername)
            shareTitle += " • ${activityViewModel.currentUser.name}"

        if (!isDigest) {
            val topType = when (type) {
                Stuff.TYPE_ARTISTS -> getString(R.string.top_artists)
                Stuff.TYPE_ALBUMS -> getString(R.string.top_albums)
                Stuff.TYPE_TRACKS -> getString(R.string.top_tracks)
                else -> throw IllegalArgumentException("Unknown type: $type")
            }
            shareTitle = "$topType • $shareTitle"
        }

        shareTitle += "\n\n"

        val shareSig = if (!prefs.proStatus) "\n\n" + getString(R.string.share_sig) else ""

        // fetch images
        binding.collageGenerateProgress.isIndeterminate = false

        if (isDigest) {
            binding.collageGenerateProgress.max = 3
            val musicEntriesAllPlaced = mutableListOf<MusicEntry>()
            musicEntriesAll.forEach { musicEntries ->
                if (musicEntries.isEmpty())
                    return@forEach

                binding.collageGenerateProgress.max = collageSize * 3

                val headerText = when (musicEntries.first()) {
                    is Artist -> getString(R.string.top_artists)
                    is Album -> getString(R.string.top_albums)
                    is Track -> getString(R.string.top_tracks)
                    else -> throw IllegalArgumentException("Unknown type: $type")
                }
                createAddHeader(headerText)

                val imagesContainer = createAddImagesContainer()
                musicEntriesAllPlaced += fetchImages(musicEntries, imagesContainer, collageSize)

            }
            text = shareTitle + createDigestShareText(musicEntriesAll) + shareSig
        } else {
            val imagesContainer = createAddImagesContainer()
            binding.collageGenerateProgress.max = collageSize * collageSize
            val musicEntriesPlaced =
                fetchImages(musicEntriesAll.first(), imagesContainer, collageSize * collageSize)
            if (prefs.collageText)
                text = shareTitle + createShareText(musicEntriesPlaced) + shareSig
        }


        prepareFooter(collageFooter, type, textScaler)
        collageRoot.addView(collageFooter.root)
        val uri = getCollageFileUri(collageRoot)
        withContext(Dispatchers.Main) {
            shareImageUri(uri, text)
        }

        if (BuildConfig.DEBUG)
            binding.collagePreview.load(uri)
    }

    private fun createDigestShareText(musicEntriesAll: List<List<MusicEntry>>): String {
        val shareTextList = mutableListOf<String>()
        val resultsMap = mapOf(
            R.string.top_artists to musicEntriesAll[0],
            R.string.top_albums to musicEntriesAll[1],
            R.string.top_tracks to musicEntriesAll[2],
        )
        resultsMap.forEach { (titleRes, musicEntries) ->

            val title = getString(titleRes)
            val text = musicEntries.take(3).joinToString { it.name }
            shareTextList += "$title:\n$text"
        }

        return shareTextList.joinToString("\n")
    }

    private suspend fun getCollageFileUri(collageRoot: LinearLayout): Uri {
        val imageDimensionPx = 300
        val totalWidth = imageDimensionPx * prefs.collageSize

        val collageFile = File(context!!.filesDir, "collage.jpg")
        val specHeight = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val specWidth = View.MeasureSpec.makeMeasureSpec(totalWidth, View.MeasureSpec.EXACTLY)
        collageRoot.measure(specWidth, specHeight)
        collageRoot.layout(
            collageRoot.left,
            collageRoot.top,
            collageRoot.measuredWidth,
            collageRoot.measuredHeight
        )

        withContext(Dispatchers.IO) {
            FileOutputStream(collageFile).use { fos ->
                collageRoot.drawToBitmap().compress(Bitmap.CompressFormat.JPEG, 95, fos)
            }
        }
        return FileProvider.getUriForFile(context!!, "com.arn.scrobble.fileprovider", collageFile)
    }

    private fun prepareFooter(collageFooter: FooterCollageBinding, type: Int, textScaler: Float) {
        if (prefs.collageUsername) {
            collageFooter.collageFooterUsername.visibility = View.VISIBLE
            collageFooter.collageUsernameImage.visibility = View.VISIBLE
            collageFooter.collageFooterUsername.text = activityViewModel.currentUser.name
        } else {
            collageFooter.collageFooterUsername.visibility = View.GONE
            collageFooter.collageUsernameImage.visibility = View.GONE
        }

        // todo finalize
        if (prefs.proStatus && !BuildConfig.DEBUG) {
            collageFooter.collageFooterBrandingImage.visibility = View.GONE
            collageFooter.collageFooterBrandingText.visibility = View.GONE
        } else {
            collageFooter.collageFooterBrandingText.text =
                getString(R.string.app_name).replace(" ", "\n")
        }

        // populate
        fun shorterString(@StringRes strResTypical: Int, @StringRes strResShort: Int): String {
            val str1 = getString(strResTypical)
            val str2 = getString(strResShort)

            return if (str1.length < 20)
                str1
            else
                str2
        }

        collageFooter.collageFooterDuration.text =
            timePeriod.name.ifEmpty { timePeriod.period!!.name }
        collageFooter.collageFooterType.text = when (type) {
            Stuff.TYPE_ALL -> ""
            Stuff.TYPE_ARTISTS -> shorterString(R.string.top_artists, R.string.artists)
            Stuff.TYPE_ALBUMS -> shorterString(R.string.top_albums, R.string.albums)
            Stuff.TYPE_TRACKS -> shorterString(R.string.top_tracks, R.string.tracks)
            else -> throw IllegalArgumentException("Unknown type: $type")
        }

        val typeDrawableRes = when (type) {
            Stuff.TYPE_ALL -> 0
            Stuff.TYPE_ARTISTS -> R.drawable.vd_mic
            Stuff.TYPE_ALBUMS -> R.drawable.vd_album
            Stuff.TYPE_TRACKS -> R.drawable.vd_note
            else -> throw IllegalArgumentException("Unknown type: $type")
        }

        collageFooter.collageTypeImage.setImageResource(typeDrawableRes)

        //scale
        arrayOf(
            collageFooter.collageFooterBrandingText,
            collageFooter.collageFooterType,
            collageFooter.collageFooterDuration,
            collageFooter.collageFooterUsername,
        ).forEach {
            it.setTextSize(TypedValue.COMPLEX_UNIT_PX, it.textSize * textScaler)
        }

    }

    private suspend fun fetchImages(
        musicEntries: List<MusicEntry>,
        collageImagesBinding: ChipGroup,
        limit: Int
    ): MutableList<MusicEntry> {
        val musicEntriesPlaced = mutableListOf<MusicEntry>()

        for (entry in musicEntries) {
            if (musicEntriesPlaced.size >= limit)
                break

            var error = false
            val request = ImageRequest.Builder(context!!).apply {
                data(entry)
                allowHardware(false)
                crossfade(false)
                if (prefs.collageSkipMissing)
                    listener(
                        onError = { _, _ -> error = true },
                    )
                else
                    error(
                        context!!.getTintedDrawable(
                            R.drawable.vd_wave_simple_filled, entry.name.hashCode()
                        )
                    )
                placeholder(null)
            }.build()

            val artworkDrawable = withContext(Dispatchers.IO) {
                context!!.imageLoader.execute(request).drawable
            }
            if (prefs.collageSkipMissing && (error || artworkDrawable == null))
                continue

            binding.collageGenerateProgress.progress++
            musicEntriesPlaced += entry

            val gridItemBinding = GridItemCollageBinding.inflate(
                layoutInflater,
                collageImagesBinding,
                true
            )
            gridItemBinding.collageImage.setImageDrawable(artworkDrawable)

            if (prefs.collageCaptions) {
                when (entry) {
                    is Artist -> {
                        gridItemBinding.collageCaption.text = entry.name
                        gridItemBinding.collageSubcaption.visibility = View.GONE
                    }

                    is Album -> {
                        gridItemBinding.collageCaption.text = entry.name
                        gridItemBinding.collageSubcaption.text = entry.artist
                    }

                    is Track -> {
                        gridItemBinding.collageCaption.text = entry.name
                        gridItemBinding.collageSubcaption.text = entry.artist
                    }
                }
                gridItemBinding.collagePlayCount.text = resources.getQuantityString(
                    R.plurals.num_scrobbles_noti, entry.playcount, entry.playcount
                )
            }
        }
        return musicEntriesPlaced
    }

    private fun createShareText(musicEntries: List<MusicEntry>): String {
        if (musicEntries.isEmpty()) return ""
        var pos = 1
        val list = musicEntries.take(10).joinToString(separator = "\n") {
            when (it) {
                is Track -> getString(
                    R.string.charts_num_text,
                    pos++,
                    getString(R.string.artist_title, it.artist, it.name)
                )

                is Album -> getString(
                    R.string.charts_num_text,
                    pos++,
                    getString(R.string.artist_title, it.artist, it.name)
                )

                else -> getString(R.string.charts_num_text, pos++, it.name)
            }
        }

        return list
    }

    private fun shareImageUri(uri: Uri, text: String? = null) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            type = "image/jpeg"
        }
        startActivity(intent)
        resetProgress()
    }

    private fun resetProgress() {
        binding.collageGenerateProgress.hide()
        binding.collageGenerateProgress.isIndeterminate = true
        binding.collageGenerateButton.visibility = View.VISIBLE
    }

    override fun onStart() {
        super.onStart()
        expandIfNeeded()
    }
}