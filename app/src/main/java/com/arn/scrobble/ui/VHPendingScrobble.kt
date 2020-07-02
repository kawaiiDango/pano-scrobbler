package com.arn.scrobble.ui

import android.view.View
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.Main
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.pending.db.PendingScrobble
import kotlinx.android.synthetic.main.list_item_recents.view.*

class VHPendingScrobble(view: View, itemClickListener: ItemClickListener) : RecyclerView.ViewHolder(view) {

    private val vMenu = view.recents_menu
    private val vOverlay = view.recents_img_overlay
    private val vPlaying = view.recents_playing
    private val vDate = view.recents_date
    private val vTitle = view.recents_title
    private val vSubtitle = view.recents_subtitle
    private val vImg = view.recents_img

    init {
        vPlaying.visibility = View.GONE
        vOverlay.background = ContextCompat.getDrawable(view.context, R.drawable.vd_hourglass)
        vImg.setImageResource(R.drawable.vd_wave_simple)
        vOverlay.visibility = View.VISIBLE
        vOverlay.contentDescription = view.context.getString(R.string.pending_scrobble)
        if (Main.isTV)
            view.setOnClickListener {
                itemClickListener.onItemClick(vMenu, adapterPosition)
            }
        vMenu.setOnClickListener {
            itemClickListener.onItemClick(it, adapterPosition)
        }
    }

    fun setItemData(ps: PendingScrobble) {
        vTitle.text = ps.track
        vSubtitle.text = ps.artist
        vDate.text = Stuff.myRelativeTime(vDate.context, ps.timestamp)
    }

}