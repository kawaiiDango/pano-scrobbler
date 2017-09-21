package com.arn.scrobble

import android.app.Activity
import android.content.Context
import android.text.format.DateUtils
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.arn.scrobble.db.PendingScrobble

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
        populateItem(convertView, ps)

        return convertView
    }
    companion object {
        fun populateItem(view:View, data:PendingScrobble): View{
            val title = view.findViewById<TextView>(R.id.recents_title)
            val subtitle = view.findViewById<TextView>(R.id.recents_subtitle)
            val date = view.findViewById<TextView>(R.id.recents_date)
            val love = view.findViewById<ImageView>(R.id.recents_love)
            val play = view.findViewById<ImageView>(R.id.recents_play)
            val playing = view.findViewById<ImageView>(R.id.recents_playing)

            love.visibility = View.INVISIBLE
            playing.visibility = View.GONE

            title.text = data.track
            subtitle.text = data.artist
            var relDate = DateUtils.getRelativeTimeSpanString(
                    data.timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
            if (relDate[0] == '0')
                relDate = "Just now"
            date.text = relDate
            play.setImageResource(R.drawable.vd_hourglass)
            return view
        }
    }
}
