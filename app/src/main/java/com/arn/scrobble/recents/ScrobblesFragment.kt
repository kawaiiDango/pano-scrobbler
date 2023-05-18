package com.arn.scrobble.recents

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Parcel
import android.transition.Fade
import android.transition.TransitionManager
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.IntRange
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import coil.load
import coil.memory.MemoryCache
import com.arn.scrobble.App
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.LFMRequester
import com.arn.scrobble.MainActivity
import com.arn.scrobble.MainNotifierViewModel
import com.arn.scrobble.R
import com.arn.scrobble.ReviewPrompter
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.toBundle
import com.arn.scrobble.billing.BillingViewModel
import com.arn.scrobble.charts.TimePeriodsGenerator
import com.arn.scrobble.databinding.ContentMainBinding
import com.arn.scrobble.databinding.ContentScrobblesBinding
import com.arn.scrobble.scrobbleable.ListenBrainz
import com.arn.scrobble.scrobbleable.Scrobblables
import com.arn.scrobble.ui.EndlessRecyclerViewScrollListener
import com.arn.scrobble.ui.FocusChangeListener
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.ui.ItemLongClickListener
import com.arn.scrobble.ui.PaletteTransition
import com.arn.scrobble.ui.SimpleHeaderDecoration
import com.arn.scrobble.ui.UiUtils
import com.arn.scrobble.ui.UiUtils.memoryCacheKey
import com.arn.scrobble.ui.UiUtils.scrollToTopOnInsertToTop
import com.arn.scrobble.ui.UiUtils.setProgressCircleColors
import com.arn.scrobble.ui.UiUtils.setTitle
import com.arn.scrobble.ui.UiUtils.setupInsets
import com.arn.scrobble.ui.UiUtils.showWithIcons
import com.arn.scrobble.ui.UiUtils.toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import de.umass.lastfm.ImageSize
import de.umass.lastfm.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.util.Calendar
import java.util.Objects
import kotlin.math.max


/**
 * Created by arn on 09/07/2017.
 */

open class ScrobblesFragment : Fragment(), ItemClickListener, ScrobblesAdapter.SetHeroTrigger {
    private lateinit var adapter: ScrobblesAdapter
    private val prefs = App.prefs
    private var timedRefreshJob: Job? = null
    private lateinit var coordinatorBinding: ContentMainBinding
    private var _binding: ContentScrobblesBinding? = null
    private val binding
        get() = _binding!!
    private var lastRefreshTime = System.currentTimeMillis()
    private val viewModel by viewModels<TracksVM>()
    private val billingViewModel by activityViewModels<BillingViewModel>()
    private val activityViewModel by activityViewModels<MainNotifierViewModel>()
    private var animSet: AnimatorSet? = null
    private val cal by lazy { Calendar.getInstance() }

