package com.arn.scrobble.charts

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.core.content.FileProvider
import androidx.core.view.drawToBitmap
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import coil.imageLoader
import coil.request.ImageRequest
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.MainNotifierViewModel
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.getSingle
import com.arn.scrobble.Stuff.mapConcurrently
import com.arn.scrobble.databinding.DialogCollageGeneratorBinding
import com.arn.scrobble.databinding.FooterCollageBinding
import com.arn.scrobble.databinding.GridItemCollageBinding
import com.arn.scrobble.databinding.LayoutCollageHeaderBinding
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.scrobbleable.Scrobblables
import com.arn.scrobble.ui.UiUtils.expandIfNeeded
import com.arn.scrobble.ui.UiUtils.getSelectedItemPosition
import com.arn.scrobble.ui.UiUtils.getTintedDrawable
import com.arn.scrobble.ui.UiUtils.toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.ChipGroup
import de.umass.lastfm.Album
import de.umass.lastfm.Artist
import de.umass.lastfm.MusicEntry
import de.umass.lastfm.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar


class CollageGeneratorFragment : BottomSheetDialogFragment() {

    private var _binding: DialogCollageGeneratorBinding? = null
    private val binding get() = _binding!!
    private val activityViewModel by activityViewModels<MainNotifierViewModel>()
    private val timePeriod get() = requireArguments().getSingle<TimePeriod>()!!
    private val prefs by lazy { MainPrefs(requireContext()) }
    private lateinit var saveBySafRequest: ActivityResultLauncher<Intent>
    private val minSize = 3
    private val maxSize = 6
    private var lastUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        saveBySafRequest =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                    requireContext().contentResolver.openOutputStream(result.data!!.data!!)
                        ?.use { ostream ->
                            requireContext().contentResolver.openInputStream(lastUri!!)?.use { istream ->
                                istream.copyTo(ostream)
                            }
                        }
                }
            }

    }

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
        val sizes = (minSize..maxSize).map { "$it" }.toTypedArray()
        val types = arrayOf(
            getString(R.string.edit_all),
            getString(R.string.top_artists),
            getString(R.string.top_albums),
            getString(R.string.top_tracks),
        )

        binding.collageTypeText.setSimpleItems(types)
        binding.collageTypeText.setText(types[requireArguments().getInt(Stuff.ARG_TYPE)], false)

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

        binding.collageShareButton.setOnClickListener {
            onCreateCollageButtonClick(false)
        }

        binding.collageSaveButton.setOnClickListener {
            onCreateCollageButtonClick(true)
        }
    }

    private fun onCreateCollageButtonClick(save: Boolean) {

        binding.collageGenerateProgress.show()
        binding.collageGenerateButtons.visibility = View.INVISIBLE

        prefs.collageSkipMissing = binding.collageSkipMissingImages.isChecked
        prefs.collageCaptions = binding.collageIncludeCaptions.isChecked
        prefs.collageUsername = binding.collageIncludeUsername.isChecked
        prefs.collageText = binding.collageIncludeText.isChecked
        prefs.collageSize = binding.collageSizeText.getSelectedItemPosition() + minSize

        val collageType = binding.collageTypeText.getSelectedItemPosition()
        val cal = Calendar.getInstance()

        viewLifecycleOwner.lifecycleScope.launch {

            val results = if (collageType == Stuff.TYPE_ALL) { // digest
                listOf(
                    Stuff.TYPE_ARTISTS,
                    Stuff.TYPE_ALBUMS,
                    Stuff.TYPE_TRACKS
                ).mapConcurrently(3) {
                    kotlin.runCatching { fetchCharts(it, timePeriod) }
                }
            } else {
                val result = kotlin.runCatching { fetchCharts(collageType, timePeriod) }
                listOf(result)
            }

            if (results.all { it.isSuccess && it.getOrNull()?.pageResults != null }) {
                val (uri, text) = createCollage(
                    collageType,
                    results.map { it.getOrNull()!!.pageResults.toList() })

                resetProgress()

                if (save) {
                    val mimeType = "image/jpeg"
                    val fileName =
                        "collage_" + cal[Calendar.YEAR] + "_" + cal[Calendar.MONTH] + "_" +
                                cal[Calendar.DATE] + "_" + cal[Calendar.HOUR_OF_DAY] + "_" +
                                cal[Calendar.MINUTE] + "_" + cal[Calendar.SECOND] + ".jpg"

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        Stuff.savePictureQ(uri, fileName, mimeType)

                        binding.collageSaveButton.setText(R.string.saved_to_gallery)
                        binding.collageSaveButton.isEnabled = false
                        delay(2500)
                        binding.collageSaveButton.setText(R.string.save)
                        binding.collageSaveButton.isEnabled = true
                    } else {
                        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = mimeType
                            putExtra(Intent.EXTRA_TITLE, fileName)
                        }
                        saveBySafRequest.launch(intent)
                    }
                } else
                    shareImageUri(uri, text)
            } else {
                requireContext().toast(R.string.network_error)
            }
        }
    }

    private suspend fun fetchCharts(type: Int, timePeriod: TimePeriod) =
        withContext(Dispatchers.IO) {
            Scrobblables.current!!.getCharts(
                type,
                timePeriod,
                1,
                activityViewModel.currentUser.name
            )
        }

    private suspend fun createCollage(
        type: Int,
        musicEntriesAll: List<List<MusicEntry>>
    ): Pair<Uri, String?> {
        val collageSize = prefs.collageSize
        val textScaler = collageSize / 5f
        val isDigest = type == Stuff.TYPE_ALL

        val collageRoot = LinearLayout(context, null, com.google.android.material.R.style.Theme_Material3_Dark).apply {
            orientation = LinearLayout.VERTICAL
        }

        fun createAddImagesContainer() =
            ChipGroup(context).apply {
                collageRoot.addView(this)
                updateLayoutParams<LinearLayout.LayoutParams> {
                    width = LinearLayout.LayoutParams.MATCH_PARENT
                    height = LinearLayout.LayoutParams.WRAP_CONTENT
                }
                setChipSpacing(0)
            }

        fun createAddHeader(headerTextRes: Int, headerImageRes: Int) =
            LayoutCollageHeaderBinding.inflate(layoutInflater, collageRoot, true).apply {
                collageHeaderIcon.setImageResource(headerImageRes)
                collageHeaderText.setText(headerTextRes)
                collageHeaderText.setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    collageHeaderText.textSize * textScaler
                )

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

                val headerTextRes: Int
                val headerIconRes: Int

                when (musicEntries.first()) {
                    is Artist -> {
                        headerTextRes = R.string.top_artists
                        headerIconRes = R.drawable.vd_mic
                    }

                    is Album -> {
                        headerTextRes = R.string.top_albums
                        headerIconRes = R.drawable.vd_album
                    }

                    is Track -> {
                        headerTextRes = R.string.top_tracks
                        headerIconRes = R.drawable.vd_note
                    }

                    else -> {
                        throw IllegalArgumentException("Unknown type: $type")
                    }
                }

                createAddHeader(headerTextRes, headerIconRes)

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

        createAddFooter(collageRoot, type, textScaler)
        val (bitmap, uri) = writeCollageAndGetBitmap(collageRoot)
        lastUri = uri
        val aspectRatio = bitmap.width.toFloat() / bitmap.height
        binding.collagePreview.updateLayoutParams {
            height = (width / aspectRatio).toInt()
        }
        binding.collagePreview.setImageBitmap(bitmap)

        return uri to text
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

        return shareTextList.joinToString("\n\n")
    }

    private suspend fun writeCollageAndGetBitmap(collageRoot: LinearLayout): Pair<Bitmap, Uri> {
        val imageDimensionPx = 300
        val totalWidth = imageDimensionPx * prefs.collageSize
        var bitmap: Bitmap

        val collageFile = File(requireContext().filesDir, "collage.jpg")
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
                bitmap = collageRoot.drawToBitmap()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
            }
        }

        return bitmap to
                FileProvider.getUriForFile(requireContext(), "com.arn.scrobble.fileprovider", collageFile)
    }

    private fun createAddFooter(collageRoot: LinearLayout, type: Int, textScaler: Float) {
        val collageFooter = FooterCollageBinding.inflate(layoutInflater, collageRoot, true)

        if (prefs.collageUsername) {
            collageFooter.collageFooterUsername.visibility = View.VISIBLE
            collageFooter.collageUsernameImage.visibility = View.VISIBLE
            collageFooter.collageFooterUsername.text = activityViewModel.currentUser.name
        } else {
            collageFooter.collageFooterUsername.visibility = View.GONE
            collageFooter.collageUsernameImage.visibility = View.GONE
        }

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
            val request = ImageRequest.Builder(requireContext()).apply {
                data(entry)
                allowHardware(false)
                crossfade(false)
                if (prefs.collageSkipMissing)
                    listener(
                        onError = { _, _ -> error = true },
                    )
                else
                    error(
                        requireContext().getTintedDrawable(
                            R.drawable.vd_wave_simple_filled, entry.name.hashCode()
                        )
                    )
                placeholder(null)
            }.build()

            val artworkDrawable = withContext(Dispatchers.IO) {
                requireContext().imageLoader.execute(request).drawable
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
                        gridItemBinding.collageCaption.text = " " + entry.name
                        gridItemBinding.collageSubcaption.visibility = View.GONE
                    }

                    is Album -> {
                        gridItemBinding.collageCaption.text = " " + entry.name
                        gridItemBinding.collageSubcaption.text = " " + entry.artist
                    }

                    is Track -> {
                        gridItemBinding.collageCaption.text = " " + entry.name
                        gridItemBinding.collageSubcaption.text = " " + entry.artist
                    }
                }
                gridItemBinding.collagePlayCount.text = resources.getQuantityString(
                    R.plurals.num_scrobbles_noti, entry.playcount, " " + entry.playcount
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
    }

    private fun resetProgress() {
        binding.collageGenerateProgress.hide()
        binding.collageGenerateProgress.isIndeterminate = true
        binding.collageGenerateButtons.visibility = View.VISIBLE
    }

    override fun onStart() {
        super.onStart()
        expandIfNeeded()
    }
}