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
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.arn.scrobble.ui.EndlessRecyclerViewScrollListener
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.ui.LoadMoreGetter
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import de.umass.lastfm.ImageSize
import de.umass.lastfm.PaginatedResult
import de.umass.lastfm.Track
import de.umass.lastfm.User
import kotlinx.android.synthetic.main.content_friends.view.*
import kotlinx.android.synthetic.main.grid_item_friend.view.*
import kotlinx.android.synthetic.main.header_default.view.*
import java.lang.ref.WeakReference


/**
 * Created by arn on 10/07/2017.
 */

class FriendsAdapter(val fragmentContent: View, private val viewModel: FriendsVM) : RecyclerView.Adapter<FriendsAdapter.VHUser>(), LoadMoreGetter {

    lateinit var itemClickListener: ItemClickListener
    override lateinit var loadMoreListener: EndlessRecyclerViewScrollListener
    val handler by lazy { DelayHandler(WeakReference(this)) }

    init {
        setHasStableIds(true)
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHUser {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.grid_item_friend, parent, false)
        return VHUser(view)
    }

    fun getViewForPopup(context: Context, position: Int): View {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.grid_item_friend, fragmentContent as ViewGroup, false) as ViewGroup

        val holder = VHUser(view, false)
        holder.setItemData(viewModel.friends[position])
        return view
    }

    override fun onBindViewHolder(holder: VHUser, position: Int) {
        holder.setItemData(viewModel.friends[position])
    }

    // total number of cells
    override fun getItemCount() = viewModel.friends.size

    fun populate() {
        val refresh = fragmentContent.friends_swipe_refresh ?: return
        fragmentContent.friends_grid ?: return
        refresh.isRefreshing = false
        loadMoreListener.loading = false
        val header = fragmentContent.header_text
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
            val sortButton = fragmentContent.friends_sort
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
                fragmentContent.friends_grid.smoothScrollToPosition(0)
            }
        }
    }

    fun loadFriendsRecents(pos:Int) {
        val glm = fragmentContent.friends_grid.layoutManager as GridLayoutManager? ?: return
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

    inner class VHUser(view: View, private val clickable: Boolean = true) : RecyclerView.ViewHolder(view), View.OnClickListener {
        private val vName = view.friends_name
        private val vMusIcon = view.friends_music_icon
        private val vTrackFrame = view.friends_track_frame
        private val vTrackLL = view.friends_track_ll
        private val vDate = view.friends_date
        private val vTitle = view.friends_title
        private val vSubtitle = view.friends_subtitle
        private val vImg = view.friends_pic

        init {
            if (clickable) {
                itemView.setOnClickListener(this)
                vImg.setOnClickListener { onClick(itemView) }
            }
        }

        override fun onClick(view: View) {
            if (clickable)
                itemClickListener.onItemClick(itemView, adapterPosition)
        }

        fun setItemData(user: User) {
            vName.text = if (user.realname == null || user.realname == "")
                    user.name
            else
                user.realname

            val track = user.recentTrack
            if (track != null && track.name != null && track.name != "") {
                vTrackLL.visibility = View.VISIBLE
                vTitle.text = track.name
                vSubtitle.text = track.artist
                vDate.text = Stuff.myRelativeTime(itemView.context, track.playedWhen)

                if (track.isNowPlaying) {
                    if (vMusIcon.drawable == null ||
                            vMusIcon.drawable is VectorDrawable || vMusIcon.drawable is VectorDrawableCompat) {
                        Stuff.nowPlayingAnim(vMusIcon, true)
                    }
                } else {
                    if (vMusIcon.drawable == null ||
                            vMusIcon.drawable is AnimatedVectorDrawable || vMusIcon.drawable is AnimatedVectorDrawableCompat)
                        vMusIcon.setImageResource(R.drawable.vd_music_circle)
                }

                vTrackFrame.setOnClickListener { v: View ->
                    Stuff.launchSearchIntent(track.artist, track.name, itemView.context)
                }
            } else {
                vTrackLL.visibility = View.INVISIBLE
                vTrackFrame.setOnClickListener(null)

                if (vMusIcon.drawable == null ||
                        vMusIcon.drawable is AnimatedVectorDrawable || vMusIcon.drawable is AnimatedVectorDrawableCompat)
                    vMusIcon.setImageResource(R.drawable.vd_music_circle)

                if (!handler.hasMessages(user.name.hashCode()) && adapterPosition > -1) {
                    val msg = handler.obtainMessage(user.name.hashCode())
                    msg.arg1 = adapterPosition
                    handler.sendMessageDelayed(msg, Stuff.FRIENDS_RECENTS_DELAY)
                }
            }

            val userImg = user.getWebpImageURL(ImageSize.EXTRALARGE)
            if (userImg != vImg.tag) {
                vImg.tag = userImg
                val bgDark = ContextCompat.getColor(itemView.context, R.color.darkToolbar)
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
                            .into(vImg, object : Callback {
                                override fun onSuccess() {
                                    val b = (vImg?.drawable as BitmapDrawable).bitmap
                                    if (!wasCached)
                                        Palette.from(b).generate { palette ->
                                            palette ?: return@generate

                                            val colorMutedBlack = palette.getDarkMutedColor(bgDark)
                                            val anim = ObjectAnimator.ofArgb(itemView, "backgroundColor", bgDark, colorMutedBlack)
                                            anim.duration = 350
                                            anim.interpolator = AccelerateInterpolator()
                                            anim.start()
                                            viewModel.paletteColorsCache.put(userImg, colorMutedBlack)
                                        }
                                }

                                override fun onError(e: Exception) {
                                }
                            })
                } else {
                    vImg.setImageResource(R.drawable.vd_placeholder_user)
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