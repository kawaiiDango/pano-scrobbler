package com.arn.scrobble

import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.transition.ChangeBounds
import android.transition.Fade
import android.transition.Transition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.PopupWindow
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.arn.scrobble.charts.ChartsPagerFragment
import com.arn.scrobble.ui.EndlessRecyclerViewScrollListener
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.ui.SimpleHeaderDecoration
import de.umass.lastfm.User
import kotlinx.android.synthetic.main.action_friends.view.*
import kotlinx.android.synthetic.main.content_friends.*
import kotlinx.android.synthetic.main.content_friends.view.*
import kotlinx.android.synthetic.main.coordinator_main.*
import kotlinx.android.synthetic.main.grid_item_friend.view.*
import kotlinx.android.synthetic.main.header_default.view.*
import java.lang.ref.WeakReference
import kotlin.math.min
import kotlin.math.roundToInt


/**
 * Created by arn on 09/07/2017.
 */

class FriendsFragment : Fragment(), ItemClickListener {

    private lateinit var adapter: FriendsAdapter
    private var runnable = Runnable {
        if (viewModel.sorted)
            refreshFriendsRecents()
        else
            loadFriends(1)
        lastRefreshTime = System.currentTimeMillis()
    }
    private var popupWr: WeakReference<PopupWindow>? = null
    private val username: String?
        get() = parentFragment?.arguments?.getString(Stuff.ARG_USERNAME)
    private val viewModel by lazy { VMFactory.getVM(this, FriendsVM::class.java) }
    var lastRefreshTime = System.currentTimeMillis()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.content_friends, container, false)
        view.isNestedScrollingEnabled = false
        return view
    }

    private fun loadFriends(page: Int):Boolean {
        friends_linear_layout ?: return false
        friends_grid.layoutManager ?: return false

        if (Main.isOnline) {
            friends_linear_layout.header_text.visibility = View.GONE
        } else {
            friends_linear_layout.header_text.text = getString(R.string.offline)
            friends_linear_layout.header_text.visibility = View.VISIBLE
        }

        return if (page <= viewModel.totalPages || viewModel.totalPages == 0) {
            if ((page == 1 && (friends_grid.layoutManager as GridLayoutManager).findFirstVisibleItemPosition() < 15) || page > 1) {
                viewModel.loadFriendsList(page)
            }
            if (adapter.itemCount == 0)
                friends_linear_layout.friends_swipe_refresh.isRefreshing = true
            true
        } else {
            adapter.loadMoreListener.isAllPagesLoaded = true
            false
        }
    }

    override fun onStop() {
        if (isVisible)
            popupWr?.get()?.dismiss()
        super.onStop()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val glm = friends_grid?.layoutManager as GridLayoutManager?
        glm?.spanCount = getNumColumns()
    }

    override fun onResume() {
        super.onResume()
        if (friends_grid?.adapter == null)
            postInit()
        else if (System.currentTimeMillis() - lastRefreshTime >= Stuff.RECENTS_REFRESH_INTERVAL &&
                (viewModel.page == 1 || viewModel.sorted))
            runnable.run()
    }

    override fun onPause() {
        adapter.handler.removeCallbacks(runnable)
        super.onPause()
    }

    private fun postInit() {
        Stuff.setTitle(activity, 0)

        Stuff.setProgressCircleColor(friends_swipe_refresh)
        friends_swipe_refresh.setOnRefreshListener {
            viewModel.sorted = false
            friends_sort.hide()
            loadFriends(1)
        }

        val glm = GridLayoutManager(context!!, getNumColumns())
        friends_grid.layoutManager = glm
        (friends_grid.itemAnimator as SimpleItemAnimator?)?.supportsChangeAnimations = false
        viewModel.username = username
        adapter = FriendsAdapter(view!!, viewModel)
        friends_grid.adapter = adapter
        friends_grid.addItemDecoration(SimpleHeaderDecoration(0, Stuff.dp2px(25, context!!)))

        val loadMoreListener = object : EndlessRecyclerViewScrollListener(glm) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView) {
                loadFriends(page)
            }
        }

        loadMoreListener.currentPage = viewModel.page
        adapter.loadMoreListener = loadMoreListener

        if (!viewModel.receiver.hasActiveObservers()) {
            viewModel.receiver.observe(viewLifecycleOwner, {
                it ?: return@observe
                viewModel.totalPages = it.totalPages
                /*
                val sortedRes = res.pageResults.sortedByDescending {
                    if (it?.playcount == null || it.playcount == 0) //put users with 0 plays at the end
                        0L
                    else
                        it.recentTrack?.playedWhen?.time ?: System.currentTimeMillis()
                }
                */
                val newFriendsMap = mutableMapOf<String, User>()
                it.pageResults.forEach {
                    if (it.name != null)
                        newFriendsMap[it.name] = it
                }
                val glm = friends_grid.layoutManager as GridLayoutManager
                //get old now playing data to prevent flicker
                val firstVisible = glm.findFirstVisibleItemPosition()
                val lastVisible = glm.findLastVisibleItemPosition()
                for (i in 0 until adapter.itemCount) {
                    val friend = newFriendsMap[viewModel.friends[i].name]
                    if (friend != null &&
                            friend.recentTrack == null && viewModel.friends[i].recentTrack != null &&
                            (i in firstVisible..lastVisible)) {
                        friend.recentTrack = viewModel.friends[i].recentTrack
                        friend.playcount = viewModel.friends[i].playcount
                    }
                }
                refreshFriendsRecents()
                if (it.page == 1)
                    viewModel.friends.clear()
                viewModel.friends.addAll(it.pageResults)
                adapter.populate()
                viewModel.receiver.value = null
                loadMoreListener.currentPage = it.page
                if (it.page == 1) {
                    adapter.handler.removeCallbacks(runnable)
                    adapter.handler.postDelayed(runnable, Stuff.RECENTS_REFRESH_INTERVAL * 2)
                }
            })
            viewModel.track.observe(viewLifecycleOwner, {
                        it ?: return@observe
                        adapter.populateFriendsRecent(it.second, it.first)
                    })
        }
        adapter.itemClickListener = this

        if (viewModel.friends.isEmpty())
            loadFriends(1)
        else
            adapter.populate()

        friends_grid.addOnScrollListener(loadMoreListener)
    }

    private fun refreshFriendsRecents() {
        val glm = friends_grid.layoutManager as GridLayoutManager
        val firstVisible = glm.findFirstVisibleItemPosition()
        val lastVisible = glm.findLastVisibleItemPosition()
        for (i in firstVisible..lastVisible) {
            if (i < 0)
                break
            if (!adapter.handler.hasMessages(viewModel.friends[i].name.hashCode())) {
                val msg = adapter.handler.obtainMessage(viewModel.friends[i].name.hashCode())
                msg.arg1 = i
                adapter.handler.sendMessageDelayed(msg, Stuff.FRIENDS_RECENTS_DELAY)
            }
        }
        adapter.handler.removeCallbacks(runnable)
        adapter.handler.postDelayed(runnable, Stuff.RECENTS_REFRESH_INTERVAL * 2)
    }

    override fun onItemClick (view: View, position: Int) {
        val user = adapter.getItem(position)
        if (user != null) {
            val gridItem = adapter.getViewForPopup(context!!, position) as ViewGroup
            showPopupWindow(gridItem, view, user)
        }
    }

    private fun showPopupWindow(content: ViewGroup, anchor: View, user: User) {
        val userLink = user.url ?: return
        val popup = PopupWindow(content, anchor.measuredWidth, anchor.measuredHeight, true)
        popupWr = WeakReference(popup)
        popup.elevation = Stuff.dp2px(10, context!!).toFloat()
        popup.isTouchable = true
        popup.isOutsideTouchable = true
        popup.setBackgroundDrawable(ColorDrawable())

        fun postTransition() {
            TransitionManager.beginDelayedTransition(content, ChangeBounds().setDuration(150))
            val action = layoutInflater.inflate(R.layout.action_friends, content, false)
            content.addView(action, 4)

            content.friends_pic.layoutParams.width = Stuff.dp2px(120, context!!)
            content.friends_pic.layoutParams.height = Stuff.dp2px(120, context!!)
            if (user.playcount > 0) {
                val since = if (user.registeredDate == null || user.registeredDate.time == 0L)
                    ""
                else
                    DateFormat.getMediumDateFormat(context).format(user.registeredDate.time)
                content.friends_scrobbles_since.text = getString(R.string.num_scrobbles_since, user.playcount, since)
                content.friends_scrobbles_since.visibility = View.VISIBLE
            }
            if (user.country != null && user.country != "None") {
                content.friends_country.text = getString(R.string.from, user.country)
                content.friends_country.visibility = View.VISIBLE
            }

            action.friends_profile.setOnClickListener { v:View ->
                Stuff.openInBrowser(userLink, activity, v)
            }
            action.friends_scrobbles.setOnClickListener { v:View ->
                val f = HomePagerFragment()
                val b = Bundle()
                b.putString(Stuff.ARG_USERNAME, user.name)
                b.putLong(Stuff.ARG_REGISTERED_TIME, user.registeredDate.time)
                f.arguments = b
                activity!!.supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.frame, f, Stuff.TAG_HOME_PAGER)
                        .addToBackStack(null)
                        .commit()
            }
            action.friends_charts.setOnClickListener { v:View ->
                val f = ChartsPagerFragment()
                val b = Bundle()
                b.putString(Stuff.ARG_USERNAME, user.name)
                f.arguments = b
                activity!!.supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.frame, f, Stuff.TAG_CHART_PAGER)
                        .addToBackStack(null)
                        .commit()
            }