    private val focusChangeListener by lazy {
        object : FocusChangeListener {
            //only called when !view.isInTouchMode
            override fun onFocus(view: View, position: Int) {
                if (!view.isInTouchMode) {
                    val pos = IntArray(2)
                    view.getLocationInWindow(pos)

                    if (pos[1] + view.height > coordinatorBinding.coordinator.height && coordinatorBinding.appBar.isExpanded)
                        coordinatorBinding.appBar.setExpanded(false, true)
                }
            }
        }
    }
    private val itemLongClickListener = object : ItemLongClickListener {
        override fun onItemLongClick(view: View, position: Int) {
            val track = adapter.getItem(position) as? Track ?: return
            showTrackInfo(track)
        }
    }
    lateinit var loadMoreListener: EndlessRecyclerViewScrollListener

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ContentScrobblesBinding.inflate(inflater, container, false)
        binding.scrobblesList.setupInsets()
        coordinatorBinding = (activity as MainActivity).binding
        return binding.root
    }

    override fun onDestroyView() {
        coordinatorBinding.heroButtonsGroup.children.forEach { it.setOnClickListener(null) }
        _binding = null
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()

        setTitle(0)
        TransitionManager.beginDelayedTransition(
            coordinatorBinding.ctl,
            Fade().addTarget(coordinatorBinding.heroFrame)
        )
        coordinatorBinding.heroFrame.isVisible = true

        if (binding.scrobblesList.adapter == null)
            postInit()
        else {
            if (viewModel.selectedPos > -1 && viewModel.selectedPos < adapter.itemCount) {
                val track = adapter.getItem(viewModel.selectedPos) as? Track
                if (track != null)
                    onSetHero(track, null)
            }
            doNextTimedRefresh()
        }
        activity ?: return
        viewModel.reemitColors()
    }

    override fun onPause() {
        super.onPause()

        animSet?.end()
        TransitionManager.beginDelayedTransition(
            coordinatorBinding.ctl,
            Fade().addTarget(coordinatorBinding.heroFrame)
        )
        coordinatorBinding.heroFrame.isVisible = false
        val bgColor = MaterialColors.getColor(
            activity,
            android.R.attr.colorBackground,
            null
        )
        coordinatorBinding.sidebarNav.setBackgroundColor(bgColor)
        coordinatorBinding.ctl.setStatusBarScrimColor(bgColor)
    }

    private fun postInit() {
        val activity = activity as MainActivity? ?: return

        val llm = LinearLayoutManager(requireContext())
        binding.scrobblesList.layoutManager = llm

        viewModel.username = if (activityViewModel.userIsSelf)
            null
        else
            activityViewModel.currentUser.name
        adapter = ScrobblesAdapter(
            fragmentBinding = binding,
            itemClickListener = this,
            itemLongClickListener = itemLongClickListener,
            focusChangeListener = focusChangeListener,
            setHeroListener = this,
            viewModel = viewModel
        )
        adapter.isShowingAlbums = prefs.showAlbumInRecents
        adapter.isShowingPlayers = !viewModel.isShowingLoves && activityViewModel.userIsSelf &&
                prefs.proStatus && prefs.showScrobbleSources

        animSet = AnimatorSet()

        binding.scrobblesList.addItemDecoration(SimpleHeaderDecoration())
        binding.swipeRefresh.setProgressCircleColors()
        binding.swipeRefresh.setOnRefreshListener {
            loadRecents(1)
        }
        binding.swipeRefresh.isRefreshing = false
        binding.scrobblesList.adapter = adapter
        binding.scrobblesList.scrollToTopOnInsertToTop()
        (binding.scrobblesList.itemAnimator as DefaultItemAnimator?)?.supportsChangeAnimations =
            false

        loadMoreListener = EndlessRecyclerViewScrollListener(llm) {
            loadRecents(it)
        }
        loadMoreListener.currentPage = viewModel.page
        adapter.loadMoreListener = loadMoreListener

        binding.scrobblesList.addOnScrollListener(loadMoreListener)

        viewModel.pendingScrobblesLd.observe(viewLifecycleOwner) {
            adapter.updatePendingScrobbles(it ?: return@observe)
        }

        viewModel.pendingLovesLd.observe(viewLifecycleOwner) {
            adapter.updatePendingLoves(it ?: return@observe)
        }

        viewModel.tracksReceiver.observe(viewLifecycleOwner) {
            synchronized(viewModel.tracks) {
                viewModel.totalPages = max(1, it.totalPages) //dont let totalpages be 0
                if (it.page == 1) {
                    viewModel.tracks.clear()
                    cal.timeInMillis =
                        it.firstOrNull()?.playedWhen?.time ?: System.currentTimeMillis()
                } else if (viewModel.tracks.isNotEmpty())
                    cal.timeInMillis =
                        viewModel.tracks.last().playedWhen?.time ?: System.currentTimeMillis()

                // mark first scrobble of the day

                var prevDate = cal[Calendar.DAY_OF_YEAR]
                it.forEach { track ->
                    if (it.toString() in viewModel.deletedTracksStringSet)
                        return@forEach

                    if (!track.isNowPlaying || it.page == 1) {
                        if (track.playedWhen != null && !viewModel.isShowingLoves) {
                            cal.time = track.playedWhen
                            val currentDate = cal[Calendar.DAY_OF_YEAR]
                            if (prevDate != currentDate)
                                track.isLastScrobbleOfTheDay = true
                            prevDate = currentDate
                        }
                        viewModel.tracks.add(track)
                    }
                }
                adapter.populate()
            }
            if (viewModel.page != it.page && Stuff.isTv)
                loadRecents(1, true)
            loadMoreListener.currentPage = it.page

            doNextTimedRefresh()
            setLoading(false)
        }


        activityViewModel.editData.observe(viewLifecycleOwner) {
            adapter.editTrack(it)
            ReviewPrompter(requireActivity()).showIfNeeded()
        }

        binding.scrobblesChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener

            binding.swipeRefresh.isRefreshing = true

            when (checkedId) {
                R.id.recents_chip,
                R.id.loves_chip -> {
                    viewModel.isShowingLoves = checkedId == R.id.loves_chip
                    viewModel.toTime = null
                    binding.timeJumpChip.isCheckable = false
                    viewModel.loadedCached = false
                    loadRecents(1, true)
                }
            }
        }

        coordinatorBinding.heroShare.setOnClickListener {
            val track = coordinatorBinding.heroImg.getTag(R.id.hero_track)
            if (track is Track) {
                val heart = if (track.isLoved) "♥️" else ""

                var shareText = if (activityViewModel.userIsSelf)
                    getString(
                        R.string.recents_share,
                        heart + getString(R.string.artist_title, track.artist, track.name),
                        Stuff.myRelativeTime(requireContext(), track.playedWhen, true)
                    )
                else
                    getString(
                        R.string.recents_share_username,
                        heart + getString(R.string.artist_title, track.artist, track.name),
                        Stuff.myRelativeTime(requireContext(), track.playedWhen, true),
                        viewModel.username
                    )
                if (billingViewModel.proStatus.value != true)
                    shareText += "\n\n" + getString(R.string.share_sig)
                val i = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, shareText)
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                }
                startActivity(Intent.createChooser(i, getString(R.string.share_this_song)))
            }
        }

        coordinatorBinding.heroInfo.setOnClickListener {
            val track = coordinatorBinding.heroImg.getTag(R.id.hero_track)
            if (track is Track) {
                showTrackInfo(track)
                if (!prefs.longPressLearnt) {
                    requireContext().toast(R.string.info_long_press_hint, Toast.LENGTH_LONG)
                    prefs.longPressLearnt = true
                }
            }
        }

        binding.randomChip.setOnClickListener {
            val arguments = Bundle().apply {
                putInt(
                    Stuff.ARG_TYPE,
                    if (viewModel.isShowingLoves)
                        Stuff.TYPE_LOVES
                    else
                        Stuff.TYPE_TRACKS
                )
            }
            findNavController().navigate(R.id.randomFragment, arguments)
        }

        if (BuildConfig.DEBUG)
            coordinatorBinding.heroPlay.setOnLongClickListener {
                val track = coordinatorBinding.heroImg.getTag(R.id.hero_track)
                if (track is Track) {
                    Stuff.openInBrowser(
                        "https://en.touhouwiki.net/index.php?search=" +
                                URLEncoder.encode("${track.artist} - ${track.name}", "UTF-8")
                    )
                }
                true
            }

        coordinatorBinding.heroPlay.setOnClickListener {
            val track = coordinatorBinding.heroImg.getTag(R.id.hero_track)
            if (track is Track) {
                val pkgName = if (track.playedWhen != null)
                    viewModel.pkgMap[track.playedWhen.time]
                else if (track.isNowPlaying)
                    viewModel.pkgMap[0]
                else
                    null
                Stuff.launchSearchIntent(track, pkgName)
            }
        }

        binding.timeJumpChip.setOnClickListener { view ->
            val anchorTime = viewModel.toTime ?: System.currentTimeMillis()
            val timePeriodsToIcons =
                TimePeriodsGenerator(
                    activityViewModel.currentUser.registeredTime,
                    anchorTime,
                    requireContext()
                ).recentsTimeJumps

            val popupMenu = PopupMenu(requireContext(), view)
            timePeriodsToIcons.forEachIndexed { index, (timePeriod, iconRes) ->
                popupMenu.menu.add(Menu.NONE, index, Menu.NONE, timePeriod.name)
                    .apply {
                        setIcon(iconRes)
                    }
            }
            val customId = -1
            popupMenu.menu.add(
                Menu.NONE,
                customId,
                Menu.NONE,
                getString(R.string.charts_custom)
            )
                .apply {
                    setIcon(R.drawable.vd_calendar_today)
                }

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    customId -> {
                        openCalendar()
                    }

                    else -> {
                        val timePeriod = timePeriodsToIcons[item.itemId].first
                        viewModel.toTime = timePeriod.end
                        viewModel.loadRecents(1)
                        binding.timeJumpChip.isCheckable = true
                        binding.timeJumpChip.isChecked = true
                        binding.scrobblesList.scheduleLayoutAnimation()
                    }
                }
                true
            }
            popupMenu.showWithIcons(
                iconTintColor = MaterialColors.getColor(
                    requireContext(),
                    com.google.android.material.R.attr.colorSecondary,
                    null
                )
            )
        }

        if (viewModel.tracks.isEmpty())
            loadRecents(1)
        else
            adapter.populate()
        if (!Stuff.isTv)
            coordinatorBinding.heroButtonsGroup.isVisible = true

        // old value found, could be a uiMode change
        viewModel.paletteColors.value?.setDarkModeFrom(requireContext())

        viewModel.paletteColors.observe(viewLifecycleOwner) { colors ->
            if (colors == null) {
                // applicationcontext doesn't know about dark mode
                viewModel.paletteColors.value = PaletteColors(requireContext())
                return@observe
            }

            if (!isResumed)
                return@observe


            val contentBgFrom = (binding.root.background as ColorDrawable).color
            val tintButtonsFrom =
                (coordinatorBinding.heroButtonsGroup.children.first() as MaterialButton)
                    .iconTint.defaultColor

            val animSetList = mutableListOf<Animator>()

            coordinatorBinding.heroButtonsGroup.children.forEach { button ->
                button as MaterialButton
                animSetList += ValueAnimator.ofArgb(
                    tintButtonsFrom,
                    colors.foreground
                ).apply {
                    addUpdateListener {
                        button.iconTint = ColorStateList.valueOf(it.animatedValue as Int)
                    }
                }
            }

            if (billingViewModel.proStatus.value != true ||
                prefs.themeTintBackground
            ) {
                if (UiUtils.isTabletUi)
                    animSetList += ObjectAnimator.ofArgb(
                        activity.binding.sidebarNav,
                        "backgroundColor",
                        contentBgFrom,
                        colors.background
                    )
                animSetList += ObjectAnimator.ofArgb(
                    binding.root,
                    "backgroundColor",
                    contentBgFrom,
                    colors.background
                )

                binding.scrobblesList.children.forEach {
                    val titleTextView = it.findViewById<TextView>(R.id.recents_title)
                    if (titleTextView != null)
                        animSetList += ObjectAnimator.ofArgb(
                            titleTextView,
                            "textColor",
                            tintButtonsFrom,
                            colors.foreground
                        )
                }

                activity.binding.ctl.setStatusBarScrimColor(colors.background)
            } else {
                animSetList += ObjectAnimator.ofArgb(
                    binding.root, "backgroundColor", contentBgFrom,
                    MaterialColors.getColor(context, android.R.attr.colorBackground, null)
                )
            }

            animSet?.apply {
                cancel()
                playTogether(animSetList)
                interpolator = AccelerateDecelerateInterpolator()
                duration = 1000
                start()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            delay(50)
            binding.scrobblesChipGroup.visibility = View.VISIBLE
        }
    }

    private fun loadRecents(page: Int, force: Boolean = false): Boolean {
        _binding ?: return false

        if (page <= viewModel.totalPages) {
            val firstVisible =
                ((binding.scrobblesList.layoutManager ?: return false) as LinearLayoutManager)
                    .findFirstVisibleItemPosition()
            if (force || (page == 1 && firstVisible < 5) || page > 1) {
                if (viewModel.isShowingLoves)
                    viewModel.loadLoves(page)
                else
                    viewModel.loadRecents(page)
            }
            if (adapter.itemCount == 0 || page > 1)
                setLoading(true)
        } else {
            setLoading(false)
            loadMoreListener.isAllPagesLoaded = true
            return false
        }
        activityViewModel.updateCanIndex()
        return true
    }

    private fun doNextTimedRefresh() {
        timedRefreshJob?.cancel()
        timedRefreshJob = viewLifecycleOwner.lifecycleScope.launch {
            if (!isResumed)
                return@launch

            if (System.currentTimeMillis() - lastRefreshTime < Stuff.RECENTS_REFRESH_INTERVAL)
                delay(Stuff.RECENTS_REFRESH_INTERVAL)

            if (!isResumed)
                return@launch

            if (viewModel.toTime == null && !viewModel.isShowingLoves && viewModel.page == 1)
                loadRecents(1)
            lastRefreshTime = System.currentTimeMillis()
        }
    }

    override fun onSetHero(track: Track, cacheKey: MemoryCache.Key?) {
        _binding ?: return

        //TODO: check
        coordinatorBinding.heroImg.setTag(R.id.hero_track, track)
        val imgUrl = track.getWebpImageURL(ImageSize.EXTRALARGE)?.replace("300x300", "600x600")

        val errColor = UiUtils.getMatColor(
            coordinatorBinding.heroImg.context,
            Objects.hash(track.artist, track.name)
        )
        val errDrawable =
            ContextCompat.getDrawable(requireContext(), R.drawable.vd_wave_simple_filled)!!
                .apply { setTint(errColor) }

        coordinatorBinding.heroImg.load(imgUrl ?: "") {
            placeholderMemoryCacheKey(cacheKey ?: coordinatorBinding.heroImg.memoryCacheKey)
            placeholder(errDrawable)
            error(errDrawable)
            allowHardware(false)
            transitionFactory(PaletteTransition.Factory { palette ->
                viewModel.paletteColors.value = PaletteColors(context ?: return@Factory, palette)
            })
            listener(
                onError = { imageRequest, errorResult ->
                    Stuff.log("Coil err for ${imageRequest.data} : ${errorResult.throwable.message}")

                    val swatch = Palette.Swatch(errColor, 1)
                    val palette = Palette.from(listOf(swatch))
                    viewModel.paletteColors.value = PaletteColors(requireContext(), palette)
                }
            )
        }
    }

    override fun onItemClick(view: View, position: Int) {
        val item = adapter.getItem(position)
        val dateFrame = (view.parent as ViewGroup).findViewById<FrameLayout>(R.id.date_frame)
        if (item !is Track) {
            if (view.id == R.id.recents_menu)
                PopupMenuUtils.openPendingPopupMenu(
                    dateFrame,
                    viewLifecycleOwner.lifecycleScope,
                    item
                )
            return
        }
        when (view.id) {
            R.id.recents_menu -> openTrackPopupMenu(dateFrame, item)

            else -> {
                val lastClickedPos = viewModel.selectedPos
                viewModel.selectedPos = position
                adapter.notifyItemChanged(lastClickedPos)
                adapter.notifyItemChanged(viewModel.selectedPos)

                if (!coordinatorBinding.appBar.isExpanded) {
//                  recents_list.smoothScrollToPosition(position) //wont work even with smoothscroller

                    val smoothScroller =
                        object : LinearSmoothScroller(binding.scrobblesList.context) {
                            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics?) =
                                super.calculateSpeedPerPixel(displayMetrics) * 3

                            override fun getVerticalSnapPreference() = SNAP_TO_START
                        }
                    smoothScroller.targetPosition = position
                    binding.scrobblesList.layoutManager?.startSmoothScroll(smoothScroller)
                    coordinatorBinding.appBar.setExpanded(true, true)
                }

                if (!view.isInTouchMode)
                    openTrackPopupMenu(dateFrame, item)
            }
        }
    }

    private fun openTrackPopupMenu(anchor: View, track: Track) {
        val popup = PopupMenu(requireContext(), anchor)


        if (activityViewModel.userIsSelf) {
            popup.menuInflater.inflate(R.menu.recents_item_menu, popup.menu)
            val loveMenu = popup.menu.findItem(R.id.menu_love)

            if (track.isLoved) {
                loveMenu.title = getString(R.string.unlove)
                loveMenu.icon =
                    ContextCompat.getDrawable(requireContext(), R.drawable.vd_heart_break_outline)
            }

            if (track.isHated) {
                popup.menu.findItem(R.id.menu_hate).title = getString(R.string.unhate)
            }

            if (track.playedWhen == null)
                popup.menu.removeItem(R.id.menu_delete)

            if (viewModel.isShowingLoves) {
                popup.menu.removeItem(R.id.menu_delete)
                popup.menu.removeItem(R.id.menu_edit)
            }

            if (Scrobblables.current !is ListenBrainz)
                popup.menu.removeItem(R.id.menu_hate)
        }
        if (!anchor.isInTouchMode)
            popup.menuInflater.inflate(R.menu.recents_item_tv_menu, popup.menu)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_love -> loveUnloveHate(
                    (anchor.parent as ViewGroup).findViewById(R.id.recents_img_overlay),
                    track,
                    if (track.isLoved) 0 else 1
                )

                R.id.menu_hate -> loveUnloveHate(
                    (anchor.parent as ViewGroup).findViewById(R.id.recents_img_overlay),
                    track,
                    if (track.isHated) 0 else -1
                )

                R.id.menu_edit -> PopupMenuUtils.editScrobble(findNavController(), track)
                R.id.menu_delete -> PopupMenuUtils.deleteScrobble(
                    findNavController(),
                    viewModel.viewModelScope,
                    track
                ) { succ ->
                    if (succ)
                        adapter.removeTrack(track)
                    else
                        requireActivity().toast(R.string.network_error)
                }

                R.id.menu_play -> coordinatorBinding.heroPlay.callOnClick()
                R.id.menu_info -> coordinatorBinding.heroInfo.callOnClick()
                R.id.menu_share -> coordinatorBinding.heroShare.callOnClick()
            }
            true
        }

        popup.showWithIcons()
    }

    private fun openCalendar() {
        val time = viewModel.toTime ?: System.currentTimeMillis()
        val endTime = System.currentTimeMillis()
        val dpd = MaterialDatePicker.Builder
            .datePicker()
            .setTitleText(R.string.time_jump)
            .setCalendarConstraints(
                CalendarConstraints.Builder()
                    .setStart(activityViewModel.currentUser.registeredTime)
                    .setEnd(endTime)
                    .setOpenAt(time)
                    .setValidator(object : CalendarConstraints.DateValidator {
                        override fun describeContents() = 0

                        override fun writeToParcel(p0: Parcel, p1: Int) {}

                        override fun isValid(date: Long) =
                            date in activityViewModel.currentUser.registeredTime..endTime
                    })
                    .build()
            )
            .setSelection(time)
            .build()
        dpd.addOnPositiveButtonClickListener {
            viewModel.toTime = Stuff.timeToLocal(it) + (24 * 60 * 60 - 1) * 1000
//                Stuff.log("time=" + Date(viewModel.toTime))
            loadRecents(1, true)

            binding.timeJumpChip.isCheckable = true
            binding.timeJumpChip.isChecked = true
            binding.scrobblesList.scheduleLayoutAnimation()
        }

        dpd.addOnNegativeButtonClickListener {
            if (viewModel.toTime != null) {
                viewModel.toTime = null
                loadRecents(1, true)
            }
        }

        dpd.show(parentFragmentManager, null)
    }

    private fun loveUnloveHate(loveIcon: View, track: Track, @IntRange(-1, 1) score: Int) {
        val alphaAnimator = ObjectAnimator.ofFloat(loveIcon, "alpha", 0f)
        val scalexAnimator = ObjectAnimator.ofFloat(loveIcon, "scaleX", 0f)
        val scaleyAnimator = ObjectAnimator.ofFloat(loveIcon, "scaleY", 0f)
        val rotAnimator = ObjectAnimator.ofFloat(loveIcon, "rotation", 0f)

        val isRtl = resources.getBoolean(R.bool.is_rtl)

        if (score == 0) { // was loved or hated
            LFMRequester(lifecycleScope).loveOrUnlove(track, false)

            alphaAnimator.setFloatValues(0f)
            scalexAnimator.setFloatValues(1f, 2f)
            scaleyAnimator.setFloatValues(1f, 2f)
            val startRot = if (isRtl) -10f else 10f
            val toRot = if (isRtl) 50f else -50f
            rotAnimator.setFloatValues(startRot, toRot)
        } else {
            loveIcon.background = ContextCompat.getDrawable(
                requireContext(),
                if (score == 1)
                    R.drawable.vd_heart_stroked
                else
                    R.drawable.vd_heart_break_stroked
            )

            if (score == 1)
                LFMRequester(lifecycleScope).loveOrUnlove(track, true)
            else
                viewModel.viewModelScope.launch(Dispatchers.IO + LFMRequester.ExceptionNotifier()) {
                    (Scrobblables.current as? ListenBrainz)?.hate(track)
                }

            loveIcon.alpha = 0f
            loveIcon.visibility = View.VISIBLE
            alphaAnimator.setFloatValues(0.9f)
            scalexAnimator.setFloatValues(2f, 1f)
            scaleyAnimator.setFloatValues(2f, 1f)
            val startRot = if (isRtl) 50f else -50f
            val toRot = if (isRtl) -10f else 10f
            rotAnimator.setFloatValues(startRot, toRot)
        }
        val aSet = AnimatorSet()
        aSet.playTogether(alphaAnimator, scalexAnimator, scaleyAnimator, rotAnimator)
        aSet.interpolator = OvershootInterpolator()
        aSet.duration = 800
        aSet.start()

        track.score = score
    }

    private fun showTrackInfo(track: Track) {
        val arguments = track.toBundle().apply {
            val pkgName = if (track.playedWhen != null)
                viewModel.pkgMap[track.playedWhen.time]
            else if (track.isNowPlaying)
                viewModel.pkgMap[0]
            else
                null
            putString(Stuff.ARG_PKG, pkgName)
        }
        findNavController().navigate(R.id.infoFragment, arguments)
    }

    private fun setLoading(b: Boolean) {
        loadMoreListener.loading = b
        binding.swipeRefresh.isRefreshing = false
    }
}
