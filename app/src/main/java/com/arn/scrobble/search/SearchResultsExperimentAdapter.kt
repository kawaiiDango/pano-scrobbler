package com.arn.scrobble.search

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.arn.scrobble.MainActivity
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.ui.UiUtils.autoNotify
import com.arn.scrobble.ui.UiUtils.getTintedDrawable
import com.arn.scrobble.databinding.HeaderWithActionBinding
import com.arn.scrobble.databinding.ListItemRecentsBinding
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.ui.*
import de.umass.lastfm.Album
import de.umass.lastfm.ImageSize
import de.umass.lastfm.MusicEntry
import de.umass.lastfm.Track
import java.text.DateFormat
import java.text.NumberFormat


class SearchResultsExperimentAdapter(
    private val context: Context,
    private val viewModel: SearchVM,
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
            else -> throw RuntimeException("Invalid view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            SectionedVirtualList.TYPE_HEADER_DEFAULT -> (holder as VHSearchHeader).setData(data[position] as ExpandableHeader)
            SectionedVirtualList.TYPE_ITEM_DEFAULT ->
                (holder as VHSearchResult).setData(data[position] as MusicEntry)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return data.getItemType(position)
    }

    override fun getItemCount() = data.size

    fun getItem(pos: Int) = data[pos]

    fun populate(searchResults: SearchVM.SearchResults) {
        searchType = searchResults.searchType

        if (searchResults.isEmpty) {
            data.clear()
            data.addSection(
                SectionWithHeader(
                    Section.NOT_FOUND,
                    emptyList(),
                    header = ExpandableHeader(
                        R.drawable.vd_ban,
                        if (!Stuff.isOnline)
                            context.getString(R.string.unavailable_offline)
                        else
                            context.getString(R.string.not_found),
                    ),
                    showHeaderWhenEmpty = true
                )
            )
        } else {
            data.removeSection(Section.NOT_FOUND)
            data.addSection(
                SectionWithHeader(
                    Section.ARTISTS,
                    searchResults.artists,
                    header = ExpandableHeader(
                        context,
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
                        context,
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
                        context,
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
                        context,
                        R.drawable.vd_heart,
                        R.string.loved,
                    )
                )
            )
        }

        data.removeSection(Section.REINDEX)

        if (searchType == SearchType.LOCAL) {
            var lastIndexedInfo = context.getString(
                R.string.searched_n_items,
                NumberFormat.getInstance().format(Stuff.MAX_INDEXED_ITEMS)
            )
            lastIndexedInfo += "\n" + context.getString(
                R.string.last_indexed,
                DateFormat.getDateInstance()
                    .format(MainPrefs(context).lastMaxIndexTime ?: 0)
            )
            data.addSection(
                SectionWithHeader(
                    Section.REINDEX,
                    emptyList(),
                    header = ExpandableHeader(
                        R.drawable.vd_info,
                        lastIndexedInfo,
                        context.getString(R.string.reindex),
                        context.getString(R.string.reindex),
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
            binding.recentsMenuText.visibility = View.GONE
            binding.recentsMenu.visibility = View.INVISIBLE
            binding.recentsImgOverlay.setBackgroundResource(R.drawable.vd_heart_stroked)
            itemView.setOnClickListener {
                clickListener.onItemClick(
                    itemView,
                    data[bindingAdapterPosition] as? MusicEntry ?: return@setOnClickListener
                )
            }
        }

        fun setData(entry: MusicEntry) {
            binding.recentsTitle.text = entry.name
            if (entry.listeners > 0 && searchType == SearchType.GLOBAL) {
                binding.recentsDate.text = itemView.context.resources.getQuantityString(
                    R.plurals.num_listeners,
                    entry.listeners,
                    NumberFormat.getInstance().format(entry.listeners)
                )
            } else if (entry.userPlaycount > 0 && searchType == SearchType.LOCAL) {
                binding.recentsDate.text = itemView.context.resources.getQuantityString(
                    R.plurals.num_scrobbles_noti,
                    entry.userPlaycount,
                    NumberFormat.getInstance().format(entry.userPlaycount)
                )
            } else {
                binding.recentsDate.text = ""
            }
            when (entry) {
                is Album -> binding.recentsSubtitle.text = entry.artist
                is Track -> binding.recentsSubtitle.text = entry.artist
                else -> binding.recentsSubtitle.text = ""
            }

            binding.recentsImgOverlay.visibility =
                if ((entry as? Track)?.isLoved == true) View.VISIBLE else View.INVISIBLE

            val errorDrawable = itemView.context.getTintedDrawable(
                R.drawable.vd_wave_simple_filled,
                entry.name.hashCode()
            )

            if (entry is Album && searchType == SearchType.GLOBAL) {
                binding.recentsImg.load(entry.getWebpImageURL(ImageSize.LARGE) ?: "") {
                    placeholder(R.drawable.vd_wave_simple_filled)
                    error(errorDrawable)
                    allowHardware(false)
                }
            } else {
                binding.recentsImg.load(MusicEntryImageReq(entry, ImageSize.LARGE, false)) {
                    placeholder(R.drawable.vd_wave_simple_filled)
                    error(errorDrawable)
                    allowHardware(false) // crashes on back otherwise
                }
            }
        }
    }

    inner class VHSearchHeader(private val binding: HeaderWithActionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun setData(headerData: ExpandableHeader) {
            if (headerData.section.listSize > 3 || headerData.section.sectionId == Section.REINDEX) {
                binding.headerAction.visibility = View.VISIBLE
                binding.headerAction.text = headerData.actionText
            } else {
                binding.headerAction.visibility = View.GONE
            }
            binding.headerText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                ContextCompat.getDrawable(
                    itemView.context,
                    headerData.iconRes
                ), null, null, null
            )
            binding.headerText.text = headerData.title

            if (headerData.section.sectionId == Section.REINDEX) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    binding.headerText.setTextAppearance(R.style.TextAppearance_Material3_BodyMedium)
                }

                binding.headerAction.setOnClickListener {
                    viewModel.fullIndex()
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    binding.headerText.setTextAppearance(R.style.TextAppearance_Material3_TitleMedium)
                }

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

    enum class Section {
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
