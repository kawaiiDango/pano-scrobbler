package com.arn.scrobble

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.GridView
import com.squareup.picasso.Picasso
import de.umass.lastfm.ImageSize
import de.umass.lastfm.Track
import kotlinx.android.synthetic.main.content_similar.*
import kotlinx.android.synthetic.main.header_default.view.*
import kotlinx.android.synthetic.main.list_item_recents.view.*

/**
 * Created by arn on 29/12/2017.
 */
class SimilarTracksAdapter

(c: Context, private val layoutResourceId: Int) : ArrayAdapter<Track>(c, layoutResourceId, mutableListOf()) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView: View? = convertView
        if (convertView == null) {
            // inflate the layout
            val inflater = (context as Activity).layoutInflater
            convertView = inflater.inflate(layoutResourceId, parent, false)!!
            (convertView as ViewGroup).layoutTransition?.setDuration(100)
        }
        val t = getItem(position) ?: return convertView

        convertView.recents_love.visibility = View.GONE
        convertView.recents_playing.visibility = View.GONE
        convertView.recents_date.visibility = View.GONE

        convertView.recents_title.text = t.name
        convertView.recents_subtitle.text = t.artist

        if ((parent as GridView).isItemChecked(position)){
            convertView.recents_play.visibility = View.VISIBLE
        } else
            convertView.recents_play.visibility = View.GONE

        val imgUrl = t.getImageURL(ImageSize.LARGE)

        if (imgUrl != null && imgUrl != "") {
            convertView.recents_album_art.clearColorFilter()
            Picasso.with(context)
                    .load(imgUrl)
                    .fit()
                    .centerCrop()
                    .placeholder(R.drawable.ic_placeholder_music)
                    .error(R.drawable.ic_placeholder_music)
                    .into(convertView.recents_album_art)

        } else {
            convertView.recents_album_art.setImageResource(R.drawable.ic_placeholder_music)
            convertView.recents_album_art.setColorFilter(Stuff.getMatColor(context, "500", t.name.hashCode().toLong()))
        }

        return convertView
    }

    fun populate(res: ArrayList<Track>){
        if (res.size == 0)
            (context as Activity).similar_grid_container?.header_text?.text = context.getString(R.string.no_similar_tracks)
        else
            addAll(res)
    }
}
