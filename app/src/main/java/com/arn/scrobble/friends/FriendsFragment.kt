package com.arn.scrobble.friends

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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.arn.scrobble.*
import com.arn.scrobble.Stuff.dp
import com.arn.scrobble.databinding.ActionFriendsBinding
import com.arn.scrobble.databinding.ContentFriendsBinding
import com.arn.scrobble.databinding.GridItemFriendBinding
import com.arn.scrobble.ui.EndlessRecyclerViewScrollListener
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.ui.SimpleHeaderDecoration
import de.umass.lastfm.User
import java.lang.ref.WeakReference
import java.text.NumberFormat
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
    private var _binding: ContentFriendsBinding? = null
    private val binding
        get() = _binding!!


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ContentFriendsBinding.inflate(inflater, container, false)
        binding.root.isNestedScrollingEnabled = false
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun loadFriends(page: Int):Boolean {
        _binding ?: return false
        binding.friendsGrid.layoutManager ?: return false

        if (Main.isOnline) {
            binding.friendsHeader.headerText.visibility = View.GONE
        } else {
            binding.friendsHeader.headerText.text = getString(R.string.offline)
            binding.friendsHeader.headerText.visibility = View.VISIBLE
        }

        return if (page <= viewModel.totalPages || viewModel.totalPages == 0) {
            if ((page == 1 && (binding.friendsGrid.layoutManager as GridLayoutManager).findFirstVisibleItemPosition() < 15) || page > 1) {
                viewModel.loadFriendsList(page)
            }
            if (adapter.itemCount == 0)
                binding.friendsSwipeRefresh.isRefreshing = true
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
        val glm = binding.friendsGrid.layoutManager as GridLayoutManager?
        glm?.spanCount = getNumColumns()
    }

    override fun onResume() {
        super.onResume()
        if (binding.friendsGrid.adapter == null)
            postInit()
        else if (System.currentTimeMillis() - lastRefreshTime >= Stuff.RECENTS_REFRESH_INTERVAL &&
                (viewModel.page == 1 || viewModel.sorted))
            runnable.run()
        Stuff.setTitle(activity, 0)
    }

    override fun onPause() {
        adapter.handler.removeCallbacks(runnable)
        super.onPause()
    }

    private fun postInit() {
        Stuff.setTitle(activity, 0)

        Stuff.setProgressCircleColor(binding.friendsSwipeRefresh)
        binding.friendsSwipeRefresh.setOnRefreshListener {
            viewModel.sorted = false
            binding.friendsSort.hide()
            loadFriends(1)
        }

        val glm = GridLayoutManager(context!!, getNumColumns())
        binding.friendsGrid.layoutManager = glm
        (binding.friendsGrid.itemAnimator as SimpleItemAnimator?)?.supportsChangeAnimations = false
        viewModel.username = username
        adapter = FriendsAdapter(binding, viewModel)
        binding.friendsGrid.adapter = adapter
        binding.friendsGrid.addItemDecoration(SimpleHeaderDecoration(0, 25.dp))

        var itemDecor = DividerItemDecoration(context!!, DividerItemDecoration.HORIZONTAL)
        itemDecor.setDrawable(ContextCompat.getDrawable(context!!, R.drawable.shape_divider_chart)!!)
        binding.friendsGrid.addItemDecoration(itemDecor)

        itemDecor = DividerItemDecoration(context!!, DividerItemDecoration.VERTICAL)
        itemDecor.setDrawable(ContextCompat.getDrawable(context!!, R.drawable.shape_divider_chart)!!)
        binding.friendsGrid.addItemDecoration(itemDecor)


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

        binding.friendsGrid.addOnScrollListener(loadMoreListener)
    }

    private fun refreshFriendsRecents() {
        val glm = binding.friendsGrid.layoutManager as GridLayoutManager
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
            val gridItem = adapter.getViewBindingForPopup(context!!, position)
            showPopupWindow(gridItem, view, user)
        }
    }

    private fun showPopupWindow(contentBinding: GridItemFriendBinding, anchor: View, user: User) {
        val userLink = user.url ?: return
        val popup = PopupWindow(contentBinding.root, anchor.measuredWidth, anchor.measuredHeight, true)
        popupWr = WeakReference(popup)
        popup.elevation = 10.dp.toFloat()
        popup.isTouchable = true
        popup.isOutsideTouchable = true
        popup.setBackgroundDrawable(ColorDrawable())

        fun postTransition() {
            TransitionManager.beginDelayedTransition(contentBinding.root, ChangeBounds().setDuration(150))
            val actionsBinding = ActionFriendsBinding.inflate(layoutInflater, contentBinding.root, false)
            contentBinding.root.addView(actionsBinding.root, 4)

            contentBinding.friendsPic.layoutParams.width = 120.dp
            contentBinding.friendsPic.layoutParams.height = 120.dp
            if (user.playcount > 0) {
                val since = if (user.registeredDate == null || user.registeredDate.time == 0L)
                    ""
                else
                    DateFormat.getMediumDateFormat(context).format(user.registeredDate.time)
                contentBinding.friendsScrobblesSince.text = getString(
                    R.string.num_scrobbles_since,
                        NumberFormat.getInstance().format(user.playcount),
                        since)
                contentBinding.friendsScrobblesSince.visibility = View.VISIBLE
            }
            if (user.country != null && user.country != "None") {
                contentBinding.friendsCountry.text = getString(R.string.from, user.country)
                contentBinding.friendsCountry.visibility = View.VISIBLE
            }

            actionsBinding.friendsProfile.setOnClickListener { v:View ->
                Stuff.openInBrowser(userLink, activity, v)
            }
            actionsBinding.friendsScrobbles.setOnClickListener { v:View ->
                (activity as Main).enableGestures()
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
            actionsBinding.friendsCharts.setOnClickListener { v:View ->
                (activity as Main).enableGestures()
                val f = HomePagerFragment()
                val b = Bundle()
                b.putString(Stuff.ARG_USERNAME, user.name)
                b.putLong(Stuff.ARG_REGISTERED_TIME, user.registeredDate.time)
                b.putInt(Stuff.ARG_TYPE, 3)
                f.arguments = b
                activity!!.supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.frame, f, Stuff.TAG_HOME_PAGER)
                        .addToBackStack(null)
                        .commit()
            }
            actionsBinding.root.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            contentBinding.root.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

            val point = Point()
            activity!!.windowManager.defaultDisplay.getSize(point)
            val screenW = point.x
            val screenH = point.y
            val w = min((0.8 * screenW).toInt(), 400.dp)
            val h = contentBinding.root.measuredHeight
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
            contentBinding.root.postDelayed( { postTransition() }, 10)

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
        return (resources.displayMetrics.widthPixels - (activity as Main).coordinatorPadding) /
                resources.getDimension(R.dimen.grid_size).roundToInt()
    }
}