package com.arn.scrobble

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import android.os.Handler
import android.os.Message
import android.util.LruCache
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
import com.arn.scrobble.ui.ItemClickListener
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

class FriendsAdapter(private val fragmentContent: View) : RecyclerView.Adapter<FriendsAdapter.VHUser>() {

    private var users = mutableListOf<User>()
    private var itemClickListener: ItemClickListener? = null
    var totalPages: Int = 1
    val handler = DelayHandler(WeakReference(this))
    lateinit var viewModel: FriendsVM
    private val paletteColorsCache = LruCache<String, Int>(50)

    init {
        setHasStableIds(true)
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
        holder.setItemData(users[position])
        return view
    }

    override fun onBindViewHolder(holder: VHUser, position: Int) {
        holder.setItemData(users[position])
    }

    // total number of cells
    override fun getItemCount() = users.size

    fun populate(res: PaginatedResult<User>, page: Int = 1) {
        val refresh = fragmentContent.friends_swipe_refresh ?: return
        if (!fragmentContent.isShown || fragmentContent.friends_grid == null)
            return
        refresh.isRefreshing = false
        totalPages = res.totalPages

        val sortedRes = res.pageResults.sortedByDescending {
            if (it?.playcount == null || it.playcount == 0) //put users with 0 plays at the end
                0L
            else
                it.recentTrack?.playedWhen?.time ?: System.currentTimeMillis()
        }
        val glm = fragmentContent.friends_grid.layoutManager as GridLayoutManager? ?: return
        //get old now playing data to prevent flicker
        val firstVisible = glm.findFirstVisibleItemPosition()
        val lastVisible = glm.findLastVisibleItemPosition()
        for (i in 0 until itemCount)
            for (j in i until sortedRes.size) {
                if (users[i].name == sortedRes[j].name &&
                        sortedRes[j].recentTrack == null && users[i].recentTrack != null &&
                        (i in firstVisible..lastVisible)) {
                    sortedRes[j].recentTrack = users[i].recentTrack
                    sortedRes[j].playcount = users[i].playcount
                    if (!handler.hasMessages(users[i].name.hashCode())) {
                        val msg = handler.obtainMessage(users[i].name.hashCode())
                        msg.arg1 = j
                        handler.sendMessageDelayed(msg, Stuff.FRIENDS_RECENTS_DELAY)
                    }
                }
            }

        if (page == 1) {
            users.clear()
            val header = fragmentContent.header_text
            if (res.isEmpty) {
                header.visibility = View.VISIBLE
                header.text = header.context.getString(R.string.no_friends)
            }
        }
        users.addAll(sortedRes)
        notifyDataSetChanged()
    }

    fun populateFriendsRecent(res: PaginatedResult<Track>, username: String) {
        if (!res.isEmpty && users.isNotEmpty()) {
            for (pos in 0..users.size) {
                if (pos < users.size && users[pos].name == username){
                    val oldRecent = users[pos].recentTrack
                    val newRecent = res.pageResults.first()
                    if (oldRecent?.playedWhen != newRecent?.playedWhen || oldRecent?.name != newRecent?.name) {
                        users[pos].recentTrack = newRecent
                        users[pos].playcount = res.totalPages
                        notifyItemChanged(pos, 0)
                    }
                    break
                }
            }
        }
    }

    fun loadFriendsRecents(pos:Int) {
        val glm = fragmentContent.friends_grid.layoutManager as GridLayoutManager? ?: return
        if (pos < users.size && (pos + glm.spanCount) >= glm.findFirstVisibleItemPosition() &&
                (pos - glm.spanCount) <= glm.findLastVisibleItemPosition())
            viewModel.loadFriendsRecents(users[pos].name)
    }

    fun getItem(id: Int): User? {
        return if (id >= 0 && id < users.size)
            users[id]
        else
            null
    }

    override fun getItemId(position: Int): Long {
        return users[position].name.hashCode().toLong()
    }

    fun setClickListener(itemClickListener: ItemClickListener) {
        this.itemClickListener = itemClickListener
    }

    inner class VHUser(view: View, private val clickable: Boolean = true) : RecyclerView.ViewHolder(view), View.OnClickListener {
        private val vName = view.friends_name
        private val vMusIcon = view.friends_music_icon
        private val vTrackContainer = view.friends_track_container
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
                itemClickListener?.onItemClick(itemView, adapterPosition)
        }

        fun setItemData(user: User) {
            vName.text = if (user.realname == null || user.realname == "")
                    user.name
            else
                user.realname

            val track = user.recentTrack
            if (track != null && track.name != null && track.name != "") {
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

                vTrackContainer.setOnClickListener { v: View ->
                    Stuff.launchSearchIntent(track.artist, track.name, itemView.context)
                }
            } else {
                vTitle.text = "…"
                vSubtitle.text = " "
                vDate.text = " "
                vTrackContainer.setOnClickListener {}
                if (!handler.hasMessages(user.name.hashCode()) && adapterPosition > -1) {
                    val msg = handler.obtainMessage(user.name.hashCode())
                    msg.arg1 = adapterPosition
                    handler.sendMessageDelayed(msg, Stuff.FRIENDS_RECENTS_DELAY)
                }
            }

            val userImg = user.getImageURL(ImageSize.MEDIUM)
            if (userImg != vImg.tag) {
                vImg.tag = userImg
                val bgDark = ContextCompat.getColor(itemView.context, android.R.color.background_dark)
                val wasCached = paletteColorsCache[userImg] != null
                if (wasCached)
                    itemView.setBackgroundColor(paletteColorsCache[userImg])
                else
                    itemView.setBackgroundColor(bgDark)

                if (userImg != null && userImg != "") {
                    Picasso.get()
                            .load(userImg)
                            .fit()
                            .centerInside()
                            .placeholder(R.drawable.ic_placeholder_user)
                            .error(R.drawable.ic_placeholder_user)
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
                                            paletteColorsCache.put(userImg, colorMutedBlack)
                                        }
                                }

                                override fun onError(e: Exception) {
                                }
                            })
                } else {
                    vImg.setImageResource(R.drawable.ic_placeholder_user)
                }
            }
        }
    }

    class DelayHandler(private val friendsAdapterWr: WeakReference<FriendsAdapter>) : Handler() {
        override fun handleMessage(m: Message) {
            val pos = m.arg1
            friendsAdapterWr.get()?.loadFriendsRecents(pos)
        }
    }
}