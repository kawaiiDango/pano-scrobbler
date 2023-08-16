package com.arn.scrobble.recents

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePaddingRelative
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import coil.dispose
import coil.load
import coil.memory.MemoryCache
import coil.size.Scale
import com.arn.scrobble.App
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.databinding.ContentScrobblesBinding
import com.arn.scrobble.databinding.HeaderWithActionBinding
import com.arn.scrobble.databinding.ListItemRecentsBinding
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.PendingLove
import com.arn.scrobble.db.PendingScrobble
import com.arn.scrobble.pending.VHPendingLove
import com.arn.scrobble.pending.VHPendingScrobble
import com.arn.scrobble.ui.EndlessRecyclerViewScrollListener
import com.arn.scrobble.ui.ExpandableHeader
import com.arn.scrobble.ui.FocusChangeListener
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.ui.ItemLongClickListener
import com.arn.scrobble.ui.LoadMoreGetter
import com.arn.scrobble.ui.MusicEntryImageReq
import com.arn.scrobble.ui.PackageName
import com.arn.scrobble.ui.SectionWithHeader
import com.arn.scrobble.ui.SectionedVirtualList
import com.arn.scrobble.ui.UiUtils
import com.arn.scrobble.ui.UiUtils.autoNotify
import com.arn.scrobble.ui.UiUtils.dp
import com.arn.scrobble.ui.UiUtils.getTintedDrawable
import com.arn.scrobble.ui.UiUtils.memoryCacheKey
import com.arn.scrobble.ui.UiUtils.showWithIcons
import de.umass.lastfm.ImageSize
import de.umass.lastfm.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Objects


/**
 * Created by arn on 10/07/2017.
 */

