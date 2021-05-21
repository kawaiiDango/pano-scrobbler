package com.arn.scrobble.info

import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Layout
import android.text.Spanned
import android.text.style.URLSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.*
import com.arn.scrobble.databinding.ListItemInfoBinding
import com.arn.scrobble.ui.ItemClickListener
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.umass.lastfm.Album
import de.umass.lastfm.MusicEntry
import de.umass.lastfm.Track
import java.text.NumberFormat


class InfoAdapter(private val viewModel: InfoVM, private val fragment: BottomSheetDialogFragment, private val username: String?) : RecyclerView.Adapter<InfoAdapter.VHInfo>() {

    init {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHInfo{
        val inflater = LayoutInflater.from(parent.context)
        return VHInfo(ListItemInfoBinding.inflate(inflater, parent, false))
    }

    override fun getItemCount() = viewModel.info.size

    override fun onBindViewHolder(holder: VHInfo, position: Int) {
        holder.setItemData(viewModel.info[position], username)
    }

    inner class VHInfo(private val binding: ListItemInfoBinding): RecyclerView.ViewHolder(binding.root){

        init {
            // workaround for library bug where the bg color depends on when the chip was added
            // the resulting bg color was still very bright #262626 vs #1a1a1a
            // that is offset by setting chip.backgroundDrawable!!.alpha lmao
            for (i in 1..8){
                val chip = Chip(itemView.context)
                chip.id = View.generateViewId()
                chip.chipBackgroundColor = null
                chip.backgroundDrawable!!.alpha = (0.68 * 255).toInt()
                chip.setOnClickListener {
                    val tif = TagInfoFragment()
                    tif.arguments = Bundle().apply { putString(Stuff.ARG_TAG, chip.text.toString()) }
                    tif.show(fragment.parentFragmentManager, null)
                }
                chip.visibility = View.GONE
                binding.infoTags.addView(chip)
            }
        }

        private fun setLoved(track: Track) {
            if (track.isLoved) {
                binding.infoHeart.setImageResource(R.drawable.vd_heart_filled)
                binding.infoHeart.contentDescription = itemView.context.getString(R.string.loved)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    binding.infoHeart.tooltipText = itemView.context.getString(R.string.loved)
                }
            } else {
                binding.infoHeart.setImageResource(R.drawable.vd_heart)
                binding.infoHeart.contentDescription = itemView.context.getString(R.string.unloved)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    binding.infoHeart.tooltipText = itemView.context.getString(R.string.unloved)
                }
            }
        }

