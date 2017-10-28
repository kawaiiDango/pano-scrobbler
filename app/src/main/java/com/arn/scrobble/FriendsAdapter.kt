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
import android.widget.GridView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import de.umass.lastfm.ImageSize
import de.umass.lastfm.PaginatedResult
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

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view : View? = convertView
        val activity = context as Activity
        if (view == null) {
            // inflate the layout
            val inflater = activity.layoutInflater
            view = inflater.inflate(layoutResourceId, parent, false)!!
        }
        // object item based on the position
        val user = getItem(position) ?: return view
        parent as GridView

        view.friends_name.text = user.realname ?: user.name

        val track = user.recentTrack
        if(track != null && track.name != null && track.name != ""){
            view.friends_track.visibility = View.VISIBLE
            view.friends_title.text = track.name
            view.friends_subtitle.text = track.artist
            view.friends_date.text = Stuff.myRelativeTime(track.playedWhen)

            view.friends_track.setOnClickListener {
                var ytUrl = "https://www.youtube.com/results?search_query="
                try {
                    ytUrl += URLEncoder.encode(track.artist + " - " + track.name, "UTF-8")
                } catch (e: UnsupportedEncodingException) {
                    Stuff.toast(context, "failed to encode url")
                }
                Stuff.openInBrowser(ytUrl, context)
            }
        } else {
            view.friends_track.visibility = View.INVISIBLE
            Stuff.log(user.name +" doesnt have a track")
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
/*
    override fun getItemId(position: Int): Long {
        return getItem(position)?.playedWhen?.time ?: NP_ID
    }

    override fun hasStableIds(): Boolean {
        return true
    }
*/
    fun loadFriends(page: Int):Boolean {
        if (page <= totalPages) {
            Stuff.log("loadFriends $page")
            LFMRequester(context, handler).execute(Stuff.GET_FRIENDS, page.toString())
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
        handler.removeMessages(Stuff.RECENTS_REFRESH_INTERVAL.toInt())
        if (page == 1) {
            clear()
            val msg = handler.obtainMessage(Stuff.RECENTS_REFRESH_INTERVAL.toInt(), Pair(Stuff.REFRESH_RECENTS, ""))
            handler.sendMessageDelayed(msg, Stuff.RECENTS_REFRESH_INTERVAL)
            val header = layout.header_text
            if (res.isEmpty){
                header.visibility = View.VISIBLE
                header.text = context.getString(R.string.no_friends)
            } else
                header.visibility = View.GONE
        }
        addAll(res.pageResults)

        notifyDataSetChanged()
    }

    class ResponseHandler(private val recentsAdapter: FriendsAdapter): Handler(){
        override fun handleMessage(m: Message) {
            //usually:
            // obj = command, paginatedresult;
            val pair = m.obj as Pair<String, Any>
            val command = pair.first
            val data = pair.second
            when(command){
                Stuff.GET_FRIENDS -> {
                    recentsAdapter.populate(data as PaginatedResult<User>, data.page)
                }
//                Stuff.IS_ONLINE -> {
//                    val list = (recentsAdapter.context as Activity).friends_grid ?: return
//                    if (data as Boolean)
//                        list.header_text.text = recentsAdapter.context.getString(R.string.recently_scrobbled)
//                    else
//                        list.header_text.text = recentsAdapter.context.getString(R.string.offline)
//                }
//                Stuff.REFRESH_RECENTS -> recentsAdapter.loadRecents(1)
            }
        }
    }

}