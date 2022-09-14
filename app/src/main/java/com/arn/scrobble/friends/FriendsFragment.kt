package com.arn.scrobble.friends

import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.transition.Fade
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.arn.scrobble.*
import com.arn.scrobble.Stuff.putSingle
import com.arn.scrobble.billing.BillingFragment
import com.arn.scrobble.databinding.ActionFriendsBinding
import com.arn.scrobble.databinding.ContentFriendsBinding
import com.arn.scrobble.databinding.GridItemFriendBinding
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.ui.EndlessRecyclerViewScrollListener
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.ui.SimpleHeaderDecoration
import com.arn.scrobble.ui.UiUtils.dp
import com.arn.scrobble.ui.UiUtils.isTv
import com.arn.scrobble.ui.UiUtils.openInBrowser
import com.arn.scrobble.ui.UiUtils.setProgressCircleColors
import com.arn.scrobble.ui.UiUtils.setTitle
import com.arn.scrobble.ui.UiUtils.toast
import com.google.android.material.transition.platform.MaterialElevationScale
import kotlinx.coroutines.launch
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
        if (!viewModel.sorted)
            loadFriends(1)
        if (isResumed)
            refreshFriendsRecents()
        lastRefreshTime = System.currentTimeMillis()
    }
    private var popupWr: WeakReference<PopupWindow>? = null
    private val viewModel by viewModels<FriendsVM>()
    private val activityViewModel by viewModels<MainNotifierViewModel>({ activity!! })
    private var lastRefreshTime = System.currentTimeMillis()
    private val prefs by lazy { MainPrefs(context!!) }
    private var _binding: ContentFriendsBinding? = null
    private val binding
        get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ContentFriendsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun loadFriends(page: Int): Boolean {
        _binding ?: return false
        binding.friendsGrid.layoutManager ?: return false

        if (Stuff.isOnline) {
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

    override fun onResume() {
        super.onResume()
        if (binding.friendsGrid.adapter == null)
            postInit()
        else if (System.currentTimeMillis() - lastRefreshTime >= Stuff.RECENTS_REFRESH_INTERVAL &&
            (viewModel.page == 1 || viewModel.sorted)
        )
            runnable.run()
        setTitle(0)
    }

    override fun onPause() {
        adapter.handler.removeCallbacks(runnable)
        super.onPause()
    }

    private fun postInit() {
        setTitle(0)

        viewModel.username = activityViewModel.peekUser().name

        binding.friendsSwipeRefresh.setProgressCircleColors()
        binding.friendsSwipeRefresh.setOnRefreshListener {
            viewModel.sorted = false
            binding.friendsSort.hide()
            loadFriends(1)
            if (isResumed)
                refreshFriendsRecents()
            lastRefreshTime = System.currentTimeMillis()
        }

        val glm = GridLayoutManager(context!!, getNumColumns())
        binding.friendsGrid.layoutManager = glm
        (binding.friendsGrid.itemAnimator as SimpleItemAnimator?)?.supportsChangeAnimations = false
        adapter = FriendsAdapter(binding, viewModel)
        binding.friendsGrid.adapter = adapter
        binding.friendsGrid.addItemDecoration(SimpleHeaderDecoration())

        var itemDecor = DividerItemDecoration(context!!, DividerItemDecoration.HORIZONTAL)
        itemDecor.setDrawable(
            ContextCompat.getDrawable(
                context!!,
                R.drawable.shape_divider_chart
            )!!
        )
        binding.friendsGrid.addItemDecoration(itemDecor)

        itemDecor = DividerItemDecoration(context!!, DividerItemDecoration.VERTICAL)
        itemDecor.setDrawable(
            ContextCompat.getDrawable(
                context!!,
                R.drawable.shape_divider_chart
            )!!
        )
        binding.friendsGrid.addItemDecoration(itemDecor)


        val loadMoreListener = EndlessRecyclerViewScrollListener(glm) {
            loadFriends(it)
        }

        loadMoreListener.currentPage = viewModel.page
        adapter.loadMoreListener = loadMoreListener

        binding.friendsSort.setOnClickListener {
            viewModel.friendsFiltered.sortByDescending {
                if (viewModel.lastPlayedTracksMap[it.name] == null) //put users with no tracks at the end
                    0L
                else
                    viewModel.lastPlayedTracksMap[it.name]!!.playedWhen?.time
                        ?: System.currentTimeMillis()
            }
            viewModel.sorted = true
            adapter.notifyDataSetChanged()
            binding.friendsSort.hide()
            binding.friendsGrid.smoothScrollToPosition(0)
        }

        viewModel.friendsReceiver.observe(viewLifecycleOwner) {
            it ?: return@observe
            viewModel.totalPages = it.totalPages
            viewModel.hasLoaded = true

            viewModel.putFriends(it.pageResults, replace = it.page == 1)

            loadMoreListener.currentPage = it.page

            if (it.page == 1) {
                adapter.handler.removeCallbacks(runnable)
                adapter.handler.postDelayed(runnable, Stuff.RECENTS_REFRESH_INTERVAL * 2)
            }

            if (binding.friendsSwipeRefresh.isRefreshing) {
                binding.friendsGrid.scheduleLayoutAnimation()
                binding.friendsSwipeRefresh.isRefreshing = false
            }
            loadMoreListener.loading = false
            val header = binding.friendsHeader.headerText
            if (viewModel.sectionedList.isEmpty()) {
                header.visibility = View.VISIBLE
                header.text = header.context.getString(R.string.no_friends)
            } else
                header.visibility = View.GONE

            adapter.notifyDataSetChanged()
        }

        viewModel.tracksReceiver.observe(viewLifecycleOwner) {
            it ?: return@observe
            val (username, tracksPr) = it
            viewModel.lastPlayedTracksMap[username] = tracksPr.pageResults.firstOrNull()
            viewModel.playCountsMap[username] = tracksPr.totalPages

            val idxChanged = viewModel.sectionedList.indexOfFirst {
                it as UserSerializable
                it.name == username
            }

            if (idxChanged != -1) {
                adapter.notifyItemChanged(idxChanged, 0)
            }

            if (!context!!.isTv && !viewModel.sorted && loadMoreListener.isAllPagesLoaded && viewModel.sectionedList.size > 1 &&
                viewModel.lastPlayedTracksMap.size == viewModel.sectionedList.size
            ) {
                binding.friendsSort.show()
            }
        }

        binding.friendsGrid.addOnScrollListener(loadMoreListener)
        adapter.itemClickListener = this

        if (!viewModel.hasLoaded) {
            loadFriends(1)
            viewModel.refreshPins()
        }
    }

    private fun refreshFriendsRecents() {
        val glm = binding.friendsGrid.layoutManager as GridLayoutManager
        val firstVisible = glm.findFirstVisibleItemPosition()
        val lastVisible = glm.findLastVisibleItemPosition()
        for (i in firstVisible..lastVisible) {
            if (i < 0)
                break
            val userSerializable = viewModel.sectionedList[i] as UserSerializable
            if (!adapter.handler.hasMessages(userSerializable.name.hashCode())) {
                val msg = adapter.handler.obtainMessage(userSerializable.name.hashCode())
                msg.arg1 = i
                adapter.handler.sendMessageDelayed(msg, Stuff.FRIENDS_RECENTS_DELAY)
            }
        }
        adapter.handler.removeCallbacks(runnable)
        adapter.handler.postDelayed(runnable, Stuff.RECENTS_REFRESH_INTERVAL * 2)
    }

    override fun onItemClick(view: View, position: Int) {
        val user = viewModel.sectionedList[position] as UserSerializable
        val gridItem = adapter.getViewBindingForPopup(context!!, position)
        showPopupWindow(gridItem, view, user)
    }

    private fun showPopupWindow(
        contentBinding: GridItemFriendBinding,
        anchor: View,
        userSerializable: UserSerializable
    ) {
        val userLink = userSerializable.url
        val popup =
            PopupWindow(contentBinding.root, anchor.measuredWidth, anchor.measuredHeight, true)
                .apply {
                    elevation = 16.dp.toFloat()
                    isTouchable = true
                    isOutsideTouchable = true
                    setBackgroundDrawable(ColorDrawable())
                    popupWr = WeakReference(this)
                }

        fun postTransition() {
//            TransitionManager.beginDelayedTransition(contentBinding.root, ChangeBounds().setDuration(150))
            val actionsBinding =
                ActionFriendsBinding.inflate(layoutInflater, contentBinding.root, false)
            contentBinding.root.addView(actionsBinding.root, 4)

            contentBinding.friendsPic.layoutParams.width = 130.dp
            contentBinding.friendsPic.layoutParams.height = 130.dp
            val playCount = viewModel.playCountsMap[userSerializable.name] ?: 0
            if (playCount > 0) {
                val since = if (userSerializable.registeredTime == 0L)
                    ""
                else
                    DateFormat.getMediumDateFormat(context).format(userSerializable.registeredTime)
                contentBinding.friendsScrobblesSince.text = getString(
                    R.string.num_scrobbles_since,
                    NumberFormat.getInstance().format(playCount),
                    since
                )
                contentBinding.friendsScrobblesSince.visibility = View.VISIBLE
            }
            if (userSerializable.country.isNotEmpty() && userSerializable.country != "None") {
                contentBinding.friendsCountry.text =
                    getString(
                        R.string.from,
                        userSerializable.country + " " + Stuff.getCountryFlag(userSerializable.country)
                    )
                if (Stuff.DEMO_MODE)
                    contentBinding.friendsCountry.text = getString(R.string.from, "Gensokyo")
                contentBinding.friendsCountry.visibility = View.VISIBLE
            }

            fun updatePinIndicator(isPinned: Boolean) {
                if (!activityViewModel.userIsSelf)
                    return
                contentBinding.friendsPin.visibility = View.VISIBLE
                contentBinding.friendsPin.setImageResource(
                    if (isPinned)
                        R.drawable.vd_pin_off
                    else
                        R.drawable.vd_pin
                )
                val text = getString(
                    if (isPinned)
                        R.string.unpin
                    else
                        R.string.pin
                )
                contentBinding.friendsPin.contentDescription = text
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    contentBinding.friendsPin.tooltipText = text
                }
            }

            updatePinIndicator(viewModel.isPinned(userSerializable.name))

            contentBinding.friendsPin.setOnClickListener {
                val wasPinned = viewModel.isPinned(userSerializable.name)
                val succ = if (wasPinned) {
                    viewModel.removePin(userSerializable)
                    true
                } else {
                    viewModel.addPin(userSerializable)
                }
                if (succ) {
                    updatePinIndicator(!wasPinned)
                    popup.setOnDismissListener {
                        adapter.notifyDataSetChanged()
                    }

                    if (viewModel.pinnedFriends.size > 1 && !prefs.reorderFriendsLearnt && !wasPinned) {
                        context!!.toast(R.string.pin_help, Toast.LENGTH_LONG)
                        prefs.reorderFriendsLearnt = true
                    }
                } else {
                    if (!prefs.proStatus) {
                        activity!!.supportFragmentManager.beginTransaction()
                            .replace(R.id.frame, BillingFragment())
                            .addToBackStack(null)
                            .commit()
                    } else {
                        context!!.toast(
                            getString(
                                R.string.pin_limit_reached,
                                Stuff.MAX_PINNED_FRIENDS
                            )
                        )
                    }
                }
            }

            actionsBinding.friendsProfile.setOnClickListener {
                context!!.openInBrowser(userLink)
            }
            actionsBinding.friendsScrobbles.setOnClickListener {
                (activity as MainActivity).enableGestures()
                activityViewModel.pushUser(userSerializable)
                val f = HomePagerFragment()
                activity!!.supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.frame, f, Stuff.TAG_HOME_PAGER)
                    .addToBackStack(null)
                    .commit()
            }

            if (BuildConfig.DEBUG) {
                actionsBinding.friendsScrobbles.setOnLongClickListener {
                    lifecycleScope.launch {
                        ContextCompat.startForegroundService(
                            context!!,
                            Intent(context!!, ListenAlongService::class.java)
                                .putSingle(userSerializable)
                        )
                    }
                    true
                }
            }

            actionsBinding.friendsCharts.setOnClickListener {
                (activity as MainActivity).enableGestures()
                val f = HomePagerFragment()
                f.arguments = Bundle().apply {
                    putInt(Stuff.ARG_TYPE, 3)
                }
                activity!!.supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.frame, f, Stuff.TAG_HOME_PAGER)
                    .addToBackStack(null)
                    .commit()
            }
            actionsBinding.root.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            contentBinding.root.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

            val screenPoint = intArrayOf(0, 0)
            binding.root.getLocationInWindow(screenPoint)
            val (screenX, screenY) = screenPoint
            val fragmentW = binding.root.measuredWidth
            val fragmentH = binding.root.measuredHeight
            val w = min((0.8 * fragmentW).toInt(), 400.dp)
            val h = contentBinding.root.measuredHeight
            popup.update(
                (fragmentW - w) / 2 + screenX,
                ((fragmentH - h) / 1.2).toInt() + screenY,
                w,
                h
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            popup.exitTransition = Fade().apply {
                duration = 100
            }
            popup.enterTransition = MaterialElevationScale(true).apply {
                duration = 300
            }
        }
        contentBinding.root.postDelayed({ postTransition() }, 10)

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
        val cols =
            (resources.displayMetrics.widthPixels - (activity as MainActivity).coordinatorPadding) /
                    resources.getDimension(R.dimen.grid_size).roundToInt()
        return cols.coerceIn(2, 5)
    }
}