        fun setItemData(pair: Pair<String, MusicEntry?>, username: String?) {
            val key = pair.first
            val entry = pair.second
            val b = Bundle()

            when (key) {
                NLService.B_TITLE -> {
                    entry as Track
                    binding.infoPlay.visibility = View.VISIBLE
                    binding.infoPlay.setOnClickListener {
                        Stuff.launchSearchIntent(entry.artist, entry.name, itemView.context)
                    }
                    binding.infoType.setImageResource(R.drawable.vd_note)
                    binding.infoType.contentDescription = itemView.context.getString(R.string.track)
                    if (entry.url != null) {
                        if (username == null) {
                            setLoved(entry)
                            binding.infoHeart.visibility = View.VISIBLE
                            binding.infoHeart.setOnClickListener {
                                entry.isLoved = !entry.isLoved
                                LFMRequester(itemView.context).loveOrUnlove(entry.isLoved, entry.artist, entry.name)
                                        .asSerialAsyncTask()
                                setLoved(entry)
                            }
                        } else {
                            if (entry.isLoved) {
                                setLoved(entry)
                                binding.infoHeart.alpha = 0.5f
                                binding.infoHeart.visibility = View.VISIBLE
                                binding.infoHeart.setOnClickListener {
                                    Stuff.toast(itemView.context, itemView.context.getString(R.string.user_loved, username))
                                }
                            }
                        }
                    }

                    b.putString(NLService.B_ARTIST, entry.artist)
                    b.putString(NLService.B_TITLE, entry.name)

                    binding.infoExtra.text = itemView.context.getString(R.string.similar)
                    binding.infoExtra.setOnClickListener {
                        InfoExtraFragment()
                                .apply { arguments = b }
                                .show(fragment.parentFragmentManager, null)
                    }
                }
                NLService.B_ALBUM -> {
                    binding.infoType.setImageResource(R.drawable.vd_album)
                    binding.infoType.contentDescription = itemView.context.getString(R.string.album)

                    val tracks = (entry as Album).tracks?.toList()

                    b.putString(NLService.B_ARTIST, entry.artist)
                    b.putString(NLService.B_ALBUM, entry.name)

                    if (!tracks.isNullOrEmpty()) {
                        var totalDuration = 0
                        var plus = ""
                        tracks.forEachIndexed { i, track ->
                            val duration = track.duration
                            if (duration > 0) {
                                totalDuration += duration
                            } else
                                plus = "+"
                        }

                        binding.infoExtra.visibility = View.VISIBLE
                        binding.infoExtra.text = itemView.context.resources.getQuantityString(R.plurals.num_songs, tracks.size, tracks.size) +
                                if (totalDuration > 0)
                                    " • " + Stuff.humanReadableDuration(totalDuration) + plus
                                else
                                    ""

                        binding.infoExtra.setOnClickListener {
                            val rv = LayoutInflater.from(itemView.context).inflate(R.layout.content_simple_list, null) as RecyclerView
                            rv.layoutManager = LinearLayoutManager(itemView.context)
                            val dialog = MaterialAlertDialogBuilder(itemView.context)
                                    .setTitle(Stuff.getColoredTitle(itemView.context, entry.name))
                                    .setIcon(R.drawable.vd_album)
                                    .setView(rv)
                                    .setNegativeButton(android.R.string.cancel, null)
                                    .create()

                            val adapter = AlbumTracksAdapter(tracks)
                            adapter.itemClickListener = object : ItemClickListener {
                                override fun onItemClick(view: View, position: Int) {
                                    if (position > -1) {
                                        dialog.dismiss()
                                        fragment.dismiss()
                                        val info = InfoFragment()
                                        val thisBundle = b.clone() as Bundle
                                        thisBundle.putString(NLService.B_TITLE, tracks[position].name)
                                        thisBundle.putString(Stuff.ARG_USERNAME, username)
                                        info.arguments = thisBundle
                                        info.show(fragment.parentFragmentManager, null)
                                    }
                                }
                            }
                            rv.adapter = adapter

                            val window = dialog.window
                            val wlp = window!!.attributes
                            wlp.gravity = Gravity.BOTTOM
                            window.attributes = wlp
                            dialog.show()
                        }
                    } else
                        binding.infoExtra.visibility = View.GONE

                    binding.infoUserTags.setOnClickListener {
                        UserTagsFragment()
                                .apply { arguments = b }
                                .show(fragment.childFragmentManager, null)
                    }
                }
                NLService.B_ARTIST -> {
                    binding.infoType.setImageResource(R.drawable.vd_mic)
                    binding.infoType.contentDescription = itemView.context.getString(R.string.artist)

                    b.putString(NLService.B_ARTIST, entry!!.name)

                    binding.infoExtra.text = itemView.context.getString(R.string.artist_extra)
                    binding.infoExtra.setOnClickListener {
                        InfoExtraFragment()
                                .apply { arguments = b }
                                .show(fragment.parentFragmentManager, null)
                    }
                }
                NLService.B_ALBUM_ARTIST -> {
                    binding.infoType.setImageResource(R.drawable.vd_album_artist)
                    binding.infoType.contentDescription = itemView.context.getString(R.string.album_artist)

                    b.putString(NLService.B_ARTIST, entry!!.name)

                    binding.infoExtra.visibility = View.VISIBLE
                    binding.infoExtra.text = itemView.context.getString(R.string.artist_extra)
                    binding.infoExtra.setOnClickListener {
                        InfoExtraFragment()
                                .apply { arguments = b }
                                .show(fragment.parentFragmentManager, null)
                    }
                }
            }
            binding.infoName.text = entry?.name
            
            if (entry?.url == null && (key in viewModel.loadedTypes || !Main.isOnline)) {
                binding.infoProgress.visibility = View.GONE
                return
            }
            if (entry?.url != null) {
                binding.infoProgress.visibility = View.GONE

                binding.infoUserTags.visibility = View.VISIBLE
                binding.infoUserTags.setOnClickListener {
                    UserTagsFragment()
                            .apply { arguments = b }
                            .show(fragment.childFragmentManager, null)
                }

                binding.infoContent.visibility = View.VISIBLE

                if (username != null)
                    binding.infoUserScrobblesLabel.text = itemView.context.getString(R.string.user_scrobbles, username)
                binding.infoUserScrobbles.text = NumberFormat.getInstance().format(entry.userPlaycount)
                binding.infoListeners.text = NumberFormat.getInstance().format(entry.listeners)
                binding.infoScrobbles.text = NumberFormat.getInstance().format(entry.playcount)

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
                        val urls = (binding.infoWiki.text as? Spanned)?.getSpans(0, binding.infoWiki.text.length, URLSpan::class.java)
                        if (urls.isNullOrEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            binding.infoWiki.justificationMode = Layout.JUSTIFICATION_MODE_INTER_WORD

                        binding.infoWiki.post{
                            if (binding.infoWiki.layout == null)
                                return@post
                            if (binding.infoWiki.lineCount > 2 ||
                                    binding.infoWiki.layout.getEllipsisCount(binding.infoWiki.lineCount - 1) > 0) {
                                val clickListener = { view: View ->
                                    if (!(view is TextView && (view.selectionStart != -1 || view.selectionEnd != -1))) {
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
                        Stuff.openInBrowser(entry.url, itemView.context)
                }
            } else
                binding.infoProgress.show()
        }
    }
}
