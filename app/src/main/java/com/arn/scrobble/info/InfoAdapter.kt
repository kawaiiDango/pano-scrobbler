package com.arn.scrobble.info

import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Layout
import android.text.Spanned
import android.text.style.URLSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.doOnNextLayout
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.result
import com.arn.scrobble.*
import com.arn.scrobble.R
import com.arn.scrobble.Stuff.copyToClipboard
import com.arn.scrobble.Stuff.toBundle
import com.arn.scrobble.databinding.ListItemInfoBinding
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.recents.TrackHistoryFragment
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.ui.UiUtils
import com.arn.scrobble.ui.UiUtils.dismissAllDialogFragments
import com.arn.scrobble.ui.UiUtils.dp
import com.arn.scrobble.ui.UiUtils.openInBrowser
import com.arn.scrobble.ui.UiUtils.scheduleTransition
import com.arn.scrobble.ui.UiUtils.toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors
import de.umass.lastfm.*
import java.text.NumberFormat
import kotlin.math.max


class InfoAdapter(
    private val viewModel: InfoVM,
    private val activityViewModel: MainNotifierViewModel,
    private val fragment: BottomSheetDialogFragment,
    private val pkgName: String?
) : RecyclerView.Adapter<InfoAdapter.VHInfo>() {

    init {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHInfo {
        val inflater = LayoutInflater.from(parent.context)
        return VHInfo(ListItemInfoBinding.inflate(inflater, parent, false))
    }

    override fun getItemCount() = viewModel.infoMap.size

    override fun onBindViewHolder(holder: VHInfo, position: Int) {
        holder.setItemData(viewModel.infoMap.entries.toList()[position], activityViewModel)
    }

    inner class VHInfo(private val binding: ListItemInfoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // workaround for library bug where the bg color depends on when the chip was added
            // the resulting bg color was still very bright #262626 vs #1a1a1a
            // that is offset by setting chip.backgroundDrawable!!.alpha lmao
            // edit: this doesn't work anymore in material 3
            for (i in 1..8) {
                val chip = Chip(itemView.context)
                chip.id = View.generateViewId()
//                chip.chipBackgroundColor = null
//                chip.backgroundDrawable!!.alpha = (0.68 * 255).toInt()
                chip.setOnClickListener {
                    val tif = TagInfoFragment()
                    tif.arguments =
                        Bundle().apply { putString(Stuff.ARG_TAG, chip.text.toString()) }
                    tif.show(fragment.parentFragmentManager, null)
                }
                chip.visibility = View.GONE
                binding.infoTags.addView(chip)
            }

            binding.infoName.setOnLongClickListener {
                itemView.context.copyToClipboard(binding.infoName.text.toString())
                true
            }

            binding.infoName.setOnClickListener {
                binding.infoTitleBar.performClick()
            }
        }

        private fun setLoved(track: Track) {
            if (track.isLoved) {
                binding.infoHeart.setIconResource(R.drawable.vd_heart_filled)
                binding.infoHeart.contentDescription = itemView.context.getString(R.string.loved)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    binding.infoHeart.tooltipText = itemView.context.getString(R.string.loved)
                }
            } else {
                binding.infoHeart.setIconResource(R.drawable.vd_heart)
                binding.infoHeart.contentDescription = itemView.context.getString(R.string.unloved)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    binding.infoHeart.tooltipText = itemView.context.getString(R.string.unloved)
                }
            }
        }

        private fun toggleAlbumTracks(album: Album, linearLayout: LinearLayout) {
//            scheduleTransition()

            val viewCount = linearLayout.childCount
            val recyclerView = linearLayout.getChildAt(viewCount - 1) as? RecyclerView
            if (recyclerView == null) {
                val tracks = album.tracks.toList()
                val albumTracksAdapter = AlbumTracksAdapter(tracks).apply {
                    stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
                }
                albumTracksAdapter.itemClickListener = object : ItemClickListener {
                    override fun onItemClick(view: View, position: Int) {
                        val info = InfoFragment()
                        info.arguments = Bundle().apply {
                            putString(NLService.B_ARTIST, album.artist)
                            putString(NLService.B_ALBUM, album.name)
                            putString(NLService.B_TRACK, tracks[position].name)
                        }
                        info.show(fragment.parentFragmentManager, null)
                    }
                }
                val albumTracksRecyclerView = RecyclerView(itemView.context).apply {
                    layoutManager = LinearLayoutManager(itemView.context)
                    adapter = albumTracksAdapter
                    isNestedScrollingEnabled = false
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                }
                linearLayout.addView(albumTracksRecyclerView)
                albumTracksRecyclerView.doOnNextLayout {
                    val parentRecyclerView = (binding.root.parent as RecyclerView)
                    parentRecyclerView.smoothScrollBy(
                        0,
                        max(parentRecyclerView.height - 300.dp, 300.dp)
                    )
                }
                binding.infoExtra.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.vd_arrow_up,
                    0
                )
                viewModel.albumTracksShown = true
            } else {
                linearLayout.removeView(recyclerView)
                binding.infoExtra.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.vd_arrow_right,
                    0
                )
                viewModel.albumTracksShown = false
            }
        }

        fun setItemData(pair: Map.Entry<String, MusicEntry>, activityViewModel: MainNotifierViewModel) {
            val (key, entry) = pair
            val entryBundle = entry.toBundle()
            var imgData: Any? = null

            when (key) {
                NLService.B_TRACK -> {
                    entry as Track
                    binding.infoType.setImageResource(R.drawable.vd_note)
                    binding.infoType.contentDescription = itemView.context.getString(R.string.track)
                    if (entry.url != null) {
                        if (activityViewModel.userIsSelf) {
                            setLoved(entry)
                            binding.infoHeart.visibility = View.VISIBLE
                            binding.infoHeart.setOnClickListener {
                                entry.isLoved = !entry.isLoved
                                LFMRequester(
                                    itemView.context,
                                    viewModel.viewModelScope
                                ).loveOrUnlove(entry, entry.isLoved)
                                setLoved(entry)
                            }
                        } else {
                            if (entry.isLoved) {
                                setLoved(entry)
                                binding.infoHeart.alpha = 0.5f
                                binding.infoHeart.visibility = View.VISIBLE
                                binding.infoHeart.setOnClickListener {
                                    itemView.context.toast(
                                        itemView.context.getString(R.string.user_loved, activityViewModel.peekUser().name)
                                    )
                                }
                            }
                        }
                    }

                    binding.infoExtra.text = itemView.context.getString(R.string.similar)
                    binding.infoExtra.setOnClickListener {
                        InfoExtraFragment()
                            .apply { arguments = entryBundle }
                            .show(fragment.parentFragmentManager, null)
                    }
                }
                NLService.B_ALBUM -> {
                    binding.infoType.setImageResource(R.drawable.vd_album)
                    binding.infoType.contentDescription = itemView.context.getString(R.string.album)

                    val tracks = (entry as Album).tracks?.toList()

                    if (!tracks.isNullOrEmpty()) {
                        var totalDuration = 0
                        var plus = ""
                        tracks.forEach {
                            val duration = it.duration
                            if (duration > 0) {
                                totalDuration += duration
                            } else
                                plus = "+"
                        }

                        binding.infoExtra.visibility = View.VISIBLE
                        binding.infoExtra.text = itemView.context.resources.getQuantityString(
                            R.plurals.num_tracks,
                            tracks.size,
                            NumberFormat.getInstance().format(tracks.size)
                        ) +
                                if (totalDuration > 0)
                                    " • " + Stuff.humanReadableDuration(totalDuration) + plus
                                else
                                    ""

                        binding.infoExtra.setOnClickListener {
                            toggleAlbumTracks(entry, binding.root)
                        }

                        if (viewModel.albumTracksShown) {
                            toggleAlbumTracks(entry, binding.root)
                        }
                    } else
                        binding.infoExtra.visibility = View.GONE

                    imgData = entry.getWebpImageURL(ImageSize.EXTRALARGE)?.ifEmpty { null }

                    binding.infoPic.contentDescription =
                        itemView.context.getString(R.string.album_art)
                    binding.infoPicExpanded.contentDescription =
                        itemView.context.getString(R.string.album_art)

                }
                NLService.B_ARTIST, NLService.B_ALBUM_ARTIST -> {
                    when (key) {
                        NLService.B_ARTIST -> {
                            binding.infoType.setImageResource(R.drawable.vd_mic)
                            binding.infoType.contentDescription =
                                itemView.context.getString(R.string.artist)
                        }
                        NLService.B_ALBUM_ARTIST -> {
                            binding.infoType.setImageResource(R.drawable.vd_album_artist)
                            binding.infoType.contentDescription =
                                itemView.context.getString(R.string.album_artist)
                        }
                    }

                    binding.infoPic.contentDescription =
                        itemView.context.getString(R.string.artist_image)
                    binding.infoPicExpanded.contentDescription =
                        itemView.context.getString(R.string.artist_image)

                    imgData = entry

                    binding.infoExtra.text = itemView.context.getString(R.string.artist_extra)
                    binding.infoExtra.setOnClickListener {
                        InfoExtraFragment()
                            .apply { arguments = entryBundle }
                            .show(fragment.parentFragmentManager, null)
                    }
                }
            }
            binding.infoName.text = entry.name

            binding.infoPlay.setOnClickListener {
                UiUtils.launchSearchIntent(itemView.context, entry, pkgName)
            }

            entry.url ?: return
            binding.infoUserTags.visibility = View.VISIBLE
            binding.infoUserTags.setOnClickListener {
                UserTagsFragment()
                    .apply { arguments = entryBundle }
                    .show(fragment.childFragmentManager, null)
            }

            binding.infoTitleBar.setOnClickListener {
                if (binding.infoPicExpandedFrame.visibility == View.VISIBLE) { // collapse
                    fragment.scheduleTransition()
                    binding.infoPicExpandedFrame.visibility = View.GONE
                    binding.infoPic.visibility = View.VISIBLE
                    viewModel.picExpandedMap[key] = false
                } else if (binding.infoPic.visibility == View.VISIBLE) { // expand
                    fragment.scheduleTransition()
                    binding.infoPicExpandedFrame.visibility = View.VISIBLE
                    binding.infoPic.visibility = View.GONE
                    binding.infoPicExpanded.load(binding.infoPic.result?.request?.data)
                    viewModel.picExpandedMap[key] = true
                }
            }

            if (imgData != null) {
                binding.infoPic.load(imgData) {
                    listener(
                        onSuccess = { _, _ ->
                            if (viewModel.picExpandedMap[key] != true) {
//                                fragment.scheduleTransition()
                                binding.infoPic.visibility = View.VISIBLE
                            } else
                                binding.infoPic.visibility = View.GONE
                        },
                        onError = { _, _ ->
                            binding.infoPic.visibility = View.GONE
                        }
                    )
                }
            } else {
                binding.infoPic.visibility = View.GONE
                binding.infoPicExpanded.visibility = View.GONE
            }

            binding.infoContent.visibility = View.VISIBLE

            if (!activityViewModel.userIsSelf)
                binding.infoUserScrobblesLabel.text =
                    itemView.context.getString(R.string.user_scrobbles, activityViewModel.peekUser().name)
            binding.infoUserScrobbles.text =
                NumberFormat.getInstance().format(entry.userPlaycount)
            binding.infoListeners.text = NumberFormat.getInstance().format(entry.listeners)
            binding.infoScrobbles.text = NumberFormat.getInstance().format(entry.playcount)

            val secondaryColor =
                MaterialColors.getColor(itemView.context, R.attr.colorSecondary, null)
            if (entry.userPlaycount > 0) {
                binding.infoUserScrobbles.setTextColor(secondaryColor)
                binding.infoUserScrobblesLabel.setTextColor(secondaryColor)

                binding.infoUserScrobblesContainer.setBackgroundResource(R.drawable.selector_border_gentle)
                binding.infoUserScrobblesContainer.isFocusable = true

                binding.infoUserScrobblesContainer.setOnClickListener {

                    when (entry) {
                        is Track -> {
                            (fragment.activity as? MainActivity)?.enableGestures()
                            fragment.parentFragmentManager
                                .beginTransaction()
                                .replace(R.id.frame,
                                    TrackHistoryFragment().apply {
                                        arguments = entryBundle.apply {
                                            putInt(Stuff.ARG_COUNT, entry.userPlaycount)
                                        }
                                    }
                                )
                                .addToBackStack(null)
                                .commit()
                            fragment.parentFragmentManager.dismissAllDialogFragments()
                        }
                        is Album,
                        is Artist -> {
                            val _username = activityViewModel.peekUser().name
                            val libraryUrl =
                                entry.url.replace("/music/", "/user/$_username/library/music/")
                            itemView.context.openInBrowser(libraryUrl)
                        }
                    }
                }
            }

            var lastI = 0
            entry.tags.forEachIndexed { i, tag ->
                (binding.infoTags.getChildAt(i) as? Chip?)?.apply {
                    text = tag
                    visibility = View.VISIBLE
                }
                lastI = i
            }

            for (i in (lastI + 1) until binding.infoTags.childCount) {
                val chip = binding.infoTags.getChildAt(i) as Chip
                chip.visibility = View.GONE
            }

            var wikiText = entry.wikiText ?: entry.wikiSummary
            if (!wikiText.isNullOrBlank()) {
                var idx = wikiText.indexOf("<a href=\"http://www.last.fm")
                if (idx == -1)
                    idx = wikiText.indexOf("<a href=\"https://www.last.fm")
                if (idx != -1)
                    wikiText = wikiText.substring(0, idx).trim()
                if (!wikiText.isNullOrBlank()) {
                    wikiText = wikiText.replace("\n", "<br>")
//                        if (entry.wikiLastChanged != null && entry.wikiLastChanged.time != 0L)
//                            wikiText += "<br><br><i>" + itemView.context.getString(R.string.last_updated,
//                                    DateFormat.getLongDateFormat(itemView.context).format(entry.wikiLastChanged)) +
//                                    "</i>"
//                        This is the first published date and not the last updated date
                    binding.infoWikiContainer.visibility = View.VISIBLE
                    binding.infoWiki.text = Html.fromHtml(wikiText)

                    //text gets cut off to the right if justified and has links
                    val urls = (binding.infoWiki.text as? Spanned)?.getSpans(
                        0,
                        binding.infoWiki.text.length,
                        URLSpan::class.java
                    )
                    if (urls.isNullOrEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        binding.infoWiki.justificationMode =
                            Layout.JUSTIFICATION_MODE_INTER_WORD

                    binding.infoWiki.post {
                        if (binding.infoWiki.layout == null)
                            return@post
                        if (binding.infoWiki.lineCount > 2 ||
                            binding.infoWiki.layout.getEllipsisCount(binding.infoWiki.lineCount - 1) > 0
                        ) {
                            val clickListener = { view: View ->

                                if (!(view is TextView && (view.selectionStart != -1 || view.selectionEnd != -1))) {
                                    fragment.scheduleTransition()

                                    if (binding.infoWiki.maxLines == 2) {
                                        binding.infoWiki.maxLines = 1000
                                        binding.infoWikiExpand.rotation = 180f
                                    } else {
                                        binding.infoWiki.maxLines = 2
                                        binding.infoWikiExpand.rotation = 0f
                                    }
                                }
                            }

                            binding.infoWiki.setOnClickListener(clickListener)
                            binding.infoWikiExpand.setOnClickListener(clickListener)
                            binding.infoWikiExpand.visibility = View.VISIBLE
                        }
                    }
                }
            }

            binding.infoLink.visibility = View.VISIBLE
            binding.infoLink.setOnClickListener {
                if (entry.url != null)
                    itemView.context.openInBrowser(entry.url)
            }
        }
    }
}
