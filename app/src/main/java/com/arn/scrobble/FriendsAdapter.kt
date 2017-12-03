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
import java.io.UnsupportedEncodingException
import java.net.URLEncoder

/**
 * Created by arn on 10/07/2017.
 */

class FriendsAdapter

(c: Context, private val layoutResourceId: Int) : ArrayAdapter<User>(c, layoutResourceId, mutableListOf()) {

    val handler = ResponseHandler(this)
    private var totalPages:Int = 1

    init {
        setNotifyOnChange(false)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view : View? = convertView
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
        if(track != null && track.name != null && track.name != ""){
            view.friends_track.visibility = View.VISIBLE
            view.friends_title.text = track.name
            view.friends_subtitle.text = track.artist
            view.friends_date.text = Stuff.myRelativeTime(context, track.playedWhen)

            if (track.isNowPlaying)
                view.friends_music_icon.visibility = View.INVISIBLE
            else
                view.friends_music_icon.visibility = View.VISIBLE
            Stuff.nowPlayingAnim(view.friends_music_icon_playing, track.isNowPlaying)

            view.friends_track.setOnClickListener {
                var ytUrl = "https://www.youtube.com/results?search_query="
                try {
                    ytUrl += URLEncoder.encode(track.artist + " - " + track.name, "UTF-8")
                } catch (e: UnsupportedEncodingException) {
                    Stuff.toast(context, context.getString(R.string.failed_encode_url))
                }
                Stuff.openInBrowser(ytUrl, context)
            }
        } else {
            view.friends_track.visibility = View.INVISIBLE
            val msg = handler.obtainMessage(Stuff.CANCELLABLE_MSG, Pair(Stuff.NEED_FRIENDS_RECENTS, position))
            handler.sendMessageDelayed(msg, Stuff.FRIENDS_RECENTS_DELAY)
        }

        val userImg = user.getImageURL(ImageSize.MEDIUM)
        if (userImg != null && userImg != "")
            Picasso.with(context)
                    .load(userImg)
                    .fit()
                    .centerInside()
                    .placeholder(R.drawable.ic_placeholder_user)
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
        return view
    }

    fun loadFriends(page: Int):Boolean {
        if (page <= totalPages || totalPages == 0) {
            Stuff.log("loadFriends $page")
            val grid = (context as Activity).friends_grid ?: return false

            if ((page == 1 && grid.firstVisiblePosition < 15) || page > 1)
                LFMRequester(context, handler).execute(Stuff.GET_FRIENDS, page.toString())
            else {
                val msg = handler.obtainMessage(Stuff.CANCELLABLE_MSG, Pair(Stuff.RELOAD_LIST_DATA, ""))
                handler.sendMessageDelayed(msg, Stuff.RECENTS_REFRESH_INTERVAL)
            }
            if (count == 0 || page > 1)
                (context as Activity).friends_linear_layout.friends_swipe_refresh.isRefreshing = true
            return true
        } else
            return false
    }

    fun populate(res: PaginatedResult<User>, page: Int = 1) {
        val layout = (context as Activity).friends_linear_layout ?: return
        val refresh = layout.friends_swipe_refresh ?: return
        if (layout.visibility != View.VISIBLE)
            return
        refresh.isRefreshing = false
        totalPages = res.totalPages
        handler.removeMessages(Stuff.CANCELLABLE_MSG)

        val sortedRes = res.pageResults.sortedByDescending {
            if (it?.playcount == null || it.playcount == 0) //put users with 0 plays at the end
                0L
            else
                it.recentTrack?.playedWhen?.time ?: System.currentTimeMillis()
        }
        val grid = (context as Activity).friends_grid
        //get old now playing data to prevent flicker
        for (i in 0 until count)
            for (j in i until sortedRes.size){
                if (getItem(i).name == sortedRes[j].name &&
                        sortedRes[j].recentTrack == null && getItem(i).recentTrack != null &&
                        (i >= grid.firstVisiblePosition && i <= grid.lastVisiblePosition)) {
                    handler.obtainMessage(Stuff.CANCELLABLE_MSG, Pair(Stuff.NEED_FRIENDS_RECENTS, i)).sendToTarget()
                    sortedRes[j].recentTrack = getItem(i).recentTrack
                }
            }

        if (page == 1) {
            clear()
            val msg = handler.obtainMessage(Stuff.CANCELLABLE_MSG, Pair(Stuff.RELOAD_LIST_DATA, ""))
            handler.sendMessageDelayed(msg, Stuff.RECENTS_REFRESH_INTERVAL)
            val header = layout.header_text
            if (res.isEmpty){
                header.visibility = View.VISIBLE
                header.text = context.getString(R.string.no_friends)
            }
        }
        addAll(sortedRes)
        notifyDataSetChanged()
    }

    fun populateFriendsRecent(res: PaginatedResult<Track>, pos: Int) {
        if(!res.isEmpty) {
            getItem(pos).recentTrack = res.pageResults.first()
            notifyDataSetChanged()
        }
    }

    class ResponseHandler(private val friendsAdapter: FriendsAdapter): Handler(){
        override fun handleMessage(m: Message) {
            //usually:
            // obj = command, paginatedresult;
            val pair = m.obj as Pair<String, Any>
            val command = pair.first
            val data = pair.second
            when(command){
                Stuff.GET_FRIENDS -> {
                    friendsAdapter.populate(data as PaginatedResult<User>, data.page)
                }
                Stuff.NEED_FRIENDS_RECENTS -> {
                    val pos = data as Int
                    val grid = (friendsAdapter.context as Activity).friends_grid ?: return
                    if (pos >= grid.firstVisiblePosition && pos <= grid.lastVisiblePosition)
                        LFMRequester(friendsAdapter.context, this).execute(Stuff.GET_FRIENDS_RECENTS,
                                friendsAdapter.getItem(pos).name, pos.toString())
                }
                Stuff.GET_FRIENDS_RECENTS -> {
                    val pos = (data as Pair<String,PaginatedResult<Track>>).first.toInt()
                    val res = data.second
                    friendsAdapter.populateFriendsRecent(res, pos)
                }
                Stuff.IS_ONLINE -> {
                    val layout = (friendsAdapter.context as Activity).friends_linear_layout ?: return
                    if (data as Boolean)
                        layout.header_text.visibility = View.GONE
                    else {
                        layout.header_text.visibility = View.VISIBLE
                        layout.header_text.text = friendsAdapter.context.getString(R.string.offline)
                    }
                }
                Stuff.RELOAD_LIST_DATA -> friendsAdapter.loadFriends(1)
            }
        }
    }

}