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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.ListenAlong
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.databinding.ContentFriendsBinding
import com.arn.scrobble.databinding.GridItemFriendBinding
import com.arn.scrobble.main.App
import com.arn.scrobble.main.FabData
import com.arn.scrobble.main.MainActivity
import com.arn.scrobble.main.MainNotifierViewModel
import com.arn.scrobble.ui.EndlessRecyclerViewScrollListener
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.ui.MusicEntryLoaderInput
import com.arn.scrobble.ui.SimpleHeaderDecoration
import com.arn.scrobble.ui.createSkeletonWithFade
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.format
import com.arn.scrobble.utils.Stuff.putSingle
import com.arn.scrobble.utils.UiUtils.collectLatestLifecycleFlow
import com.arn.scrobble.utils.UiUtils.dp
import com.arn.scrobble.utils.UiUtils.expandToHeroIfNeeded
import com.arn.scrobble.utils.UiUtils.setProgressCircleColors
import com.arn.scrobble.utils.UiUtils.setTitle
import com.arn.scrobble.utils.UiUtils.setupInsets
import com.arn.scrobble.utils.UiUtils.toast
import com.google.android.material.transition.platform.MaterialElevationScale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import kotlin.math.min
import kotlin.math.roundToInt


/**
 * Created by arn on 09/07/2017.
 */

class FriendsFragment : Fragment(), ItemClickListener<FriendsVM.FriendsItemHolder> {

    private var timedRefreshJob: Job? = null
    private lateinit var adapter: FriendsAdapter
    private var popupWr: WeakReference<PopupWindow>? = null
    private val viewModel by viewModels<FriendsVM>()
    private val activityViewModel by activityViewModels<MainNotifierViewModel>()
    private val prefs = App.prefs
    private var _binding: ContentFriendsBinding? = null
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

    override fun onStop() {
        if (isVisible)
            popupWr?.get()?.dismiss()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (binding.friendsGrid.adapter != null)
            doNextTimedRefresh()
        showFabIfNeeded()
        (activity as MainActivity).binding.appBar.expandToHeroIfNeeded(false)
    }