//            action.friends_week.setOnClickListener { v:View ->
//                Stuff.openInBrowser("$userLink/listening-report/week", activity, v)
//            }
            action.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            content.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

            val point = Point()
            activity!!.windowManager.defaultDisplay.getSize(point)
            val screenW = point.x
            val screenH = point.y
            val w = min((0.8 * screenW).toInt(), Stuff.dp2px(400, context!!))
            val h = content.measuredHeight + action.measuredHeight
            popup.update((screenW - w )/2, ((screenH - h )/1.2).toInt(), w,h)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val exitTrans = Fade()
            exitTrans.duration = 100
            popup.exitTransition = exitTrans

            val enterTrans = Fade()
            enterTrans.duration = 50
            enterTrans.addListener(object : Transition.TransitionListener{
                override fun onTransitionEnd(p0: Transition?) {
                    postTransition()
                }

                override fun onTransitionResume(p0: Transition?){}

                override fun onTransitionPause(p0: Transition?) {}

                override fun onTransitionCancel(p0: Transition?) {}

                override fun onTransitionStart(p0: Transition?) {}

            })
            popup.enterTransition = enterTrans
        } else
            content.postDelayed( { postTransition() }, 10)

        val coords = IntArray(2)
        anchor.getLocationInWindow(coords)
        popup.showAtLocation(anchor, 0, coords[0], coords[1])

        val rootView = popup.contentView.rootView as ViewGroup
        val wm = rootView.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val lp = rootView.layoutParams as WindowManager.LayoutParams
        lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
        lp.dimAmount = 0.4f
        wm.updateViewLayout(rootView, lp)
    }

    private fun getNumColumns(): Int {
        return (resources.displayMetrics.widthPixels - activity!!.coordinator!!.paddingStart) /
                resources.getDimension(R.dimen.grid_size).roundToInt()
    }
}