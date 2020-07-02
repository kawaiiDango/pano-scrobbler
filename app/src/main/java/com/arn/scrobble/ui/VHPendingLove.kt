package com.arn.scrobble.ui

import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.Main
import com.arn.scrobble.R
import com.arn.scrobble.pending.db.PendingLove
import kotlinx.android.synthetic.main.list_item_recents.view.*

class VHPendingLove(view: View, itemClickListener: ItemClickListener) : RecyclerView.ViewHolder(view) {

    private val vMenu = view.recents_menu
    private val vOverlay = view.recents_img_overlay
    private val vPlaying = view.recents_playing
    private val vDate = view.recents_date
    private val vTitle = view.recents_title
    private val vSubtitle = view.recents_subtitle
    private val vImg = view.recents_img

    init {
        vPlaying.visibility = View.GONE
        vImg.setImageResource(R.drawable.vd_wave_simple)
        vOverlay.visibility = View.VISIBLE
        if (Main.isTV)
            view.setOnClickListener {
                itemClickListener.onItemClick(vMenu, adapterPosition)
            }
        vMenu.setOnClickListener {
            itemClickListener.onItemClick(it, adapterPosition)
        }
    }

    fun setItemData(pl: PendingLove) {
        vTitle.text = pl.track
        vSubtitle.text = pl.artist
        if (pl.shouldLove) {
            vOverlay.background = ContextCompat.getDrawable(itemView.context, R.drawable.vd_heart_stroked)
            vDate.text = itemView.context.getString(R.string.loved)
            vOverlay.contentDescription = itemView.context.getString(R.string.loved)
        } else {
            vOverlay.background = ContextCompat.getDrawable(itemView.context, R.drawable.vd_heart_break_stroked)
            vDate.text = itemView.context.getString(R.string.unloved)
            vOverlay.contentDescription = itemView.context.getString(R.string.unloved)
        }
    }

}