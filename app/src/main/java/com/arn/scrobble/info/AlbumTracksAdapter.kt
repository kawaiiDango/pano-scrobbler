package com.arn.scrobble.info

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.Stuff
import com.arn.scrobble.databinding.ListItemAlbumTracksBinding
import com.arn.scrobble.ui.ItemClickListener
import de.umass.lastfm.Track
import java.text.NumberFormat


class AlbumTracksAdapter(private val tracks: List<Track>) :
    RecyclerView.Adapter<AlbumTracksAdapter.VHAlbumTrack>() {

    lateinit var itemClickListener: ItemClickListener

    init {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHAlbumTrack {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ListItemAlbumTracksBinding.inflate(inflater, parent, false)
        return VHAlbumTrack(binding, itemClickListener)
    }

    override fun getItemCount() = tracks.size

    override fun onBindViewHolder(holder: VHAlbumTrack, position: Int) {
        holder.setItemData(position, tracks[position])
    }

    class VHAlbumTrack(
        private val binding: ListItemAlbumTracksBinding,
        itemClickListener: ItemClickListener
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener { itemClickListener.call(it, bindingAdapterPosition) }
        }

        fun setItemData(pos: Int, track: Track) {
            binding.albumTrackNumber.text = NumberFormat.getInstance().format(pos + 1)
            binding.albumTrackName.text = track.name
            if (track.duration > 0)
                binding.albumTrackDuration.text = Stuff.humanReadableDuration(track.duration)
            else
                binding.albumTrackDuration.text = ""
        }
    }
}
