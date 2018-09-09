package com.arn.scrobble.ui

import android.view.View
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.pending.db.PendingScrobble
import kotlinx.android.synthetic.main.list_item_recents.view.*

class VHPending(view: View) : RecyclerView.ViewHolder(view) {

    private val vMenu = view.recents_menu
    private val vOverlay = view.recents_img_overlay
    private val vPlaying = view.recents_playing
    private val vDate = view.recents_date
    private val vTitle = view.recents_title
    private val vSubtitle = view.recents_subtitle
    private val vImg = view.recents_img

    init {
        vMenu.visibility = View.INVISIBLE
        vPlaying.visibility = View.GONE
        vOverlay.background = view.context.getDrawable(R.drawable.vd_hourglass)
        vImg.setImageResource(R.drawable.vd_wave_simple)

        val margin = Stuff.dp2px(30, view.context)
        (vOverlay.layoutParams as RelativeLayout.LayoutParams)
                .setMargins(0, margin/4, 0, margin)
        vOverlay.visibility = View.VISIBLE
    }

    fun setItemData(ps: PendingScrobble) {
        vTitle.text = ps.track
        vSubtitle.text = ps.artist
        vDate.text = Stuff.myRelativeTime(vDate.context, ps.timestamp)
    }

}