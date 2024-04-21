package com.arn.scrobble.search

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.SearchResults
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.databinding.HeaderWithActionBinding
import com.arn.scrobble.databinding.ListItemRecentsBinding
import com.arn.scrobble.main.App
import com.arn.scrobble.ui.ExpandableHeader
import com.arn.scrobble.ui.MusicEntryImageReq
import com.arn.scrobble.ui.MusicEntryItemClickListener
import com.arn.scrobble.ui.SectionWithHeader
import com.arn.scrobble.ui.SectionedVirtualList
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.format
import com.arn.scrobble.utils.UiUtils.autoNotify
import com.arn.scrobble.utils.UiUtils.fixFocusabilityOnTv
import com.arn.scrobble.utils.UiUtils.getTintedDrawable
import java.text.DateFormat


class SearchResultsAdapter(
    private val context: Context,
    viewModel: SearchVM,
    private val clickListener: MusicEntryItemClickListener
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val data = viewModel.virtualList
    private var searchType = SearchType.GLOBAL

    init {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            SectionedVirtualList.TYPE_HEADER_DEFAULT -> VHSearchHeader(
                HeaderWithActionBinding.inflate(
                    inflater,
                    parent,
                    false
                )
            )

            SectionedVirtualList.TYPE_ITEM_DEFAULT -> VHSearchResult(
                ListItemRecentsBinding.inflate(
                    inflater,
                    parent,
                    false
                )
            )

            else -> throw IllegalArgumentException("Invalid view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            SectionedVirtualList.TYPE_HEADER_DEFAULT -> (holder as VHSearchHeader).setData(data[position] as ExpandableHeader)
            SectionedVirtualList.TYPE_ITEM_DEFAULT ->
                (holder as VHSearchResult).setData(data[position] as MusicEntry)
        }
    }

    override fun getItemViewType(position: Int) = data.getItemType(position)

    override fun getItemCount() = data.size

    fun getItem(pos: Int) = data[pos]

    fun populate(searchResults: SearchResults) {
        searchType = searchResults.searchType
        data.removeSection(Section.NOT_FOUND)
        data.addSection(
            SectionWithHeader(
                Section.ARTISTS,
                searchResults.artists,
                header = ExpandableHeader(
                    R.drawable.vd_mic,
                    R.string.artists,
                )
            )
        )
        data.addSection(
            SectionWithHeader(
                Section.ALBUMS,
                searchResults.albums,
                header = ExpandableHeader(
                    R.drawable.vd_album,
                    R.string.albums,
                )
            )
        )
        data.addSection(
            SectionWithHeader(
                Section.TRACKS,
                searchResults.tracks,
                header = ExpandableHeader(
                    R.drawable.vd_note,
                    R.string.tracks,
                )
            )
        )
        data.addSection(
            SectionWithHeader(
                Section.LOVED_TRACKS,
                searchResults.lovedTracks,
                header = ExpandableHeader(
                    R.drawable.vd_heart,
                    R.string.loved,
                )
            )
        )


        data.removeSection(Section.REINDEX)

        if (searchType == SearchType.LOCAL) {
            var lastIndexedInfo = context.getString(
                R.string.searched_n_items,
                Stuff.MAX_INDEXED_ITEMS.format()
            )
            lastIndexedInfo += "\n" + context.getString(
                R.string.last_indexed,
                DateFormat.getDateInstance()
                    .format(App.prefs.lastMaxIndexTime ?: 0)
            )
            data.addSection(
                SectionWithHeader(
                    Section.REINDEX,
                    emptyList(),
                    header = ExpandableHeader(
                        R.drawable.vd_info,
                        lastIndexedInfo,
                        "",
                        "",
                    ),
                    showHeaderWhenEmpty = true
                )
            )
        }

        notifyDataSetChanged()
    }

    inner class VHSearchResult(private val binding: ListItemRecentsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.recentsMenu.visibility = View.INVISIBLE
            binding.recentsImgOverlay.setBackgroundResource(R.drawable.vd_heart_stroked)

            binding.recentsImgFrame.background = null
            binding.recentsTrackLl.background = null

            binding.root.isFocusable = true
            binding.root.setOnClickListener {
                clickListener.onItemClick(
                    itemView,
                    data[bindingAdapterPosition] as? MusicEntry ?: return@setOnClickListener
                )
            }
        }

        fun setData(entry: MusicEntry) {
            binding.recentsTitle.text = entry.name
            if ((entry.listeners ?: 0) > 0 && searchType == SearchType.GLOBAL) {
                binding.recentsDate.text = itemView.context.resources.getQuantityString(
                    R.plurals.num_listeners,
                    entry.listeners!!,
                    entry.listeners!!.format()
                )
            } else if ((entry.userplaycount ?: 0) > 0 && searchType == SearchType.LOCAL) {
                binding.recentsDate.text = itemView.context.resources.getQuantityString(
                    R.plurals.num_scrobbles_noti,
                    entry.userplaycount!!,
                    entry.userplaycount!!.format()
                )
            } else {
                binding.recentsDate.text = ""
            }
            when (entry) {
                is Album -> binding.recentsSubtitle.text = entry.artist!!.name
                is Track -> binding.recentsSubtitle.text = entry.artist.name
                else -> binding.recentsSubtitle.text = ""
            }

            binding.recentsImgOverlay.visibility =
                if ((entry as? Track)?.userloved == true) View.VISIBLE else View.INVISIBLE

            val errorDrawable = itemView.context.getTintedDrawable(
                R.drawable.vd_wave_simple_filled,
                entry.name.hashCode()
            )

            if (entry is Album && searchType == SearchType.GLOBAL) {
                binding.recentsImg.load(entry) {
                    placeholder(R.drawable.avd_loading)
                    error(errorDrawable)
                }
            } else {
                binding.recentsImg.load(MusicEntryImageReq(entry, fetchAlbumInfoIfMissing = true)) {
                    placeholder(R.drawable.avd_loading)
                    error(errorDrawable)
                }
            }
        }
    }

    inner class VHSearchHeader(private val binding: HeaderWithActionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun setData(headerData: ExpandableHeader) {
            if (headerData.section.listSize > 3) {
                binding.headerAction.visibility = View.VISIBLE
                binding.headerAction.text = headerData.actionText
                binding.fixFocusabilityOnTv()
            } else {
                binding.headerAction.visibility = View.GONE
                binding.root.setOnClickListener(null)
                binding.root.isClickable = false
                binding.root.isFocusable = false
            }
            binding.headerText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                ContextCompat.getDrawable(
                    itemView.context,
                    headerData.iconRes
                ), null, null, null
            )
            binding.headerText.text = headerData.title

            if (headerData.section.sectionId != Section.REINDEX) {
                binding.headerText.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)

                binding.headerAction.setOnClickListener {
                    (data[bindingAdapterPosition] as? ExpandableHeader)?.let {
                        val oldData = data.copy()
                        data.toggleOne(it.section.sectionId)
                        autoNotify(oldData, data) { o, n ->
                            o is ExpandableHeader && n is ExpandableHeader
                                    // prevent change animation, check for contents later
                                    ||
                                    o === n
                        }
                    }
                }
            }
        }
    }

    private enum class Section {
        ARTISTS,
        ALBUMS,
        TRACKS,
        LOVED_TRACKS,
        NOT_FOUND,
        REINDEX
    }

    enum class SearchType {
        GLOBAL,
        LOCAL,
    }
}
