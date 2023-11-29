package com.arn.scrobble.info

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.databinding.ListItemAlbumTracksBinding
import com.arn.scrobble.ui.GenericDiffCallback
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.format


class AlbumTracksAdapter(private val tracks: List<Track>) :
    ListAdapter<Track, AlbumTracksAdapter.VHAlbumTrack>(
        GenericDiffCallback { o, n -> o.name == n.name }
    ) {

    lateinit var itemClickListener: ItemClickListener<Track>

    init {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY

        submitList(tracks)
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

    inner class VHAlbumTrack(
        private val binding: ListItemAlbumTracksBinding,
        itemClickListener: ItemClickListener<Track>
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                itemClickListener.call(
                    it,
                    bindingAdapterPosition
                ) { getItem(bindingAdapterPosition) }
            }
        }

        fun setItemData(pos: Int, track: Track) {
            binding.albumTrackNumber.text = (pos + 1).format()
            binding.albumTrackName.text = track.name
            if (track.duration != null)
                binding.albumTrackDuration.text = Stuff.humanReadableDuration(track.duration)
            else
                binding.albumTrackDuration.text = ""
        }
    }
}
