package com.arn.scrobble

import android.app.Activity
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.os.Message
import android.support.v4.content.ContextCompat
import android.support.v7.graphics.Palette
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import de.umass.lastfm.ImageSize
import de.umass.lastfm.PaginatedResult
import de.umass.lastfm.Track
import de.umass.lastfm.User
import kotlinx.android.synthetic.main.content_friends.*
import kotlinx.android.synthetic.main.content_friends.view.*
import kotlinx.android.synthetic.main.grid_item_friend.view.*
import kotlinx.android.synthetic.main.header_default.view.*
import java.lang.ref.WeakReference

/**
 * Created by arn on 10/07/2017.
 */

class FriendsAdapter

(c: Context, private val layoutResourceId: Int) : ArrayAdapter<User>(c, layoutResourceId, mutableListOf()) {

    var totalPages: Int = 1
    lateinit var friendsRecentsLoader: LFMRequester
    private val handler = DelayHandler(WeakReference(this))
    private val recentsLoadQ = mutableListOf<Int>()

    init {
        setNotifyOnChange(false)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view: View? = convertView
        val activity = context as Activity
        if (view == null) {
            // inflate the layout
            val inflater = activity.layoutInflater
            view = inflater.inflate(layoutResourceId, parent, false)!!
        }
        // object item based on the position
        val user = getItem(position) ?: return view
//        parent as GridView

        view.friends_name.text = user.realname ?: user.name

        val track = user.recentTrack
        if (track != null && track.name != null && track.name != "") {
            view.friends_track.visibility = View.VISIBLE
            view.friends_title.text = track.name
            view.friends_subtitle.text = track.artist
            view.friends_date.text = Stuff.myRelativeTime(context, track.playedWhen)

            if (track.isNowPlaying)
                view.friends_music_icon.visibility = View.INVISIBLE
            else
                view.friends_music_icon.visibility = View.VISIBLE
            Stuff.nowPlayingAnim(view.friends_music_icon_playing, track.isNowPlaying)

            view.friends_track.setOnClickListener { v: View ->
                Stuff.openSearchURL(track.artist + " - " + track.name, v, context)
            }
        } else {
            view.friends_track.visibility = View.INVISIBLE
            if (!recentsLoadQ.contains(position))
                recentsLoadQ.add(position)
            val msg = handler.obtainMessage(Stuff.CANCELLABLE_MSG, Stuff.NEED_FRIENDS_RECENTS)
            handler.sendMessageDelayed(msg, Stuff.FRIENDS_RECENTS_DELAY)
        }

        val userImg = user.getImageURL(ImageSize.MEDIUM)
        if (userImg != view.friends_pic.tag) {
            view.friends_pic.tag = userImg
            if (userImg != null && userImg != "")
                Picasso.with(context)
                        .load(userImg)
                        .fit()
                        .centerInside()
//                        .noPlaceholder()
//                        .placeholder(R.drawable.ic_placeholder_user)
                        .error(R.drawable.ic_placeholder_user)
                        .into(view.friends_pic, object : Callback {
                            override fun onSuccess() {
                                view?.friends_pic?.clearColorFilter()
                                val b = (view?.friends_pic?.drawable as BitmapDrawable).bitmap
                                Palette.generateAsync(b) { palette ->
                                    val colorDomPrimary = palette.getDominantColor(ContextCompat.getColor(context, R.color.colorPrimary))
                                    val colorMutedBlack = palette.getDarkMutedColor(ContextCompat.getColor(context, android.R.color.background_dark))

                                    view?.setBackgroundColor(colorMutedBlack)
                                }
                            }

                            override fun onError() {
                                Stuff.log("Picasso onError")
                            }
                        })
            else
                view.friends_pic.setImageResource(R.drawable.ic_placeholder_user)
        }
        return view
    }

    fun populate(res: PaginatedResult<User>, page: Int = 1) {
        val layout = (context as Activity).friends_linear_layout ?: return
        val refresh = layout.friends_swipe_refresh ?: return
        if (layout.visibility != View.VISIBLE)
            return
        refresh.isRefreshing = false
        totalPages = res.totalPages

        val sortedRes = res.pageResults.sortedByDescending {
            if (it?.playcount == null || it.playcount == 0) //put users with 0 plays at the end
                0L
            else
                it.recentTrack?.playedWhen?.time ?: System.currentTimeMillis()
        }
        val grid = (context as Activity).friends_grid
        //get old now playing data to prevent flicker
        for (i in 0 until count)
            for (j in i until sortedRes.size) {
                if (getItem(i).name == sortedRes[j].name &&
                        sortedRes[j].recentTrack == null && getItem(i).recentTrack != null &&
                        (i >= grid.firstVisiblePosition && i <= grid.lastVisiblePosition)) {
                    if (!recentsLoadQ.contains(i))
                        recentsLoadQ.add(i)
                    loadNextRecents()
//                    handler.obtainMessage(Stuff.CANCELLABLE_MSG, Stuff.NEED_FRIENDS_RECENTS).sendToTarget()
                    sortedRes[j].recentTrack = getItem(i).recentTrack
                }
            }

        if (page == 1) {
            clear()
            val header = layout.header_text
            if (res.isEmpty) {
                header.visibility = View.VISIBLE
                header.text = context.getString(R.string.no_friends)
            }
        }
        addAll(sortedRes)
        notifyDataSetChanged()
    }

    fun populateFriendsRecent(res: PaginatedResult<Track>, pos: Int) {
        if (!res.isEmpty) {
            getItem(pos).recentTrack = res.pageResults.first()
            notifyDataSetChanged()
        }
        if (recentsLoadQ.isNotEmpty()) {
            recentsLoadQ.removeAt(0)
            loadNextRecents()
        }
    }

    fun loadNextRecents() {
        synchronized(recentsLoadQ) {
            if (!friendsRecentsLoader.isLoading && recentsLoadQ.isNotEmpty()) {
                val pos = recentsLoadQ.first()
                val grid = (context as Activity).friends_grid ?: return
                if (pos >= grid.firstVisiblePosition && pos <= grid.lastVisiblePosition) {
                    friendsRecentsLoader.args[0] = getItem(pos).name
                    friendsRecentsLoader.args[1] = pos.toString()
                    friendsRecentsLoader.forceLoad()
//                        LFMRequester(friendsAdapter.context, this).execute(Stuff.GET_FRIENDS_RECENTS,
//                                friendsAdapter.getItem(pos).name, pos.toString())
                } else
                    recentsLoadQ.removeAt(0)
            }
        }
    }

    class DelayHandler(private val friendsAdapterWr: WeakReference<FriendsAdapter>) : Handler() {
        override fun handleMessage(m: Message) {
            val command = m.obj as String
            when (command) {
                Stuff.NEED_FRIENDS_RECENTS ->
                    friendsAdapterWr.get()?.loadNextRecents()
            }
        }
    }
}