    override fun onPause() {
        super.onPause()
        (requireActivity() as? MainActivity)?.hideFab(false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        collectLatestLifecycleFlow(viewModel.total, Lifecycle.State.RESUMED) { total ->
            if (total == 0)
                setTitle(R.string.friends)
            else
                setTitle(
                    resources.getQuantityString(
                        R.plurals.num_friends,
                        viewModel.total.value,
                        viewModel.total.value.format()
                    )
                )
        }

        binding.swipeRefresh.setProgressCircleColors()
        binding.swipeRefresh.setOnRefreshListener {
            (requireActivity() as? MainActivity)?.hideFab(false)
            viewModel.setInput(viewModel.input.value!!.copyCacheBusted(page = 1))
            if (isResumed)
                refreshFriendsRecents()
        }

        val glm = GridLayoutManager(requireContext(), getNumColumns())
        binding.friendsGrid.layoutManager = glm
        (binding.friendsGrid.itemAnimator as SimpleItemAnimator?)?.supportsChangeAnimations = false
        adapter = FriendsAdapter(binding, viewModel, viewLifecycleOwner)
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


        val loadMoreListener = EndlessRecyclerViewScrollListener(glm) { page ->
            if (page <= viewModel.totalPages)
                viewModel.setInput(viewModel.input.value!!.copy(page = page))
            adapter.loadMoreListener.isAllPagesLoaded = page >= viewModel.totalPages
        }

        loadMoreListener.currentPage = viewModel.input.value?.page ?: 1
        adapter.loadMoreListener = loadMoreListener

        val skeleton = binding.friendsGrid.createSkeletonWithFade(
            listItemLayoutResId = R.layout.grid_item_friend,
            skeletonConfigRadiusDp = 100
        )

        collectLatestLifecycleFlow(
            viewModel.friendsCombined.filterNotNull(),
        ) {
            loadMoreListener.currentPage = viewModel.input.value?.page ?: 1

            if (binding.swipeRefresh.isRefreshing && isResumed) {
                binding.friendsGrid.scheduleLayoutAnimation()
            }
            binding.empty.isVisible = it.isEmpty()

            adapter.submitList(it)

            showFabIfNeeded()

            doNextTimedRefresh()
        }

        collectLatestLifecycleFlow(viewModel.hasLoaded) {
            loadMoreListener.loading = !it

            if (!it) {
                if (adapter.itemCount == 0) {
                    skeleton.showSkeleton()
                }

                if (!skeleton.isSkeleton() && viewModel.input.value?.page == 1)
                    binding.swipeRefresh.isRefreshing = true

                binding.empty.isVisible = false
            } else {
                skeleton.showOriginal()
                binding.swipeRefresh.isRefreshing = false

                if (viewModel.friendsCombined.value?.isEmpty() == true) {
                    binding.empty.isVisible = true
                }
            }
        }

        binding.friendsGrid.addOnScrollListener(loadMoreListener)
        adapter.itemClickListener = this

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.setInput(
                    MusicEntryLoaderInput(
                        user = activityViewModel.currentUser,
                        timePeriod = null,
                        type = Stuff.TYPE_FRIENDS,
                        page = 1,
                    ), initial = true
                )
            }
        }
    }

    private fun showFabIfNeeded() {
        if (binding.friendsGrid.adapter != null && !Stuff.isTv && !viewModel.sorted &&
            adapter.loadMoreListener.isAllPagesLoaded &&
            adapter.itemCount > 1 &&
            viewModel.friendsCombined.value!!.count { it.track != null } == adapter.itemCount
        ) {
            val fabData = FabData(
                viewLifecycleOwner,
                R.string.sort,
                R.drawable.vd_sort_clock,
                {
                    viewModel.sortByTime()
                    (requireActivity() as? MainActivity)?.hideFab(false)
                    binding.friendsGrid.smoothScrollToPosition(0)
                }
            )
            activityViewModel.setFabData(fabData)
        }
    }

    private fun refreshFriendsRecents() {
        val glm = binding.friendsGrid.layoutManager as GridLayoutManager
        val firstVisible = glm.findFirstVisibleItemPosition()
        val lastVisible = glm.findLastVisibleItemPosition()
        for (i in firstVisible..lastVisible) {
            if (i < 0)
                break
            viewModel.friendsCombined.value?.get(i)?.let {
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.loadFriendsRecents(it.user.name)
                }
            }
        }
    }

    private fun doNextTimedRefresh() {
        timedRefreshJob?.cancel()
        timedRefreshJob = viewLifecycleOwner.lifecycleScope.launch {
            if (!isResumed)
                return@launch

            if (System.currentTimeMillis() - viewModel.lastFriendsLoadTime < Stuff.RECENTS_REFRESH_INTERVAL * 2)
                delay(Stuff.RECENTS_REFRESH_INTERVAL * 2)

            if (!isResumed)
                return@launch

            if (viewModel.input.value?.page == 1) {
                refreshFriendsRecents()
            }
        }
    }

    override fun onItemClick(view: View, position: Int, item: FriendsVM.FriendsItemHolder) {
        val gridItem = adapter.getViewBindingForPopup(requireContext(), item)
        showPopupWindow(gridItem, view, item)
    }

    private fun showPopupWindow(
        contentBinding: GridItemFriendBinding,
        anchor: View,
        friendsItemHolder: FriendsVM.FriendsItemHolder
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

        val userCached = friendsItemHolder.user

        fun postTransition() {
            contentBinding.friendsPic.layoutParams.width = 150.dp
            contentBinding.friendsPic.layoutParams.height = 150.dp
            val playCount = friendsItemHolder.playCount
            if (playCount > 0 && userCached.registeredTime >= Stuff.TIME_2002) {
                val since =
                    DateFormat.getMediumDateFormat(context).format(userCached.registeredTime)
                contentBinding.friendsScrobblesSince.text = getString(
                    R.string.num_scrobbles_since,
                    playCount.format(),
                    since
                )
                contentBinding.friendsScrobblesSince.visibility = View.VISIBLE
            }
            if (userCached.country.isNotEmpty() && userCached.country != "None") {
                contentBinding.friendsCountry.text =
                    getString(
                        R.string.from,
                        userCached.country + " " + Stuff.getCountryFlag(userCached.country)
                    )
                if (prefs.demoMode)
                    contentBinding.friendsCountry.text = getString(R.string.from, "Gensokyo")
                contentBinding.friendsCountry.visibility = View.VISIBLE
            }

            if (userCached.realname.isNotEmpty()) {
                contentBinding.friendsUsername.text = userCached.name
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

            updatePinIndicator(viewModel.isPinned(userCached.name))

            contentBinding.friendsPin.setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch {

                    val wasPinned = viewModel.isPinned(userCached.name)
                    val succ = if (wasPinned) {
                        viewModel.removePinAndSave(userCached)
                        true
                    } else {
                        viewModel.addPinAndSave(userCached)
                    }
                    if (succ) {
                        updatePinIndicator(!wasPinned)
                        if (!prefs.reorderFriendsLearnt && !wasPinned) {
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
            }

            contentBinding.friendsLinksGroup.isVisible = true
            contentBinding.friendsScrobbles.setOnClickListener {
//                activityViewModel.pushUser(userSerializable)
                findNavController().navigate(
                    R.id.othersHomePagerFragment,
                    Bundle().putSingle(userCached)
                )
            }
            contentBinding.friendsProfile.setOnClickListener {
                Stuff.openInBrowser(userCached.url)
            }

            if (BuildConfig.DEBUG) {
                contentBinding.friendsScrobbles.setOnLongClickListener {
                    requireContext().sendBroadcast(
                        Intent(NLService.iLISTEN_ALONG)
                            .setPackage(requireContext().packageName)
                            .putExtra(ListenAlong.USERNAME_EXTRA, userCached.name)
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

        popup.exitTransition = Fade().apply {
            duration = 100
        }
        popup.enterTransition = MaterialElevationScale(true).apply {
            duration = 300
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