package com.arn.scrobble

import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.Fade
import android.transition.Transition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.arn.scrobble.ui.EndlessRecyclerViewScrollListener
import com.arn.scrobble.ui.ItemClickListener
import de.umass.lastfm.User
import kotlinx.android.synthetic.main.action_friends.view.*
import kotlinx.android.synthetic.main.content_friends.*
import kotlinx.android.synthetic.main.content_friends.view.*
import kotlinx.android.synthetic.main.grid_item_friend.view.*
import kotlinx.android.synthetic.main.header_default.view.*
import java.lang.ref.WeakReference
import kotlin.math.min


/**
 * Created by arn on 09/07/2017.
 */

class FriendsFragment : Fragment(), ItemClickListener {

    private var adapter: FriendsAdapter? = null
    private var runnable = Runnable { loadFriends(1) }
    private var popupWr: WeakReference<PopupWindow>? = null
    private lateinit var viewModel: FriendsVM

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.content_friends, container, false)
        view.isNestedScrollingEnabled = false
        Stuff.setProgressCircleColor(view.friends_swipe_refresh)
        view.friends_swipe_refresh.setOnRefreshListener { loadFriends(1) }

        return view
    }

    private fun loadFriends(page: Int):Boolean {
        friends_grid ?: return false
        val adapter = adapter ?: return false

        if (Main.isOnline) {
            friends_linear_layout.header_text.visibility = View.GONE
        } else {
            friends_linear_layout.header_text.text = getString(R.string.offline)
            friends_linear_layout.header_text.visibility = View.VISIBLE
        }

        return if (page <= adapter.totalPages || adapter.totalPages == 0) {
            if ((page == 1 && (friends_grid.layoutManager as GridLayoutManager).findFirstVisibleItemPosition() < 15) || page > 1) {
                viewModel.loadFriendsList(page, true)
            } else {
                adapter.handler.postDelayed(runnable, Stuff.RECENTS_REFRESH_INTERVAL)
            }
            if (adapter.itemCount == 0 || page > 1)
                friends_linear_layout.friends_swipe_refresh.isRefreshing = true
            true
        } else
            false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (userVisibleHint) // userVisibleHint may get set before onCreateView
            postInit()
    }

    override fun onStop() {
        if (userVisibleHint)
            popupWr?.get()?.dismiss()
        adapter?.handler?.removeCallbacks(runnable)
        super.onStop()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        if (newConfig != null) {
            val glm = friends_grid?.layoutManager as GridLayoutManager?
            glm?.spanCount = getNumColumns(newConfig)
        }
    }

    override fun setUserVisibleHint(visible: Boolean) {
        super.setUserVisibleHint(visible)
        if (visible && isResumed && friends_grid?.adapter == null)
            postInit()
    }

    private fun postInit() {
        Stuff.setTitle(activity, 0)

        adapter = FriendsAdapter(view!!)

        viewModel = VMFactory.getVM(this, FriendsVM::class.java)
        val friendsListLd = viewModel.loadFriendsList(1, false)
        if (!friendsListLd.hasActiveObservers()) {
            friendsListLd.observe(viewLifecycleOwner, Observer {
                it ?: return@Observer
                adapter!!.populate(it, it.page)
            })
            viewModel.loadFriendsRecents(null)
                    .observe(viewLifecycleOwner, Observer {
                        it ?: return@Observer
                        adapter!!.populateFriendsRecent(it.second, it.first)
                    })
        }
        adapter!!.setClickListener(this)
        adapter!!.viewModel = viewModel

        val glm = GridLayoutManager(context!!, getNumColumns())
        friends_grid.layoutManager = glm
        (friends_grid.itemAnimator as SimpleItemAnimator?)?.supportsChangeAnimations = false
        friends_grid.adapter = adapter

        loadFriends(1)

        val loadMoreListener = object : EndlessRecyclerViewScrollListener(glm) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView) {
                loadFriends(page+1)
            }
        }
        friends_grid.addOnScrollListener(loadMoreListener)
    }

    override fun onItemClick (view: View, position: Int) {
        val gridItem = adapter!!.getViewForPopup(context!!, position) as ViewGroup
        val user = adapter!!.getItem(position)

        showPopupWindow(gridItem, view, user)
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
            content.addView(action, 2)

            action.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

            val point = Point()
            activity!!.windowManager.defaultDisplay.getSize(point)
            val screenW = point.x
            val screenH = point.y
            val w = min((0.8 * screenW).toInt(), Stuff.dp2px(400, context!!))
            val h = popup.height + Stuff.dp2px(120-64, context!!) + action.measuredHeight
            popup.update((screenW - w )/2, ((screenH - h )/1.2).toInt(), w,h)

            content.friends_pic.layoutParams.width = Stuff.dp2px(120, context!!)
            content.friends_pic.layoutParams.height = Stuff.dp2px(120, context!!)
            content.friends_name.text = getString(R.string.num_scrobbles, content.friends_name.text, user.playcount)

            action.friends_profile.setOnClickListener { v:View ->
                Stuff.openInBrowser(userLink, activity, v)
            }
            action.friends_loved.setOnClickListener { v:View ->
                Stuff.openInBrowser("$userLink/loved", activity, v)
            }
            action.friends_chart.setOnClickListener { v:View ->
                Stuff.openInBrowser("$userLink/listening-report/week", activity, v)
            }

            drawableTintCompat(action.friends_profile)
            drawableTintCompat(action.friends_loved)
            drawableTintCompat(action.friends_chart)
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

    private fun drawableTintCompat(view: TextView, color:Int = ContextCompat.getColor(activity!!, R.color.colorAccent)){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            view.compoundDrawables.forEach {
                it?.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
            }
        }
    }

    private fun getNumColumns(config:Configuration = resources.configuration): Int {
        return config.screenWidthDp / 160
    }
}