class ScrobblesAdapter(
    private val fragmentBinding: ContentScrobblesBinding,
    private val itemClickListener: ItemClickListener,
    private val itemLongClickListener: ItemLongClickListener,
    private val focusChangeListener: FocusChangeListener,
    private val setHeroListener: SetHeroTrigger,
    private val viewModel: TracksVM,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), LoadMoreGetter {
    override lateinit var loadMoreListener: EndlessRecyclerViewScrollListener
    var isShowingAlbums = false
    var isShowingPlayers = false
    private var lastPopulateTime = System.currentTimeMillis()
    private val playerDao = PanoDb.db.getScrobbleSourcesDao()
    private val prefs = App.prefs

    init {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            SectionedVirtualList.TYPE_HEADER_DEFAULT -> VHHeader(
                HeaderWithActionBinding.inflate(inflater, parent, false)
            )

            Section.SCROBBLES.ordinal -> VHScrobble(
                ListItemRecentsBinding.inflate(inflater, parent, false)
            )

            Section.PENDING_SCROBBLES.ordinal -> VHPendingScrobble(
                ListItemRecentsBinding.inflate(inflater, parent, false),
                isShowingAlbums,
                itemClickListener
            )

            Section.PENDING_LOVES.ordinal -> VHPendingLove(
                ListItemRecentsBinding.inflate(inflater, parent, false),
                isShowingAlbums,
                itemClickListener
            )

            else -> throw IllegalArgumentException("Invalid view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is VHScrobble -> holder.setItemData(viewModel.virtualList[position] as Track)
            is VHPendingScrobble -> holder.setItemData(viewModel.virtualList[position] as PendingScrobble)
            is VHPendingLove -> holder.setItemData(viewModel.virtualList[position] as PendingLove)
            is VHHeader -> holder.setItemData(viewModel.virtualList[position] as ExpandableHeader)
            else -> throw ClassCastException("Invalid view type $holder")
        }
    }

    override fun getItemViewType(position: Int) = viewModel.virtualList.getItemType(position)

    fun getItem(pos: Int) = viewModel.virtualList[pos]

    override fun getItemCount() = viewModel.virtualList.size

    fun updateTracks(tracks: List<Track>) {
        viewModel.virtualList.addSection(
            SectionWithHeader(
                Section.SCROBBLES,
                tracks,
                Section.SCROBBLES.ordinal,
                header = null
            )
        )
    }

    fun updatePendingScrobbles(pendingScrobbles: List<PendingScrobble>, notify: Boolean = true) {
        val list = if (!viewModel.isShowingLoves && viewModel.username == null)
            pendingScrobbles
        else
            emptyList()

        val oldVirtualList = viewModel.virtualList.copy()

        viewModel.virtualList.addSection(
            SectionWithHeader(
                Section.PENDING_SCROBBLES,
                list,
                Section.PENDING_SCROBBLES.ordinal,
                header = ExpandableHeader(
                    R.drawable.vd_hourglass,
                    R.string.scrobbles,
                    maxCollapsedItems = 2
                )
            )
        )

        if (notify)
            notify(oldVirtualList)
    }

    fun updatePendingLoves(pendingLoves: List<PendingLove>, notify: Boolean = true) {
        val list = if (!viewModel.isShowingLoves && viewModel.username == null)
            pendingLoves
        else
            emptyList()

        val oldVirtualList = viewModel.virtualList.copy()

        viewModel.virtualList.addSection(
            SectionWithHeader(
                Section.PENDING_LOVES,
                list,
                Section.PENDING_LOVES.ordinal,
                header = ExpandableHeader(
                    R.drawable.vd_hourglass,
                    R.string.loved,
                    maxCollapsedItems = 2
                )
            )
        )

        if (notify)
            notify(oldVirtualList)

    }

    fun removeTrack(track: Track) {
        val idx = viewModel.virtualList
            .indexOfFirst {
                it is Track && it.playedWhen == track.playedWhen &&
                        it.name == track.name && it.artist == track.artist
            }

        if (idx == -1) return
        val foundTrack = viewModel.virtualList[idx] as Track

        viewModel.deletedTracksStringSet += track.toString()

        synchronized(viewModel.tracks) {
            viewModel.tracks.remove(foundTrack)
        }

        updateTracks(viewModel.tracks.toList())

        notifyItemRemoved(idx)
    }

    fun editTrack(track: Track) {
        val idx =
            viewModel.virtualList.indexOfFirst { it is Track && it.playedWhen == track.playedWhen }
        val idxTracks = viewModel.tracks.indexOfFirst { it.playedWhen == track.playedWhen }

        if (idx == -1 || idxTracks == -1) return
        val prevTrack = viewModel.virtualList[idx] as Track

        if (prevTrack.artist == track.artist && prevTrack.album == track.album && prevTrack.name == track.name)
            return

        viewModel.tracks[idxTracks] = track
        updateTracks(viewModel.tracks.toList())
        viewModel.deletedTracksStringSet += prevTrack.toString()
        notifyItemChanged(idx)
    }

    fun populate() {
        val oldVirtualList = viewModel.virtualList.copy()
        val prevSelectedItem =
            if (viewModel.virtualList.isEmpty())
                null
            else
                viewModel.virtualList.getOrNull(viewModel.selectedPos)
        val firstTrack = viewModel.virtualList[Section.SCROBBLES]?.items?.firstOrNull()
        val firstTrackSelected = prevSelectedItem === firstTrack

        updatePendingScrobbles(viewModel.pendingScrobblesLd.value ?: listOf(), false)
        updatePendingLoves(viewModel.pendingLovesLd.value ?: listOf(), false)
        updateTracks(viewModel.tracks.toList())

        if (!firstTrackSelected && prevSelectedItem is Track) {
            val pos =
                viewModel.virtualList.indexOfFirst { it is Track && it.playedWhen?.time == prevSelectedItem.playedWhen?.time }
            if (pos != -1) {
                viewModel.selectedPos = pos
            }
        } else {
            viewModel.selectedPos = viewModel.virtualList.indexOfFirst { it is Track }
        }


        if (oldVirtualList.isEmpty() && viewModel.virtualList.isNotEmpty() || fragmentBinding.swipeRefresh.isRefreshing) {
            fragmentBinding.scrobblesList.scheduleLayoutAnimation()
            notifyItemChanged(0, 0) //animation gets delayed otherwise
        } else if (oldVirtualList.size < viewModel.virtualList.size) // remove the loading gap from the last item
            notifyItemChanged(itemCount - 1, 0)
        notify(oldVirtualList, true, prevSelectedItem)

        lastPopulateTime = System.currentTimeMillis()
    }

    private fun notify(
        oldVirtualList: SectionedVirtualList,
        allHaveChanged: Boolean = false,
        prevSelectedItem: Any? = null
    ) {
        autoNotify(oldVirtualList,
            viewModel.virtualList,
            compare = { oldItem, newItem ->
                when {
                    oldItem is ExpandableHeader && newItem is ExpandableHeader -> oldItem.title == newItem.title
                    oldItem is PendingScrobble && newItem is PendingScrobble -> oldItem._id == newItem._id
                    oldItem is PendingLove && newItem is PendingLove -> oldItem._id == newItem._id
                    oldItem is Track && newItem is Track -> oldItem.playedWhen?.time == newItem.playedWhen?.time
                    else -> false
                }
            },
            compareContents = { oldItem, newItem ->
                when {
                    allHaveChanged -> false
                    oldItem === prevSelectedItem -> false // clears the previous selection
                    System.currentTimeMillis() - lastPopulateTime > 60 * 60 * 1000 -> false
                    oldItem is ExpandableHeader && newItem is ExpandableHeader -> oldItem == newItem
                    oldItem is PendingScrobble && newItem is PendingScrobble -> oldItem == newItem
                    oldItem is PendingLove && newItem is PendingLove -> oldItem == newItem
                    oldItem is Track && newItem is Track -> oldItem.name == newItem.name &&
                            oldItem.album == newItem.album && oldItem.artist == newItem.artist &&
                            oldItem.isLoved == newItem.isLoved &&
                            oldItem.getImageURL(ImageSize.MEDIUM) == newItem.getImageURL(ImageSize.MEDIUM)

                    else -> false
                }
            }
        )
    }

    inner class VHScrobble(private val binding: ListItemRecentsBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnFocusChangeListener {
        private var job: Job? = null

        init {
            binding.root.setOnClickListener {
                itemClickListener.call(itemView, bindingAdapterPosition)
            }
            binding.root.setOnLongClickListener {
                itemLongClickListener.call(itemView, bindingAdapterPosition)
                true
            }
            binding.root.onFocusChangeListener = this
            binding.recentsMenu.setOnClickListener {
                itemClickListener.call(it, bindingAdapterPosition)
            }
        }

        override fun onFocusChange(view: View?, focused: Boolean) {
            if (view != null && !view.isInTouchMode && focused)
                focusChangeListener.call(itemView, bindingAdapterPosition)
        }

        private fun setSelected(
            selected: Boolean,
            item: Any = viewModel.virtualList[viewModel.selectedPos]
        ) {
            if (item is Track) {
                itemView.isActivated = selected
                if (selected)
                    setHeroListener.onSetHero(item, binding.recentsImg.memoryCacheKey)
            }
        }

        private fun setPlayerIcon(track: Track) {
            val timeMillis = track.playedWhen?.time
            binding.playerIcon.visibility = View.VISIBLE

            fun fetchIcon(pkgName: String) {
                binding.playerIcon.load(PackageName(pkgName)) {
                    scale(Scale.FIT)
                    listener(onSuccess = { _, _ ->
                        binding.playerIcon.contentDescription = pkgName
                    })
                }
            }

            if (timeMillis != null && viewModel.pkgMap[timeMillis] != null) {
                fetchIcon(viewModel.pkgMap[timeMillis]!!)
            } else {
                binding.playerIcon.dispose()
                binding.playerIcon.load(null)
                binding.playerIcon.contentDescription = null
                job?.cancel()

                if (timeMillis != null) {
                    job = viewModel.viewModelScope.launch(Dispatchers.IO) {
                        delay(100)
                        playerDao.findPlayer(timeMillis)?.pkg?.let { pkgName ->
                            viewModel.pkgMap[timeMillis] = pkgName
                            fetchIcon(pkgName)
                        }
                    }
                }
            }
        }

        fun setItemData(track: Track) {
            binding.recentsTitle.text = track.name
            binding.recentsSubtitle.text = track.artist

            if (isShowingAlbums) {
                if (!track.album.isNullOrEmpty()) {
                    binding.recentsAlbum.text = track.album
                    binding.recentsAlbum.visibility = View.VISIBLE
                    binding.recentsTrackLl.setPaddingRelative(
                        0,
                        0,
                        0,
                        0
                    )
                } else {
                    val albumHeight =
                        itemView.context.resources.getDimension(R.dimen.album_text_height).toInt()
                    binding.recentsAlbum.visibility = View.GONE
                    binding.recentsTrackLl.setPaddingRelative(
                        0,
                        albumHeight / 2,
                        0,
                        albumHeight / 2
                    )
                }
            }

            if (track.isNowPlaying) {
                binding.recentsDate.visibility = View.GONE
                UiUtils.nowPlayingAnim(binding.recentsPlaying, true)
                binding.root.updatePaddingRelative(top = 0)
                binding.dividerCircle.isVisible = false
            } else {
                binding.recentsDate.visibility = View.VISIBLE
                binding.recentsDate.text =
                    Stuff.myRelativeTime(itemView.context, track.playedWhen?.time ?: 0)

                if (track.isLastScrobbleOfTheDay) {
                    binding.root.updatePaddingRelative(top = 16.dp)
                    binding.recentsDate.typeface = Typeface.DEFAULT_BOLD
                    binding.dividerCircle.isVisible = true
                } else {
                    binding.recentsDate.typeface = Typeface.DEFAULT
                    binding.root.updatePaddingRelative(top = 0)
                    binding.dividerCircle.isVisible = false
                }


                UiUtils.nowPlayingAnim(binding.recentsPlaying, false)
            }

            if (track.isLoved || track.isHated) {
                binding.recentsImgOverlay.background = ContextCompat.getDrawable(
                    binding.recentsImgOverlay.context,
                    if (track.isLoved)
                        R.drawable.vd_heart_stroked
                    else
                        R.drawable.vd_heart_break_stroked
                )
                binding.recentsImgOverlay.visibility = View.VISIBLE
            } else {
                binding.recentsImgOverlay.visibility = View.INVISIBLE
            }

            if (isShowingPlayers) {
                setPlayerIcon(track)
            }

            val imgUrl = track.getWebpImageURL(ImageSize.LARGE)
            val errorDrawable = itemView.context.getTintedDrawable(
                R.drawable.vd_wave_simple_filled,
                Objects.hash(track.artist, track.name)
            )

            if (!viewModel.isShowingLoves) {
                binding.recentsImg.load(imgUrl ?: "") {
                    placeholder(R.drawable.vd_wave_simple_filled)
                    error(errorDrawable)
                }
            } else {
                val musicEntryImageReq = MusicEntryImageReq(track, ImageSize.LARGE, true)
                binding.recentsImg.load(musicEntryImageReq) {
                    placeholder(R.drawable.vd_wave_simple_filled)
                    error(errorDrawable)
                    listener(onSuccess = { request, _ ->
                        (request.data as? String)?.let {
                            if (bindingAdapterPosition == viewModel.selectedPos) {
                                val idx = bindingAdapterPosition
                                setHeroListener.onSetHero(
                                    viewModel.virtualList[idx] as Track,
                                    binding.recentsImg.memoryCacheKey
                                )
                            }
                        }
                    })
                }
            }

            if (prefs.themeTintBackground)
                viewModel.paletteColors.value?.foreground?.let {
                    // todo this still does not fix the colors bug
//                Stuff.log("Color for ${track.name} is $it")
                    binding.recentsTitle.setTextColor(it)
                }

            setSelected(bindingAdapterPosition == viewModel.selectedPos, track)
        }
    }

    inner class VHHeader(private val binding: HeaderWithActionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun setItemData(headerData: ExpandableHeader) {
            if (headerData.section.itemType in arrayOf(
                    Section.PENDING_SCROBBLES.ordinal,
                    Section.PENDING_LOVES.ordinal
                )
            ) {
                val listSize = headerData.section.listSize
                binding.headerText.text = headerData.title + ": " +
                        itemView.context.resources.getQuantityString(
                            R.plurals.num_pending,
                            listSize,
                            listSize
                        )
                binding.headerOverflowButton.isVisible = true
                binding.headerOverflowButton.setOnClickListener {
                    val popupMenu = PopupMenu(
                        binding.headerOverflowButton.context,
                        binding.headerOverflowButton
                    )
                    popupMenu.inflate(R.menu.delete_all_menu)
                    popupMenu.setOnMenuItemClickListener {
                        if (it.itemId == R.id.delete_all_confirm) {
                            viewModel.viewModelScope.launch(Dispatchers.IO) {
                                PanoDb.db.getPendingScrobblesDao().nuke()
                                PanoDb.db.getPendingLovesDao().nuke()
                            }
                            true
                        } else
                            false
                    }
                    popupMenu.showWithIcons()
                }
            } else {
                binding.headerText.text = headerData.title
                binding.headerOverflowButton.isVisible = false
            }

            if (headerData.section.listSize > 3) {
                binding.headerAction.visibility = View.VISIBLE
                binding.headerAction.text = headerData.actionText
            } else {
                binding.headerAction.visibility = View.GONE
            }

            binding.headerAction.setOnClickListener {
                (viewModel.virtualList[bindingAdapterPosition] as? ExpandableHeader)?.let {
                    val oldData = viewModel.virtualList.copy()
                    it.toggle()
                    autoNotify(oldData, viewModel.virtualList) { o, n ->
                        o is ExpandableHeader && n is ExpandableHeader
                                // prevent change animation, check for contents later
                                ||
                                o === n
                    }
                }
            }
        }
    }

    interface SetHeroTrigger {
        fun onSetHero(track: Track, cacheKey: MemoryCache.Key?)
    }
}


private enum class Section {
    PENDING_SCROBBLES,
    PENDING_LOVES,
    SCROBBLES,
}