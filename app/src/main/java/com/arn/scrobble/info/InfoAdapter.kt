package com.arn.scrobble.info

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Layout
import android.text.Spanned
import android.text.style.URLSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDeepLinkBuilder
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.result
import com.arn.scrobble.LFMRequester
import com.arn.scrobble.MainActivity
import com.arn.scrobble.MainDialogActivity
import com.arn.scrobble.MainNotifierViewModel
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.copyToClipboard
import com.arn.scrobble.Stuff.toBundle
import com.arn.scrobble.databinding.ListItemInfoBinding
import com.arn.scrobble.ui.UiUtils
import com.arn.scrobble.ui.UiUtils.scheduleTransition
import com.arn.scrobble.ui.UiUtils.toast
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors
import de.umass.lastfm.Album
import de.umass.lastfm.ImageSize
import de.umass.lastfm.MusicEntry
import de.umass.lastfm.Track
import java.text.NumberFormat


class InfoAdapter(
    private val viewModel: InfoVM,
    private val activityViewModel: MainNotifierViewModel,
    private val fragment: InfoFragment,
    private val pkgName: String?,
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
            for (i in 1..8) {
                val chip = Chip(itemView.context)
                chip.id = View.generateViewId()
                chip.setOnClickListener {
                    val args =
                        Bundle().apply { putString(Stuff.ARG_TAG, chip.text.toString()) }
                    fragment.findNavController().navigate(R.id.tagInfoFragment, args)
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

        fun setItemData(
            pair: Map.Entry<String, MusicEntry>,
            activityViewModel: MainNotifierViewModel
        ) {
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
                                LFMRequester(viewModel.viewModelScope).loveOrUnlove(
                                    entry,
                                    entry.isLoved
                                )
                                setLoved(entry)
                            }
                        } else {
                            if (entry.isLoved) {
                                setLoved(entry)
                                binding.infoHeart.alpha = 0.5f
                                binding.infoHeart.visibility = View.VISIBLE
                                binding.infoHeart.setOnClickListener {
                                    itemView.context.toast(
                                        itemView.context.getString(
                                            R.string.user_loved,
                                            activityViewModel.currentUser.name
                                        )
                                    )
                                }
                            }
                        }
                    }

                    if (entry.duration > 0) {
                        binding.infoTrackDuration.isVisible = true
                        binding.infoTrackDuration.text = Stuff.humanReadableDuration(entry.duration)
                    }

                    binding.infoExtraButton.visibility = View.VISIBLE
                    binding.infoExtraButton.text =
                        itemView.context.getString(R.string.analysis) + " • " +
                                itemView.context.getString(R.string.similar)

                    binding.infoExtraButton.setOnClickListener {
                        fragment.findNavController().navigate(R.id.infoExtraFragment, entryBundle)
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

                        binding.infoExtraButton.visibility = View.VISIBLE
                        binding.infoExtraButton.text = itemView.context.resources.getQuantityString(
                            R.plurals.num_tracks,
                            tracks.size,
                            NumberFormat.getInstance().format(tracks.size)
                        ) +
                                if (totalDuration > 0)
                                    " • " + Stuff.humanReadableDuration(totalDuration) + plus
                                else
                                    ""

                        binding.infoExtraButton.setOnClickListener {
                            fragment.toggleAlbumTracks(
                                entry,
                                binding.infoExtraContent,
                                binding.infoExtraButton
                            )
                        }

                    } else
                        binding.infoExtraButton.visibility = View.GONE

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

                    binding.infoExtraButton.visibility = View.VISIBLE
                    binding.infoExtraButton.text = itemView.context.getString(R.string.artist_extra)
                    binding.infoExtraButton.setOnClickListener {
                        fragment.findNavController().navigate(R.id.infoExtraFragment, entryBundle)
                    }
                }
            }
            binding.infoName.text = entry.name

            binding.infoPlay.setOnClickListener {
                Stuff.launchSearchIntent(entry, pkgName)
            }

            entry.url ?: return
            binding.infoUserTags.visibility = View.VISIBLE
            binding.infoUserTags.setOnClickListener {
                fragment.findNavController().navigate(R.id.userTagsFragment, entryBundle)
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

            if (viewModel.infoExtraExpandedMap[key] == true) {
                binding.infoExtraButton.callOnClick()
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

            if (entry.userPlaycount > -1) {
                if (!activityViewModel.userIsSelf) {
                    UiUtils.loadSmallUserPic(
                        binding.infoUserScrobblesLabel.context,
                        activityViewModel.currentUser
                    ) {
                        binding.infoUserScrobblesLabel
                            .setCompoundDrawablesRelativeWithIntrinsicBounds(it, null, null, null)
                        binding.infoUserScrobbles
                            .setCompoundDrawablesRelativeWithIntrinsicBounds(
                                ColorDrawable(Color.TRANSPARENT),
                                null,
                                null,
                                null
                            )
                    }

                    binding.infoUserScrobblesLabel.text =
                        itemView.context.getString(R.string.their_scrobbles)
                }
                binding.infoUserScrobbles.text =
                    NumberFormat.getInstance().format(entry.userPlaycount)
            } else {
                binding.infoUserScrobblesContainer.visibility = View.GONE
            }
            binding.infoListeners.text = NumberFormat.getInstance().format(entry.listeners)
            binding.infoScrobbles.text = NumberFormat.getInstance().format(entry.playcount)

            val secondaryColor =
                MaterialColors.getColor(
                    itemView.context,
                    com.google.android.material.R.attr.colorSecondary,
                    null
                )
            if (entry.userPlaycount > 0) {
                binding.infoUserScrobbles.setTextColor(secondaryColor)
                binding.infoUserScrobblesLabel.setTextColor(secondaryColor)

                binding.infoUserScrobblesContainer.setBackgroundResource(R.drawable.selector_border_gentle)
                binding.infoUserScrobblesContainer.isFocusable = true

                binding.infoUserScrobblesContainer.setOnClickListener {

                    when (entry) {
                        is Track -> {
                            val args = entryBundle.apply {
                                putInt(Stuff.ARG_COUNT, entry.userPlaycount)
                            }

                            if (fragment.requireActivity() is MainDialogActivity) {
                                NavDeepLinkBuilder(itemView.context)
                                    .setComponentName(MainActivity::class.java)
                                    .setGraph(R.navigation.nav_graph)
                                    .setDestination(R.id.trackHistoryFragment)
                                    .setArguments(args)
                                    .createPendingIntent()
                                    .send()
                            } else {
                                fragment.findNavController()
                                    .navigate(R.id.trackHistoryFragment, args)
                            }
                        }

                        else -> {
                            val _username = activityViewModel.currentUser.name
                            val libraryUrl =
                                entry.url.replace("/music/", "/user/$_username/library/music/")
                            Stuff.openInBrowser(libraryUrl)
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
                if (wikiText.isNotBlank()) {
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
                    Stuff.openInBrowser(entry.url)
            }
        }
    }
}
