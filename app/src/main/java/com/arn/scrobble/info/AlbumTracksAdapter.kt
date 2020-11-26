package com.arn.scrobble.info

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.ui.ItemClickListener
import de.umass.lastfm.Track
import kotlinx.android.synthetic.main.list_item_album_tracks.view.*
import java.text.NumberFormat


class AlbumTracksAdapter(private val tracks: List<Track>) : RecyclerView.Adapter<AlbumTracksAdapter.VHAlbumTrack>() {

    lateinit var itemClickListener: ItemClickListener

    init {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHAlbumTrack{
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.list_item_album_tracks, parent, false)
        return VHAlbumTrack(view, itemClickListener)
    }

    override fun getItemCount() = tracks.size

    override fun onBindViewHolder(holder: VHAlbumTrack, position: Int) {
        holder.setItemData(position, tracks[position])
    }

    class VHAlbumTrack(view: View, itemClickListener: ItemClickListener): RecyclerView.ViewHolder(view){

        private val vNum = view.album_track_number
        private val vTitle = view.album_track_name
        private val vDuration = view.album_track_duration

        init {
            view.setOnClickListener { itemClickListener.onItemClick(it,  adapterPosition) }
        }

        fun setItemData(pos: Int, track: Track) {
            vNum.text = NumberFormat.getInstance().format(pos + 1)
            vTitle.text = track.name
            if (track.duration > 0)
                vDuration.text = Stuff.humanReadableDuration(track.duration)
            else
                vDuration.text = ""
        }
    }
}
