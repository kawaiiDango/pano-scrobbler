package com.arn.scrobble

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.arn.scrobble.databinding.ContentFriendsBinding
import com.arn.scrobble.databinding.GridItemFriendBinding
import com.arn.scrobble.ui.EndlessRecyclerViewScrollListener
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.ui.LoadMoreGetter
import com.google.android.material.color.MaterialColors
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import de.umass.lastfm.ImageSize
import de.umass.lastfm.PaginatedResult
import de.umass.lastfm.Track
import de.umass.lastfm.User
import java.lang.ref.WeakReference


/**
 * Created by arn on 10/07/2017.
 */

class FriendsAdapter(private val fragmentBinding: ContentFriendsBinding, private val viewModel: FriendsVM) : RecyclerView.Adapter<FriendsAdapter.VHUser>(), LoadMoreGetter {

    lateinit var itemClickListener: ItemClickListener
    override lateinit var loadMoreListener: EndlessRecyclerViewScrollListener
    val handler by lazy { DelayHandler(WeakReference(this)) }

    init {
        setHasStableIds(true)
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHUser {
        val inflater = LayoutInflater.from(parent.context)
        return VHUser(GridItemFriendBinding.inflate(inflater, parent, false))
    }

    fun getViewBindingForPopup(context: Context, position: Int): GridItemFriendBinding {
        val inflater = LayoutInflater.from(context)
        val binding = GridItemFriendBinding.inflate(inflater, fragmentBinding.root, false)

        val holder = VHUser(binding, false)
        holder.setItemData(viewModel.friends[position])
        return binding
    }

    override fun onBindViewHolder(holder: VHUser, position: Int) {
        holder.setItemData(viewModel.friends[position])
    }

    // total number of cells
    override fun getItemCount() = viewModel.friends.size

    fun populate() {
        if (fragmentBinding.friendsSwipeRefresh.isRefreshing) {
            fragmentBinding.friendsGrid.scheduleLayoutAnimation()
            fragmentBinding.friendsSwipeRefresh.isRefreshing = false
        }
        loadMoreListener.loading = false
        val header = fragmentBinding.friendsHeader.headerText
        if (viewModel.friends.isEmpty()) {
            header.visibility = View.VISIBLE
            header.text = header.context.getString(R.string.no_friends)
        } else
            header.visibility = View.GONE

        notifyDataSetChanged()
    }

    fun populateFriendsRecent(res: PaginatedResult<Track>, username: String) {
        if (!res.isEmpty && viewModel.friends.isNotEmpty()) {
            for (pos in 0..viewModel.friends.size) {
                if (pos < viewModel.friends.size && viewModel.friends[pos].name == username){
                    val oldRecent = viewModel.friends[pos].recentTrack
                    val newRecent = res.pageResults.first()
                    if (oldRecent?.playedWhen != newRecent?.playedWhen || oldRecent?.name != newRecent?.name) {
                        viewModel.friends[pos].recentTrack = newRecent
                        viewModel.friends[pos].playcount = res.totalPages
                        notifyItemChanged(pos, 0)
                    }
                    break
                }
            }
        }
        if (!Main.isTV && !viewModel.sorted && loadMoreListener.isAllPagesLoaded && viewModel.friends.size > 1 &&
                !viewModel.friends.any { it.recentTrack == null }) {
            val sortButton = fragmentBinding.friendsSort
            sortButton.show()
            sortButton.setOnClickListener {
                viewModel.friends.sortByDescending {
                    if (it.playcount == 0) //put users with 0 plays at the end
                        0L
                    else
                        it.recentTrack?.playedWhen?.time ?: System.currentTimeMillis()
                }
                viewModel.sorted = true
                notifyDataSetChanged()
                sortButton.hide()
                fragmentBinding.friendsGrid.smoothScrollToPosition(0)
            }
        }
    }

    fun loadFriendsRecents(pos:Int) {
        val glm = fragmentBinding.friendsGrid.layoutManager as GridLayoutManager? ?: return
        if (pos < viewModel.friends.size && (pos + glm.spanCount) >= glm.findFirstVisibleItemPosition() &&
                (pos - glm.spanCount) <= glm.findLastVisibleItemPosition())
            viewModel.loadFriendsRecents(viewModel.friends[pos].name)
    }

    fun getItem(id: Int): User? {
        return if (id >= 0 && id < viewModel.friends.size)
            viewModel.friends[id]
        else
            null
    }

    override fun getItemId(position: Int): Long {
        return viewModel.friends[position].name.hashCode().toLong()
    }

    inner class VHUser(private val binding: GridItemFriendBinding, private val clickable: Boolean = true) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        init {
            if (clickable) {
                itemView.setOnClickListener(this)
                binding.friendsPic.setOnClickListener(this)
                binding.friendsPic.isFocusable = true
            }
        }

        override fun onClick(view: View) {
            if (clickable)
                itemClickListener.onItemClick(itemView, adapterPosition)
        }

        fun setItemData(user: User) {
            binding.friendsName.text = if (user.realname == null || user.realname == "")
                    user.name
            else
                user.realname

            val track = user.recentTrack
            if (track != null && track.name != null && track.name != "") {
                binding.friendsTrackLl.visibility = View.VISIBLE
                binding.friendsTitle.text = track.name
                binding.friendsSubtitle.text = track.artist
                binding.friendsDate.text = Stuff.myRelativeTime(itemView.context, track.playedWhen)

                if (track.isNowPlaying) {
                    if (binding.friendsMusicIcon.drawable == null ||
                            binding.friendsMusicIcon.drawable is VectorDrawable || binding.friendsMusicIcon.drawable is VectorDrawableCompat) {
                        Stuff.nowPlayingAnim(binding.friendsMusicIcon, true)
                    }
                } else {
                    if (binding.friendsMusicIcon.drawable == null ||
                            binding.friendsMusicIcon.drawable is AnimatedVectorDrawable || binding.friendsMusicIcon.drawable is AnimatedVectorDrawableCompat)
                        binding.friendsMusicIcon.setImageResource(R.drawable.vd_music_circle)
                }

                binding.friendsTrackFrame.setOnClickListener { v: View ->
                    Stuff.launchSearchIntent(track.artist, track.name, itemView.context)
                }
            } else {
                binding.friendsTrackLl.visibility = View.INVISIBLE
                binding.friendsTrackFrame.setOnClickListener(null)

                if (binding.friendsMusicIcon.drawable == null ||
                        binding.friendsMusicIcon.drawable is AnimatedVectorDrawable || binding.friendsMusicIcon.drawable is AnimatedVectorDrawableCompat)
                    binding.friendsMusicIcon.setImageResource(R.drawable.vd_music_circle)

                if (!handler.hasMessages(user.name.hashCode()) && adapterPosition > -1) {
                    val msg = handler.obtainMessage(user.name.hashCode())
                    msg.arg1 = adapterPosition
                    handler.sendMessageDelayed(msg, Stuff.FRIENDS_RECENTS_DELAY)
                }
            }

            val userImg = user.getWebpImageURL(ImageSize.EXTRALARGE)
            if (userImg != binding.friendsPic.tag) {
                binding.friendsPic.tag = userImg
                val bgDark = MaterialColors.getColor(binding.root, android.R.attr.colorBackground)
                val wasCached = viewModel.paletteColorsCache[userImg] != null
                if (wasCached)
                    itemView.setBackgroundColor(viewModel.paletteColorsCache[userImg]!!)
                else
                    itemView.setBackgroundColor(bgDark)

                if (userImg != null && userImg != "") {
                    Picasso.get()
                            .load(userImg)
                            .placeholder(R.drawable.vd_placeholder_user)
                            .error(R.drawable.vd_placeholder_user)
                            .into(binding.friendsPic, object : Callback {
                                override fun onSuccess() {
                                    val b = (binding.friendsPic.drawable as BitmapDrawable).bitmap
                                    if (!wasCached)
                                        Palette.from(b).generate { palette ->
                                            palette ?: return@generate

                                            val colorMutedBlack = palette.getDarkMutedColor(bgDark)
                                            val anim = ObjectAnimator.ofArgb(itemView, "backgroundColor", bgDark, colorMutedBlack)
                                            anim.duration = 350
                                            anim.interpolator = AccelerateInterpolator()
                                            anim.start()
                                            viewModel.paletteColorsCache[userImg] = colorMutedBlack
                                        }
                                }

                                override fun onError(e: Exception) {
                                }
                            })
                } else {
                    binding.friendsPic.setImageResource(R.drawable.vd_placeholder_user)
                }
            }
        }
    }

    class DelayHandler(private val friendsAdapterWr: WeakReference<FriendsAdapter>) : Handler(Looper.getMainLooper()) {
        override fun handleMessage(m: Message) {
            val pos = m.arg1
            friendsAdapterWr.get()?.loadFriendsRecents(pos)
        }
    }
}