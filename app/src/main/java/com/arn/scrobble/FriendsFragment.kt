package com.arn.scrobble

import android.app.AlertDialog
import android.app.Fragment
import android.app.LoaderManager
import android.content.Loader
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.TextView
import com.arn.scrobble.ui.EndlessScrollListener
import de.umass.lastfm.PaginatedResult
import de.umass.lastfm.Track
import de.umass.lastfm.User
import kotlinx.android.synthetic.main.content_friends.*
import kotlinx.android.synthetic.main.content_friends.view.*
import kotlinx.android.synthetic.main.grid_item_friend.view.*
import kotlinx.android.synthetic.main.header_default.view.*
import kotlin.math.max
import kotlin.math.min

/**
 * Created by arn on 09/07/2017.
 */

class FriendsFragment : Fragment(), LoaderManager.LoaderCallbacks<Any?> {

    private lateinit var adapter: FriendsAdapter
    private var runnable = Stuff.TimedRefresh(this, Stuff.GET_FRIENDS.hashCode())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.content_friends, container, false)

        adapter = FriendsAdapter(activity, R.layout.grid_item_friend)
        loaderManager.initLoader(Stuff.GET_FRIENDS.hashCode(), arrayOf("1").toArgsBundle(), this).startLoading()
        adapter.friendsRecentsLoader = loaderManager.initLoader(Stuff.GET_FRIENDS_RECENTS.hashCode(), arrayOf("", "").toArgsBundle(), this) as LFMRequester
        adapter.friendsRecentsLoader
        view.friends_grid.adapter = adapter
        view.friends_grid.setOnScrollListener(loadMoreListener)
        view.friends_grid.onItemClickListener = profileClickListener
        Stuff.setProgressCircleColor(view.friends_swipe_refresh)
        view.friends_swipe_refresh.setOnRefreshListener { loadFriends(1) }

        return view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }
    override fun onCreateLoader(id: Int, b: Bundle?): Loader<Any?>? {
        b ?: return null
        val args = b.getStringArray("args")
        return when(id){
            Stuff.GET_FRIENDS.hashCode() -> LFMRequester(activity, Stuff.GET_FRIENDS, *args)
            // user.getFriendsListeningNow never worked properly
            Stuff.GET_FRIENDS_RECENTS.hashCode() -> LFMRequester(activity, Stuff.GET_FRIENDS_RECENTS, *args)
//            Stuff.NEED_FRIENDS_RECENTS.hashCode() -> LFMRequester(activity, Stuff.NEED_FRIENDS_RECENTS, *args)
            else -> null
        }
    }

    private fun loadFriends(page: Int):Boolean {
        friends_grid ?: return false
        if (page <= adapter.totalPages || adapter.totalPages == 0) {
            if ((page == 1 && friends_grid.firstVisiblePosition < 15) || page > 1) {
                val getFriends = loaderManager.getLoader<Any>(Stuff.GET_FRIENDS.hashCode()) as LFMRequester
                getFriends.args[0] = page.toString()
                getFriends.forceLoad()
//                loaderManager.initLoader(Stuff.GET_FRIENDS.hashCode(), arrayOf(page.toString()).toArgsBundle(), this)
            } else {
                friends_grid?.postDelayed(runnable, Stuff.RECENTS_REFRESH_INTERVAL)
            }
            if (adapter.count == 0 || page > 1)
                friends_linear_layout.friends_swipe_refresh.isRefreshing = true
            return true
        } else
            return false
    }

    override fun onLoadFinished(loader: Loader<Any?>, data: Any?) {
        data ?: return
        if (Main.isOnline) {
            friends_linear_layout.header_text.visibility = View.GONE
        } else {
            friends_linear_layout.header_text.text = getString(R.string.offline)
            friends_linear_layout.header_text.visibility = View.VISIBLE
        }
        when(loader.id) {
            Stuff.GET_FRIENDS_RECENTS.hashCode() -> {
                val res = data as PaginatedResult<Track>
                loader as LFMRequester
                adapter.populateFriendsRecent(res, loader.args[1].toInt())
            }
            Stuff.GET_FRIENDS.hashCode() -> {
                data as PaginatedResult<User>
                adapter.populate(data, data.page)
                if (data.page == 1)
                    friends_grid?.postDelayed(runnable, Stuff.RECENTS_REFRESH_INTERVAL)
            }
        }
    }

    override fun onLoaderReset(loader: Loader<Any?>) {
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        Stuff.setTitle(activity, R.string.friends)
        loadFriends(1)
    }


    private val profileClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
        val gridItem = adapter.getView(position, null, null) // force inflate
        gridItem.friends_links.visibility = View.VISIBLE
        gridItem.friends_pic.layoutParams.width = Stuff.dp2px(100, activity)
        gridItem.friends_pic.layoutParams.height = Stuff.dp2px(100, activity)
        val userLink = (friends_grid.getItemAtPosition(position) as User?)?.url ?: return@OnItemClickListener
        gridItem.friends_profile.setOnClickListener { v:View ->
            Stuff.openInBrowser(userLink, activity, v)
        }
        gridItem.friends_profile.compoundDrawables
        gridItem.friends_loved.setOnClickListener { v:View ->
            Stuff.openInBrowser(userLink + "/loved", activity, v)
        }
        gridItem.friends_chart.setOnClickListener { v:View ->
            Stuff.openInBrowser(userLink + "/listening-report/week", activity, v)
        }
        drawableTintCompat(gridItem.friends_profile)
        drawableTintCompat(gridItem.friends_loved)
        drawableTintCompat(gridItem.friends_chart)
        gridItem.background = view.background


        val dialog = AlertDialog.Builder(activity)
                .setView(gridItem)
                .create()

        dialog.window.attributes.windowAnimations = R.style.TransFadeScale
        dialog.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(true)
        /*
        view.requestLayout()
        parent.requestLayout()

        val vv= dialog.window.reenterTransition

        dialog.window.attributes.gravity = Gravity.TOP or Gravity.LEFT
        dialog.window.attributes.x = 440
        dialog.window.attributes.y = 750
        val anim = dialog.window.attributes.layoutAnimationParameters
        Stuff.log("anim: $anim")
*/
        val centerPos = intArrayOf(0,0)
        view.getLocationOnScreen(centerPos)
        centerPos[0] += view.width/2
        centerPos[1] += view.height/2

        dialog.setOnShowListener {
            revealShow(gridItem, centerPos[0], centerPos[1], true, dialog)
        }
