package com.arn.scrobble.friends

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
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.arn.scrobble.App
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.ListenAlong
import com.arn.scrobble.MainActivity
import com.arn.scrobble.MainNotifierViewModel
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.putSingle
import com.arn.scrobble.databinding.ContentFriendsBinding
import com.arn.scrobble.databinding.GridItemFriendBinding
import com.arn.scrobble.scrobbleable.AccountType
import com.arn.scrobble.scrobbleable.Scrobblables
import com.arn.scrobble.ui.EndlessRecyclerViewScrollListener
import com.arn.scrobble.ui.FabData
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.ui.SimpleHeaderDecoration
import com.arn.scrobble.ui.UiUtils.dp
import com.arn.scrobble.ui.UiUtils.setProgressCircleColors
import com.arn.scrobble.ui.UiUtils.setTitle
import com.arn.scrobble.ui.UiUtils.setupInsets
import com.arn.scrobble.ui.UiUtils.toast
import com.google.android.material.transition.platform.MaterialElevationScale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.text.NumberFormat
import kotlin.math.min
import kotlin.math.roundToInt


/**
 * Created by arn on 09/07/2017.
 */

class FriendsFragment : Fragment(), ItemClickListener {

    private var timedRefreshJob: Job? = null
    private lateinit var adapter: FriendsAdapter
    private var popupWr: WeakReference<PopupWindow>? = null
    private val viewModel by viewModels<FriendsVM>()
    private val activityViewModel by activityViewModels<MainNotifierViewModel>()
    private var lastRefreshTime = System.currentTimeMillis()
    private val prefs = App.prefs
    private var _binding: ContentFriendsBinding? = null
    private var fabData: FabData? = null
    private val binding
        get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ContentFriendsBinding.inflate(inflater, container, false)
        binding.friendsGrid.setupInsets()
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun loadFriends(page: Int): Boolean {
        _binding ?: return false
        binding.friendsGrid.layoutManager ?: return false

        return if (page <= viewModel.totalPages || viewModel.totalPages == 0) {
            if ((page == 1 && (binding.friendsGrid.layoutManager as GridLayoutManager).findFirstVisibleItemPosition() < 15) || page > 1) {
                viewModel.loadFriendsList(page, activityViewModel.currentUser)
            }
            if (adapter.itemCount == 0)
                binding.swipeRefresh.isRefreshing = true
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

        if (viewModel.total == 0)
            setTitle(R.string.friends)
        else
            setTitle(
                resources.getQuantityString(
                    R.plurals.num_friends,
                    viewModel.total,
                    viewModel.total
                )
            )
        if (binding.friendsGrid.adapter == null)
            postInit()
        else
            doNextTimedRefresh()
        activityViewModel.fabData.value = fabData

    }

    override fun onPause() {
        super.onPause()
        activityViewModel.fabData.value = null
    }

    private fun postInit() {
        viewModel.showsPins =
            activityViewModel.userIsSelf && Scrobblables.current?.userAccount?.type == AccountType.LASTFM

        binding.swipeRefresh.setProgressCircleColors()
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.sorted = false
            activityViewModel.fabData.value = null
            loadFriends(1)
            if (isResumed)
                refreshFriendsRecents()
            lastRefreshTime = System.currentTimeMillis()
        }

        val glm = GridLayoutManager(requireContext(), getNumColumns())
        binding.friendsGrid.layoutManager = glm
        (binding.friendsGrid.itemAnimator as SimpleItemAnimator?)?.supportsChangeAnimations = false
        adapter = FriendsAdapter(binding, viewModel)
        binding.friendsGrid.adapter = adapter
        binding.friendsGrid.addItemDecoration(SimpleHeaderDecoration())

        var itemDecor = DividerItemDecoration(requireContext(), DividerItemDecoration.HORIZONTAL)
        itemDecor.setDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.shape_divider_chart
            )!!
        )
        binding.friendsGrid.addItemDecoration(itemDecor)

        itemDecor = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        itemDecor.setDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.shape_divider_chart
            )!!
        )
        binding.friendsGrid.addItemDecoration(itemDecor)


        val loadMoreListener = EndlessRecyclerViewScrollListener(glm) {
            loadFriends(it)
        }

        loadMoreListener.currentPage = viewModel.page
        adapter.loadMoreListener = loadMoreListener
        binding.friendsGrid.isVisible = true

        viewModel.friendsReceiver.observe(viewLifecycleOwner) {
            it ?: return@observe
            viewModel.totalPages = it.totalPages
            viewModel.total = it.total
            viewModel.hasLoaded = true

            if (viewModel.total > 0 && isResumed)
                setTitle(
                    resources.getQuantityString(
                        R.plurals.num_friends,
                        viewModel.total,
                        viewModel.total
                    )
                )

            viewModel.putFriends(it.pageResults, replace = it.page == 1)

            loadMoreListener.currentPage = it.page


            if (binding.swipeRefresh.isRefreshing) {
                binding.friendsGrid.scheduleLayoutAnimation()
                binding.swipeRefresh.isRefreshing = false
            }
            loadMoreListener.loading = false
            binding.empty.isVisible = viewModel.sectionedList.isEmpty()
            adapter.notifyDataSetChanged()

            doNextTimedRefresh()
        }

        viewModel.tracksReceiver.observe(viewLifecycleOwner) {
            val (username, tracksPr) = it
            viewModel.lastPlayedTracksMap[username] = tracksPr.pageResults.firstOrNull()
            viewModel.playCountsMap[username] = tracksPr.totalPages

            val idxChanged = viewModel.sectionedList.indexOfFirst { user ->
                user as UserSerializable
                user.name == username
            }

            if (idxChanged != -1) {
                adapter.notifyItemChanged(idxChanged, 0)
            }

            if (!Stuff.isTv && !viewModel.sorted &&
                loadMoreListener.isAllPagesLoaded &&
                viewModel.sectionedList.size > 1 &&
                viewModel.lastPlayedTracksMap.size == viewModel.sectionedList.size - viewModel.privateUsers.size
            ) {
                fabData = FabData(
                    viewLifecycleOwner,
                    R.string.sort,
                    R.drawable.vd_sort_clock,
                    {
                        viewModel.friendsFiltered.sortByDescending {
                            if (viewModel.lastPlayedTracksMap[it.name] == null) //put users with no tracks at the end
                                0L
                            else
                                viewModel.lastPlayedTracksMap[it.name]!!.playedWhen?.time
                                    ?: System.currentTimeMillis()
                        }
                        viewModel.sorted = true
                        adapter.notifyDataSetChanged()
                        activityViewModel.fabData.value = null
                        binding.friendsGrid.smoothScrollToPosition(0)
                    }
                )
                if (isResumed)
                    activityViewModel.fabData.value = fabData
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
            viewModel.viewModelScope.launch {
                adapter.loadFriendsRecents(userSerializable.name)
            }
        }
    }

    private fun doNextTimedRefresh() {
        timedRefreshJob?.cancel()
        timedRefreshJob = viewLifecycleOwner.lifecycleScope.launch {
            if (!isResumed)
                return@launch

            if (System.currentTimeMillis() - lastRefreshTime < Stuff.RECENTS_REFRESH_INTERVAL * 2)
                delay(Stuff.RECENTS_REFRESH_INTERVAL * 2)

            if (!isResumed)
                return@launch

            if (!viewModel.sorted && viewModel.page == 1) {
                loadFriends(1)
                refreshFriendsRecents()
            }
            lastRefreshTime = System.currentTimeMillis()
        }
    }

    override fun onItemClick(view: View, position: Int) {
        val user = viewModel.sectionedList[position] as UserSerializable
        val gridItem = adapter.getViewBindingForPopup(requireContext(), position)
        showPopupWindow(gridItem, view, user)
    }

    private fun showPopupWindow(
        contentBinding: GridItemFriendBinding,
        anchor: View,
        userSerializable: UserSerializable
    ) {
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
            contentBinding.friendsPic.layoutParams.width = 150.dp
            contentBinding.friendsPic.layoutParams.height = 150.dp
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
                if (prefs.demoMode)
                    contentBinding.friendsCountry.text = getString(R.string.from, "Gensokyo")
                contentBinding.friendsCountry.visibility = View.VISIBLE
            }

            if (userSerializable.realname.isNotEmpty()) {
                contentBinding.friendsUsername.text = userSerializable.name
                contentBinding.friendsUsername.visibility = View.VISIBLE
            }

            fun updatePinIndicator(isPinned: Boolean) {
                if (!viewModel.showsPins)
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
                        requireContext().toast(R.string.pin_help, Toast.LENGTH_LONG)
                        prefs.reorderFriendsLearnt = true
                    }
                } else {
                    if (!prefs.proStatus) {
                        findNavController().navigate(R.id.billingFragment)
                    } else {
                        requireContext().toast(
                            getString(
                                R.string.pin_limit_reached,
                                Stuff.MAX_PINNED_FRIENDS
                            )
                        )
                    }
                }
            }

            contentBinding.friendsLinksGroup.isVisible = true
            contentBinding.friendsScrobbles.setOnClickListener {
//                activityViewModel.pushUser(userSerializable)
                findNavController().navigate(
                    R.id.othersHomePagerFragment,
                    Bundle().putSingle(userSerializable)
                )
            }
            contentBinding.friendsProfile.setOnClickListener {
                Stuff.openInBrowser(userSerializable.url)
            }

            if (BuildConfig.DEBUG) {
                contentBinding.friendsScrobbles.setOnLongClickListener {
                    requireContext().sendBroadcast(
                        Intent(NLService.iLISTEN_ALONG)
                            .setPackage(requireContext().packageName)
                            .putExtra(ListenAlong.USERNAME_EXTRA, userSerializable.name)
                    )
                    true
                }
            }

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
        val wm = ContextCompat.getSystemService(rootView.context, WindowManager::class.java)!!

        val lp = rootView.layoutParams as WindowManager.LayoutParams
        lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
        lp.dimAmount = 0.4f
        wm.updateViewLayout(rootView, lp)
    }

    private fun getNumColumns(): Int {
        val cols = (activity as MainActivity).binding.ctl.width /
                resources.getDimension(R.dimen.grid_size).roundToInt()
        return cols.coerceIn(2, 5)
    }
}