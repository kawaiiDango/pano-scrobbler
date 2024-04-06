package com.arn.scrobble.info

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Layout
import android.text.Spanned
import android.text.style.URLSpan
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.doOnNextLayout
import androidx.core.view.get
import androidx.core.view.isEmpty
import androidx.core.view.isVisible
import androidx.navigation.NavDeepLinkBuilder
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.databinding.ContentAlbumTracksBinding
import com.arn.scrobble.databinding.ListItemInfoBinding
import com.arn.scrobble.main.MainActivity
import com.arn.scrobble.main.MainDialogActivity
import com.arn.scrobble.main.MainNotifierViewModel
import com.arn.scrobble.ui.GenericDiffCallback
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.ui.MusicEntryImageReq
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.copyToClipboard
import com.arn.scrobble.utils.Stuff.format
import com.arn.scrobble.utils.Stuff.putData
import com.arn.scrobble.utils.UiUtils
import com.arn.scrobble.utils.UiUtils.dp
import com.arn.scrobble.utils.UiUtils.toast
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors
import kotlin.math.max


class InfoAdapter(
    private val viewModel: InfoVM,
    private val activityViewModel: MainNotifierViewModel,
    private val fragment: InfoFragment,
    private val pkgName: String?,
) : ListAdapter<InfoVM.InfoHolder, InfoAdapter.VHInfo>(
    GenericDiffCallback { o, n -> o.type == n.type }
) {

    init {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHInfo {
        val inflater = LayoutInflater.from(parent.context)
        return VHInfo(ListItemInfoBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: VHInfo, position: Int) {
        holder.setItemData(getItem(position), activityViewModel)
    }

    inner class VHInfo(private val binding: ListItemInfoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var _info: InfoVM.InfoHolder? = null
        private val info get() = _info!!

        init {
            for (i in 1..8) {
                val chip = Chip(itemView.context)
                chip.id = View.generateViewId()
                chip.visibility = View.GONE
                binding.infoTags.addView(chip)
            }

            if (!Stuff.isTv)
                binding.infoTitleBar.setOnLongClickListener {
                    itemView.context.copyToClipboard(binding.infoName.text.toString())
                    true
                }

            binding.infoPlay.setOnClickListener {
                Stuff.launchSearchIntent(info.entry, pkgName)
            }

            binding.infoUserTags.setOnClickListener {
                fragment.findNavController()
                    .navigate(R.id.userTagsFragment, Bundle().putData(info.entry))
            }

            binding.infoExtraButton.setOnClickListener {

                when (info.entry) {
                    is Track, is Artist ->
                        fragment.findNavController()
                            .navigate(R.id.infoExtraFragment, Bundle().putData(info.entry))

                    is Album ->
                        viewModel.updateInfo(
                            info.copy(
                                trackListExpanded = !info.trackListExpanded
                            )
                        )
                }
            }

            binding.infoUserScrobblesContainer.setOnClickListener {
                if (info.entry.userplaycount == null || info.entry.userplaycount!! == 0)
                    return@setOnClickListener

                when (info.entry) {
                    is Track -> {
                        val entryBundle = Bundle().putData(info.entry)

                        if (fragment.requireActivity() is MainDialogActivity) {
                            NavDeepLinkBuilder(itemView.context)
                                .setComponentName(MainActivity::class.java)
                                .setGraph(R.navigation.nav_graph)
                                .setDestination(R.id.trackHistoryFragment)
                                .setArguments(entryBundle)
                                .createPendingIntent()
                                .send()
                        } else {
                            fragment.findNavController()
                                .navigate(R.id.trackHistoryFragment, entryBundle)
                        }
                    }

                    else -> {
                        val _username = activityViewModel.currentUser.name
                        info.entry.url
                            ?.replace("/music/", "/user/$_username/library/music/")
                            ?.let {
                                Stuff.openInBrowser(it)
                            }
                    }
                }
            }

            binding.infoLink.setOnClickListener {
                info.entry.url?.let {
                    Stuff.openInBrowser(it)
                }
            }

            if (Stuff.isTv)
                binding.infoLink.isVisible = false

            val wikiClickListener = { view: View ->
                viewModel.updateInfo(
                    info.copy(
                        wikiExpanded = !info.wikiExpanded
                    )
                )
            }

            if (!Stuff.isTv) {
                binding.infoWiki.autoLinkMask = Linkify.WEB_URLS
            }

            binding.infoWiki.setOnClickListener(wikiClickListener)
            binding.infoWikiExpand.setOnClickListener(wikiClickListener)

            binding.infoAddPhoto.setOnClickListener {
                val originalEntry = when (info.entry) {
                    is Artist -> viewModel.originalEntriesMap[NLService.B_ARTIST]!!
                    is Album -> viewModel.originalEntriesMap[NLService.B_ALBUM]!!
                    else -> return@setOnClickListener
                }

                val args = Bundle().putData(info.entry)
                    .putData(originalEntry, Stuff.ARG_ORIGINAL)

                if (fragment.requireActivity() is MainDialogActivity) {
                    NavDeepLinkBuilder(itemView.context)
                        .setComponentName(MainActivity::class.java)
                        .setGraph(R.navigation.nav_graph)
                        .setDestination(R.id.imageSearchFragment)
                        .setArguments(args)
                        .createPendingIntent()
                        .send()
                } else {
                    fragment.findNavController()
                        .navigate(R.id.imageSearchFragment, args)
                }
            }
        }

        private fun setLoved(track: Track) {
            if (track.userloved == true) {
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

        private fun toggleAlbumTracks(
            album: Album,
            expanded: Boolean,
            container: FrameLayout,
            button: TextView
        ) {
            val tracks = album.tracks?.track?.toList() ?: return
            val wasPopulated: Boolean
            val localBinding: ContentAlbumTracksBinding

            if (container.isEmpty()) {
                localBinding = ContentAlbumTracksBinding.inflate(
                    LayoutInflater.from(binding.root.context),
                    container,
                    false
                )
                container.addView(localBinding.root)
                wasPopulated = false
            } else {
                localBinding = ContentAlbumTracksBinding.bind(container[0])
                wasPopulated = true
            }
            if (!wasPopulated) {
                val albumTracksAdapter = AlbumTracksAdapter(tracks)

                albumTracksAdapter.itemClickListener = object : ItemClickListener<Track> {
                    override fun onItemClick(view: View, position: Int, item: Track) {
                        fragment.findNavController().navigate(
                            R.id.infoFragment,
                            Bundle().putData(item)
                        )
                    }
                }

                localBinding.tracksList.layoutManager =
                    LinearLayoutManager(localBinding.root.context)
                localBinding.tracksList.adapter = albumTracksAdapter
            }


            if (!expanded) {
                button.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.vd_arrow_down,
                    0
                )
                val scrollFn = {
                    val parentRv = (itemView.parent as RecyclerView)
                    parentRv.smoothScrollBy(
                        0,
                        max(parentRv.height - 300.dp, 300.dp)
                    )
                }

                container.doOnNextLayout { scrollFn() }
            } else {
                button.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.vd_arrow_up,
                    0
                )
            }
            container.isVisible = expanded
        }

        private fun toggleWiki() {
            val info = _info ?: return

            if (binding.infoWiki.layout == null)
                return
            if (binding.infoWiki.lineCount > 2 ||
                binding.infoWiki.layout.getEllipsisCount(binding.infoWiki.lineCount - 1) > 0
            ) {
                binding.infoWikiExpand.visibility = View.VISIBLE

                if (info.wikiExpanded) {
                    binding.infoWiki.maxLines = 1000
                    binding.infoWikiExpand.rotation = 180f
                } else {
                    binding.infoWiki.maxLines = 2
                    binding.infoWikiExpand.rotation = 0f
                }
            }
        }

        fun setItemData(
            info: InfoVM.InfoHolder,
            activityViewModel: MainNotifierViewModel
        ) {
            val prevInfo = this._info
            this._info = info
            val entry = info.entry
            val imgData = if (entry is Album || entry is Artist)
                MusicEntryImageReq(entry, fetchAlbumInfoIfMissing = true)
            else
                null

            if (prevInfo?.entry != info.entry) {
                when (info.type) {
                    NLService.B_TRACK -> {
                        entry as Track
                        binding.infoType.setImageResource(R.drawable.vd_note)
                        binding.infoType.contentDescription =
                            itemView.context.getString(R.string.track)
                        setLoved(entry)

                        if (activityViewModel.currentUser.isSelf) {
                            binding.infoHeart.setOnClickListener {
                                val newLoved = !(entry.userloved ?: false)
                                viewModel.updateInfo(
                                    info.copy(
                                        entry = entry.copy(userloved = newLoved)
                                    )
                                )
                            }
                        } else {
                            if (entry.userloved == true) {
                                binding.infoHeart.alpha = 0.5f
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

                        if (entry.duration != null && entry.duration > 0) {
                            binding.infoTrackDuration.isVisible = true
                            binding.infoTrackDuration.text =
                                Stuff.humanReadableDuration(entry.duration)
                        }

                        binding.infoExtraButton.visibility = View.VISIBLE
                        binding.infoExtraButton.text =
                            itemView.context.getString(R.string.analysis) + " • " +
                                    itemView.context.getString(R.string.similar)
                    }

                    NLService.B_ALBUM -> {
                        binding.infoType.setImageResource(R.drawable.vd_album)
                        binding.infoType.contentDescription =
                            itemView.context.getString(R.string.album)

                        val tracks = (entry as Album).tracks?.track?.toList()

                        if (!tracks.isNullOrEmpty()) {
                            var totalDuration = 0L
                            var plus = ""
                            tracks.forEach {
                                val duration = it.duration
                                if (duration != null) {
                                    totalDuration += duration
                                } else
                                    plus = "+"
                            }

                            binding.infoExtraButton.visibility = View.VISIBLE
                            binding.infoExtraButton.text =
                                itemView.context.resources.getQuantityString(
                                    R.plurals.num_tracks,
                                    tracks.size,
                                    tracks.size.format()
                                ) +
                                        if (totalDuration > 0)
                                            " • " + Stuff.humanReadableDuration(totalDuration) + plus
                                        else
                                            ""

                        } else
                            binding.infoExtraButton.visibility = View.GONE

                        binding.infoPic.contentDescription =
                            itemView.context.getString(R.string.album_art)
                        binding.infoPicExpanded.contentDescription =
                            itemView.context.getString(R.string.album_art)

                    }

                    NLService.B_ARTIST, NLService.B_ALBUM_ARTIST -> {
                        when (info.type) {
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

                        binding.infoExtraButton.visibility = View.VISIBLE
                        binding.infoExtraButton.text =
                            itemView.context.getString(R.string.artist_extra)
                    }
                }
                binding.infoName.text = entry.name
            }

            binding.infoHeart.isVisible = entry is Track
            binding.infoAddPhoto.isVisible =
                (entry is Artist || BuildConfig.DEBUG && entry is Album)

            if (imgData != null) {
                binding.infoPicExpandedFrame.isVisible = info.headerExpanded
                binding.infoPic.isVisible = !info.headerExpanded && info.hasImage

                binding.infoPic.load(imgData) {
                    listener(
                        onSuccess = { _, _ ->
                            binding.infoPic.isVisible = !info.headerExpanded
                            viewModel.updateInfo(
                                info.apply { hasImage = true }
                            )

                            binding.infoTitleBar.isClickable = true
                            binding.infoTitleBar.isFocusable = true
                            binding.infoTitleBar.setOnClickListener {
                                viewModel.updateInfo(info.copy(headerExpanded = !info.headerExpanded))
                            }
                        },
                        onError = { _, _ ->
                            binding.infoPic.isVisible = false

                            binding.infoTitleBar.setOnClickListener(null)
                            binding.infoTitleBar.isClickable = false
                            binding.infoTitleBar.isFocusable = false
                        }
                    )
                }
            } else {
                binding.infoPic.isVisible = false
                binding.infoPicExpandedFrame.isVisible = false

                binding.infoTitleBar.setOnClickListener(null)
                binding.infoTitleBar.isClickable = false
                binding.infoTitleBar.isFocusable = false
            }

            if (info.headerExpanded) {
                binding.infoPicExpanded.load(imgData)
            }

            if (entry.userplaycount != null) {
                if (!activityViewModel.currentUser.isSelf) {
                    UiUtils.loadSmallUserPic(
                        binding.infoUserScrobblesLabel.context,
                        activityViewModel.currentUser,
                        activityViewModel.drawerData.value,
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
                binding.infoUserScrobbles.text = entry.userplaycount?.format()
            }

            binding.infoUserScrobblesContainer.isVisible = entry.userplaycount != null
            binding.infoListenersContainer.isVisible = entry.listeners != null
            binding.infoScrobblesContainer.isVisible = entry.playcount != null

            binding.infoListeners.text = entry.listeners?.format()
            binding.infoScrobbles.text = entry.playcount?.format()

            val secondaryColor =
                MaterialColors.getColor(
                    itemView.context,
                    com.google.android.material.R.attr.colorSecondary,
                    null
                )
            if (entry.userplaycount != null && entry.userplaycount!! > 0) {
                binding.infoUserScrobbles.setTextColor(secondaryColor)
                binding.infoUserScrobblesLabel.setTextColor(secondaryColor)

                binding.infoUserScrobblesContainer.setBackgroundResource(R.drawable.selector_border_gentle)
                binding.infoUserScrobblesContainer.isClickable = true
                binding.infoUserScrobblesContainer.isFocusable = true
            } else {
                binding.infoUserScrobblesContainer.isClickable = false
                binding.infoUserScrobblesContainer.isFocusable = false
            }

            var lastI = 0
            entry.tags?.tag?.forEachIndexed { i, tag ->
                (binding.infoTags.getChildAt(i) as? Chip?)?.apply {
                    text = tag.name
                    visibility = View.VISIBLE

                    setOnClickListener {
                        val args = Bundle().putData(tag)
                        fragment.findNavController().navigate(R.id.tagInfoFragment, args)
                    }
                }
                lastI = i
            }

            for (i in (lastI + 1) until binding.infoTags.childCount) {
                val chip = binding.infoTags.getChildAt(i) as Chip
                chip.visibility = View.GONE
            }

            var wikiText = entry.wiki?.content
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
                        toggleWiki()
                    }
                }
            }

            if (entry is Album && info.trackListExpanded != prevInfo?.trackListExpanded)
                toggleAlbumTracks(
                    entry,
                    info.trackListExpanded,
                    binding.infoExtraContent,
                    binding.infoExtraButton
                )
        }
    }
}