//        dialog.setOnCancelListener { revealShow(gridItem, centerPos[0], centerPos[1], false, dialog) }

        dialog.show()

    }

    private fun revealShow(view: View, xAbs:Int, yAbs:Int, reveal: Boolean, dialog: AlertDialog) {
        val w = view.width
        val h = view.height
        val dPos = intArrayOf(0,0)

        view.getLocationOnScreen(dPos)

        val x = max(dPos[0], min(dPos[0]+w, xAbs)) - dPos[0]
        val y = max(dPos[1], min(dPos[1]+h, yAbs)) - dPos[1]
        val maxRadius = Math.sqrt((w * w / 4 + h * h / 4).toDouble()).toFloat()

        if (reveal) {
            val revealAnimator = ViewAnimationUtils.createCircularReveal(view,
                    x, y, 0f, maxRadius)

            view.visibility = View.VISIBLE
            revealAnimator.start()

        } else {
/*
            val anim = ViewAnimationUtils.createCircularReveal(view, x, y, maxRadius, 0f)

            anim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    dialog.dismiss()
                    view.visibility = View.INVISIBLE

                }
            })

            anim.start()
            */
        }

    }
    @Suppress("DEPRECATION")
    private fun drawableTintCompat(view: TextView, color:Int = resources.getColor(R.color.colorAccent)){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            view.compoundDrawables?.forEach {
                it?.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
            }
        }
    }

    private val loadMoreListener = object : EndlessScrollListener() {
        override fun onLoadMore(page: Int, totalItemsCount: Int): Boolean {
            return loadFriends(page)
            // true ONLY if more data is actually being loaded; false otherwise.
        }
    }
}