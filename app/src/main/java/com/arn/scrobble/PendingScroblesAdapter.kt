package com.arn.scrobble

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.arn.scrobble.db.PendingScrobble
import kotlinx.android.synthetic.main.list_item_recents.view.*

/**
 * Created by arn on 21/09/2017.
 */
class PendingScroblesAdapter

(c: Context, private val layoutResourceId: Int) : ArrayAdapter<PendingScrobble>(c, layoutResourceId, mutableListOf()) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView: View? = convertView
        if (convertView == null) {
            // inflate the layout
            val inflater = (context as Activity).layoutInflater
            convertView = inflater.inflate(layoutResourceId, parent, false)!!
        }
        val ps = getItem(position)
        populateItem(context, convertView, ps)

        return convertView
    }
    companion object {
        fun populateItem(context: Context, view:View, data:PendingScrobble): View{
            view.recents_love.visibility = View.INVISIBLE
            view.recents_playing.visibility = View.GONE

            view.recents_title.text = data.track
            view.recents_subtitle.text = data.artist
            view.recents_date.text = Stuff.myRelativeTime(context, data.timestamp)
            view.recents_play.setImageResource(R.drawable.vd_hourglass)
            return view
        }
    }
}
