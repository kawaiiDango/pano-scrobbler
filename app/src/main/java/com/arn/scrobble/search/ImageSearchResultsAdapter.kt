package com.arn.scrobble.search

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.error
import coil3.request.placeholder
import com.arn.scrobble.R
import com.arn.scrobble.api.spotify.AlbumItem
import com.arn.scrobble.api.spotify.ArtistItem
import com.arn.scrobble.api.spotify.SpotifyMusicItem
import com.arn.scrobble.databinding.ListItemRecentsBinding
import com.arn.scrobble.ui.GenericDiffCallback
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.utils.UiUtils.getTintedDrawable


class ImageSearchResultsAdapter(
    private val context: Context,
    private val clickListener: ItemClickListener<SpotifyMusicItem>
) :
    ListAdapter<SpotifyMusicItem, ImageSearchResultsAdapter.VHImageSearchResult>(
        GenericDiffCallback { o, n -> o.id == n.id }
    ) {

    init {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VHImageSearchResult {
        val inflater = LayoutInflater.from(parent.context)

        return VHImageSearchResult(
            ListItemRecentsBinding.inflate(
                inflater,
                parent,
                false
            ),
            clickListener
        )
    }

    override fun onBindViewHolder(holder: VHImageSearchResult, position: Int) {
        holder.setData(getItem(position))
    }

    class VHImageSearchResult(
        private val binding: ListItemRecentsBinding,
        itemClickListener: ItemClickListener<SpotifyMusicItem>
    ) :
        RecyclerView.ViewHolder(binding.root) {
        private lateinit var musicItem: SpotifyMusicItem

        init {
            binding.recentsMenuText.isVisible = false
            binding.recentsMenu.isInvisible = true
            binding.recentsImgOverlay.isVisible = false
            itemView.setOnClickListener {
                itemClickListener.call(
                    itemView,
                    bindingAdapterPosition
                ) { musicItem }
            }
        }

        fun setData(musicItem: SpotifyMusicItem) {
            this.musicItem = musicItem

            binding.recentsTitle.text = musicItem.name

            if (musicItem is AlbumItem)
                binding.recentsSubtitle.text = musicItem.artists.joinToString { it.name }
            else
                binding.recentsSubtitle.isVisible = false


            val errorDrawable = itemView.context.getTintedDrawable(
                R.drawable.vd_wave_simple_filled,
                musicItem.name.hashCode()
            )

            val imageUrl = when (musicItem) {
                is AlbumItem -> musicItem.mediumImageUrl
                is ArtistItem -> musicItem.mediumImageUrl
                else -> null
            }

            binding.recentsImg.load(imageUrl ?: "") {
                placeholder(R.drawable.avd_loading)
                error(errorDrawable)
            }
        }
    }
}
