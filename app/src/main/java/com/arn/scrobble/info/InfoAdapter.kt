package com.arn.scrobble.info

import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.*
import com.arn.scrobble.ui.ListViewItemHighlightTvHack
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import de.umass.lastfm.Album
import de.umass.lastfm.MusicEntry
import de.umass.lastfm.Track
import kotlinx.android.synthetic.main.list_item_info.view.*
import java.text.NumberFormat


class InfoAdapter(private val viewModel: InfoVM, private val fragment: BottomSheetDialogFragment, private val username: String?) : RecyclerView.Adapter<InfoAdapter.VHInfo>() {

    init {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHInfo{
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.list_item_info, parent, false)
        return VHInfo(view)
    }

    override fun getItemCount() = viewModel.info.size

    override fun onBindViewHolder(holder: VHInfo, position: Int) {
        holder.setItemData(viewModel.info[position], username)
    }

    inner class VHInfo(view: View): RecyclerView.ViewHolder(view){
        init {
            setIsRecyclable(false)
        }

        private fun setLoved(track: Track) {
            if (track.isLoved) {
                itemView.info_heart.setImageResource(R.drawable.vd_heart_filled)
                itemView.info_heart.contentDescription = itemView.context.getString(R.string.loved)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    itemView.info_heart.tooltipText = itemView.context.getString(R.string.loved)
                }
            } else {
                itemView.info_heart.setImageResource(R.drawable.vd_heart)
                itemView.info_heart.contentDescription = itemView.context.getString(R.string.unloved)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    itemView.info_heart.tooltipText = itemView.context.getString(R.string.unloved)
                }
            }
        }

        fun setItemData(pair: Pair<String, MusicEntry?>, username: String?) {
            val key = pair.first
            val entry = pair.second
            when (key) {
                NLService.B_TITLE -> {
                    entry as Track
                    itemView.info_play.visibility = View.VISIBLE
                    itemView.info_play.setOnClickListener {
                        Stuff.launchSearchIntent(entry.artist, entry.name, itemView.context)
                    }
                    itemView.info_type.setImageResource(R.drawable.vd_note)
                    itemView.info_type.contentDescription = itemView.context.getString(R.string.track)
                    if (entry.url != null) {
                        if (username == null) {
                            setLoved(entry)
                            itemView.info_heart.visibility = View.VISIBLE
                            itemView.info_heart.setOnClickListener {
                                entry.isLoved = !entry.isLoved
                                LFMRequester(itemView.context).loveOrUnlove(entry.isLoved, entry.artist, entry.name)
                                        .asSerialAsyncTask()
                                setLoved(entry)
                            }
                        } else {
                            if (entry.isLoved) {
                                setLoved(entry)
                                itemView.info_heart.alpha = 0.5f
                                itemView.info_heart.visibility = View.VISIBLE
                                itemView.info_heart.setOnClickListener {
                                    Stuff.toast(itemView.context, itemView.context.getString(R.string.user_loved, username))
                                }
                            }
                        }
                    }
                }
                NLService.B_ALBUM -> {
                    itemView.info_type.setImageResource(R.drawable.vd_album)
                    itemView.info_type.contentDescription = itemView.context.getString(R.string.album)
                    val tracks = (entry as? Album)?.tracks?.toList()
                    if (!tracks.isNullOrEmpty()) {
                        itemView.info_album_tracks.visibility = View.VISIBLE
                        itemView.info_album_tracks.text = itemView.context.resources.getQuantityString(R.plurals.num_songs, tracks.size, tracks.size)
                        val tracksStr = arrayOfNulls<String>(tracks.size)
                        tracks.forEachIndexed { i, track ->
                            tracksStr[i] = NumberFormat.getInstance().format(i + 1) + ". " + track.name
                            val duration = track.duration
                            if (duration > 0)
                                tracksStr[i] += String.format(" (%02d:%02d)", duration / 60, (duration % 60))
                        }
                        itemView.info_album_tracks.setOnClickListener {
                            val dialog = AlertDialog.Builder(itemView.context, R.style.DarkDialog)
                                    .setTitle(Stuff.getColoredTitle(itemView.context, entry.name))
                                    .setIcon(R.drawable.vd_album)
                                    .setItems(tracksStr) { _, i ->
                                        fragment.dismiss()
                                        val info = InfoFragment()
                                        val b = Bundle()
                                        b.putString(NLService.B_ARTIST, entry.artist)
                                        b.putString(NLService.B_ALBUM, entry.name)
                                        b.putString(NLService.B_TITLE, tracks[i].name)
                                        b.putString(Stuff.ARG_USERNAME, username)
                                        info.arguments = b
                                        info.show(fragment.parentFragmentManager, null)
                                    }
                                    .setNegativeButton(android.R.string.cancel, null)
                                    .create()
                            dialog.listView.onItemSelectedListener = ListViewItemHighlightTvHack()
                            val window = dialog.window
                            val wlp = window!!.attributes
                            wlp.gravity = Gravity.BOTTOM
                            window.attributes = wlp
                            dialog.show()
                        }
                    }
                }
                NLService.B_ARTIST -> {
                    itemView.info_type.setImageResource(R.drawable.vd_mic)
                    itemView.info_type.contentDescription = itemView.context.getString(R.string.artist)
                }
                NLService.B_ALBUM_ARTIST -> {
                    itemView.info_type.setImageResource(R.drawable.vd_album_artist)
                    itemView.info_type.contentDescription = itemView.context.getString(R.string.album_artist)
                }
            }
            itemView.info_name.text = entry?.name
            
            if (entry?.url == null && (viewModel.loadedTypes.contains(key) || !Main.isOnline)) {
                itemView.info_progress.visibility = View.GONE
                return
            }
            if (entry?.url != null) {
                itemView.info_progress.visibility = View.GONE
                itemView.info_content.visibility = View.VISIBLE
                if (username != null)
                    itemView.info_user_scrobbles_label.text = itemView.context.getString(R.string.user_scrobbles, username)
                itemView.info_user_scrobbles.text = NumberFormat.getInstance().format(entry.userPlaycount)
                itemView.info_listeners.text = NumberFormat.getInstance().format(entry.listeners)
                itemView.info_scrobbles.text = NumberFormat.getInstance().format(entry.playcount)
                itemView.info_tags.removeAllViews()
                entry.tags?.forEach {
                    val chip = Chip(itemView.context)
                    chip.text = it
                    chip.isClickable = false
                    chip.isFocusable = false
                    itemView.info_tags.addView(chip)
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
                        itemView.info_wiki.visibility = View.VISIBLE
                        itemView.info_wiki.text = Html.fromHtml(wikiText)
                        itemView.info_wiki.post{
                            if (itemView.info_wiki.lineCount > 2 ||
                                    itemView.info_wiki.layout.getEllipsisCount(itemView.info_wiki.lineCount - 1) > 0) {
                                val clickListener = { view: View ->
                                    if (!(view is TextView && (view.selectionStart != -1 || view.selectionEnd != -1))) {
                                        if (itemView.info_wiki.maxLines == 2) {
                                            itemView.info_wiki.maxLines = 1000
                                            itemView.info_wiki_expand.rotation = 180f
                                        } else {
                                            itemView.info_wiki.maxLines = 2
                                            itemView.info_wiki_expand.rotation = 0f
                                        }
                                    }
                                }
                                itemView.info_wiki.setOnClickListener(clickListener)
                                itemView.info_wiki_expand.setOnClickListener(clickListener)
                                itemView.info_wiki_expand.visibility = View.VISIBLE
                            }
                        }
                    }
                }

                itemView.info_link.visibility = View.VISIBLE
                itemView.info_link.setOnClickListener {
                    if (entry.url != null)
                        Stuff.openInBrowser(entry.url, itemView.context)
                }
            }
        }
    }
}
