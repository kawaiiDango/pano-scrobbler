package com.arn.scrobble.recents

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.transition.Fade
import android.transition.TransitionManager
import android.util.DisplayMetrics
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import coil.dispose
import coil.load
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.LFMRequester
import com.arn.scrobble.MainActivity
import com.arn.scrobble.MainNotifierViewModel
import com.arn.scrobble.R
import com.arn.scrobble.RandomFragment
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.toBundle
import com.arn.scrobble.charts.TimePeriodsGenerator
import com.arn.scrobble.databinding.ContentRecentsBinding
import com.arn.scrobble.databinding.CoordinatorMainBinding
import com.arn.scrobble.info.InfoFragment
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.ui.EndlessRecyclerViewScrollListener
import com.arn.scrobble.ui.FocusChangeListener
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.ui.ItemLongClickListener
import com.arn.scrobble.ui.PaletteTransition
import com.arn.scrobble.ui.SimpleHeaderDecoration
import com.arn.scrobble.ui.UiUtils
import com.arn.scrobble.ui.UiUtils.adjustHeight
import com.arn.scrobble.ui.UiUtils.getTintedDrawable
import com.arn.scrobble.ui.UiUtils.memoryCacheKey
import com.arn.scrobble.ui.UiUtils.setArrowColors
import com.arn.scrobble.ui.UiUtils.setProgressCircleColors
import com.arn.scrobble.ui.UiUtils.showWithIcons
import com.arn.scrobble.ui.UiUtils.toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.robinhood.spark.animation.MorphSparkAnimator
import de.umass.lastfm.ImageSize
import de.umass.lastfm.Track
import java.net.URLEncoder
import java.util.Objects
import kotlin.math.max


/**
 * Created by arn on 09/07/2017.
 */

open class RecentsFragment : Fragment(), ItemClickListener, RecentsAdapter.SetHeroTrigger {
    private lateinit var adapter: RecentsAdapter
    private val prefs by lazy { MainPrefs(context!!) }
    private var timedRefresh = Runnable {
        if (viewModel.toTime == null)
            loadRecents(1)
        val ps = coordinatorBinding.coordinator.paddingStart
        if (activity != null && ps > 0)
            LFMRequester(
                context!!,
                viewLifecycleOwner.lifecycleScope,
                (activity as MainActivity).mainNotifierViewModel.drawerData
            ).getDrawerInfo()
        lastRefreshTime = System.currentTimeMillis()
    }
    private lateinit var coordinatorBinding: CoordinatorMainBinding
    private lateinit var sparklineGroup: Array<View>
    private lateinit var heroButtonsGroup: Array<MaterialButton>
    // constraintLayout Groups have a bug where changing visibility doesn't preserve alpha

    private var _binding: ContentRecentsBinding? = null
    private val binding
        get() = _binding!!
    private var lastRefreshTime = System.currentTimeMillis()
    private val refreshHandler by lazy { Handler(Looper.getMainLooper()) }
    private val viewModel by viewModels<TracksVM>()
    private val activityViewModel by activityViewModels<MainNotifierViewModel>()
    private var animSet: AnimatorSet? = null
    protected open val isShowingLoves = false
    private val username: String?
        get() {
            return if (activityViewModel.userIsSelf)
                null
            else
                activityViewModel.currentUser.name
        }

    private val focusChangeListener = object : FocusChangeListener {
        //only called when !view.isInTouchMode
        override fun onFocus(view: View, position: Int) {
            if (!view.isInTouchMode) {
                val pos = IntArray(2)
                view.getLocationInWindow(pos)

                if (pos[1] + view.height > coordinatorBinding.coordinator.height && coordinatorBinding.appBar.isExpanded)
                    coordinatorBinding.appBar.setExpanded(expanded = false, animate = true)
            }
        }
    }

    private val itemLongClickListener = object : ItemLongClickListener {
        override fun onItemLongClick(view: View, position: Int) {
            val track = adapter.getItem(position) as? Track ?: return
            showTrackInfo(track)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ContentRecentsBinding.inflate(inflater, container, false)
        coordinatorBinding = (activity as MainActivity).binding.coordinatorMain
        sparklineGroup = arrayOf(
            coordinatorBinding.sparkline,
            coordinatorBinding.sparklineTickBottom,
            coordinatorBinding.sparklineHorizontalLabel,
            coordinatorBinding.sparklineTickTop
        )

        heroButtonsGroup = arrayOf(
            coordinatorBinding.heroCalendar,
            coordinatorBinding.heroInfo,
            coordinatorBinding.heroPlay,
            coordinatorBinding.heroShare,
            coordinatorBinding.heroRandomize,
        )
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        if (binding.recentsList.adapter == null)
            postInit()
        else {
            if (viewModel.selectedPos > -1 && viewModel.selectedPos < adapter.itemCount) {
                val track = adapter.getItem(viewModel.selectedPos) as? Track
                if (track != null)
                    onSetHero(viewModel.selectedPos, track, false)
            }
            if (!isShowingLoves &&
                System.currentTimeMillis() - lastRefreshTime >= Stuff.RECENTS_REFRESH_INTERVAL &&
                viewModel.page == 1
            )
                timedRefresh.run()
        }
        activity ?: return
        if (isShowingLoves) {
            coordinatorBinding.heroCalendar.isEnabled = false
        } else if (!Stuff.isTv) {
            coordinatorBinding.heroCalendar.isEnabled = true
        }
        viewModel.reemitColors()
    }

    override fun onPause() {
        animSet?.end()
        super.onPause()
    }

    private fun postInit() {
        val activity = activity as MainActivity? ?: return
        coordinatorBinding.toolbar.title = null
        coordinatorBinding.ctl.adjustHeight()
        val llm = LinearLayoutManager(context!!)
        binding.recentsList.layoutManager = llm

        viewModel.username = username
        adapter = RecentsAdapter(binding)
        adapter.viewModel = viewModel
        adapter.isShowingLoves = isShowingLoves
        adapter.isShowingAlbums = prefs.showAlbumInRecents
        adapter.isShowingPlayers = !isShowingLoves && username == null &&
                prefs.proStatus && prefs.showScrobbleSources
        adapter.trackBundleLd = activity.mainNotifierViewModel.trackBundleLd

//        adapter.setStatusHeader()

        animSet = AnimatorSet()

        binding.recentsList.addItemDecoration(SimpleHeaderDecoration())
        binding.recentsSwipeRefresh.setProgressCircleColors()
        binding.recentsSwipeRefresh.setOnRefreshListener {
            viewModel.toTime = null
            loadRecents(1)
        }
        binding.recentsSwipeRefresh.isRefreshing = false
        binding.recentsList.adapter = adapter
        (binding.recentsList.itemAnimator as DefaultItemAnimator?)?.supportsChangeAnimations = false

        val loadMoreListener = EndlessRecyclerViewScrollListener(llm) {
            loadRecents(it)
        }
        loadMoreListener.currentPage = viewModel.page

        binding.recentsList.addOnScrollListener(loadMoreListener)
        adapter.loadMoreListener = loadMoreListener
        adapter.itemClickListener = this
        adapter.itemLongClickListener = itemLongClickListener
        adapter.focusChangeListener = focusChangeListener
        adapter.setHeroListener = this

        if (coordinatorBinding.sparkline.adapter == null) { // not inited
            coordinatorBinding.sparkline.sparkAnimator = MorphSparkAnimator()
            coordinatorBinding.sparkline.adapter = SparkLineAdapter()
            sparklineGroup.forEach { it.visibility = View.VISIBLE }
        }
        viewModel.tracksReceiver.observe(viewLifecycleOwner) {
            it ?: return@observe
            synchronized(viewModel.tracks) {
                val oldList = mutableListOf<Track>()
                oldList.addAll(viewModel.tracks)
                viewModel.totalPages = max(1, it.totalPages) //dont let totalpages be 0
                if (it.page == 1)
                    viewModel.tracks.clear()
                it.forEach { track ->
                    if (it.toString() in viewModel.deletedTracksStringSet)
                        return@forEach
                    if (!track.isNowPlaying || it.page == 1)
                        viewModel.tracks.add(track)
                }
                adapter.populate(oldList)
            }
            if (viewModel.page != it.page && Stuff.isTv)
                loadRecents(1, true)
            loadMoreListener.currentPage = it.page

            if (it.page == 1 && !isShowingLoves) {
                if (username == null)
                    viewModel.loadPending(2, false)
                refreshHandler.removeCallbacks(timedRefresh)
                refreshHandler.postDelayed(timedRefresh, Stuff.RECENTS_REFRESH_INTERVAL)
            }
        }

        if (!isShowingLoves)
            activity.mainNotifierViewModel.editData.observe(viewLifecycleOwner) {
                it?.let {
                    adapter.editTrack(it)
                }
            }

        viewModel.listenerTrendReceiver.observe(viewLifecycleOwner) {
            it ?: return@observe
            setGraph(it)
        }

        if (!isShowingLoves && username == null)
            viewModel.loadPending(2, !activity.pendingSubmitAttempted)
                .observe(viewLifecycleOwner) {
                    it ?: return@observe
                    activity.pendingSubmitAttempted = true
                    adapter.setPending(activity.supportFragmentManager, it)
                }

        coordinatorBinding.heroShare.setOnClickListener {
            val track = coordinatorBinding.heroImg.tag
            if (track is Track) {
                val heart = if (track.isLoved) "♥️" else ""

                var shareText = if (username == null)
                    getString(
                        R.string.recents_share,
                        heart + getString(R.string.artist_title, track.artist, track.name),
                        Stuff.myRelativeTime(context!!, track.playedWhen, true)
                    )
                else
                    getString(
                        R.string.recents_share_username,
                        heart + getString(R.string.artist_title, track.artist, track.name),
                        Stuff.myRelativeTime(context!!, track.playedWhen, true),
                        username
                    )
                if (activity.billingViewModel.proStatus.value != true)
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
            val track = coordinatorBinding.heroImg.tag
            if (track is Track) {
                showTrackInfo(track)
                if (!prefs.longPressLearnt) {
                    context!!.toast(R.string.info_long_press_hint, Toast.LENGTH_LONG)
                    prefs.longPressLearnt = true
                }
            }
        }

        coordinatorBinding.heroRandomize.setOnClickListener {
            activity.supportFragmentManager
                .beginTransaction()
                .replace(
                    R.id.frame,
                    RandomFragment().apply {
                        arguments = Bundle().apply {
                            putInt(
                                Stuff.ARG_TYPE,
                                if (isShowingLoves)
                                    Stuff.TYPE_LOVES
                                else
                                    Stuff.TYPE_TRACKS
                            )
                        }
                    }
                )
                .addToBackStack(null)
                .commit()
        }

        if (BuildConfig.DEBUG)
            coordinatorBinding.heroPlay.setOnLongClickListener {
                val track = coordinatorBinding.heroImg.tag
                if (track is Track) {
                    Stuff.openInBrowser(
                        "https://en.touhouwiki.net/index.php?search=" +
                                URLEncoder.encode("${track.artist} - ${track.name}", "UTF-8")
                    )
                }
                true
            }

        coordinatorBinding.heroPlay.setOnClickListener {
            val track = coordinatorBinding.heroImg.tag
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

        if (!isShowingLoves) {
            coordinatorBinding.heroCalendar.setOnClickListener { view ->
                val anchorTime = viewModel.toTime ?: System.currentTimeMillis()
                val timePeriodsToIcons =
                    TimePeriodsGenerator(activityViewModel.currentUser.registeredTime, anchorTime, context!!).recentsTimeJumps

                val popupMenu = PopupMenu(context!!, view)
                timePeriodsToIcons.forEachIndexed { index, (timePeriod, iconRes) ->
                    popupMenu.menu.add(Menu.NONE, index, Menu.NONE, timePeriod.name)
                        .apply {
                            setIcon(iconRes)
                        }
                }
                val customId = -1
                val resetId = -2
                popupMenu.menu.add(
                    Menu.NONE,
                    customId,
                    Menu.NONE,
                    getString(R.string.charts_custom)
                )
                    .apply {
                        setIcon(R.drawable.vd_calendar_today)
                    }

                if (viewModel.toTime != null) {
                    popupMenu.menu.add(Menu.NONE, resetId, Menu.NONE, getString(R.string.reset))
                        .apply {
                            setIcon(R.drawable.vd_cancel)
                        }
                }

                popupMenu.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        customId -> {
                            openCalendar()
                        }
                        resetId -> {
                            viewModel.toTime = null
                            viewModel.loadRecents(1)
                        }
                        else -> {
                            val timePeriod = timePeriodsToIcons[item.itemId].first
                            viewModel.toTime = timePeriod.end
                            viewModel.loadRecents(1)
                            binding.recentsList.scheduleLayoutAnimation()
                        }
                    }
                    true
                }
                popupMenu.showWithIcons(
                    iconTintColor = MaterialColors.getColor(
                        context!!,
                        R.attr.colorSecondary,
                        null
                    )
                )
            }
        }

        if (viewModel.tracks.isEmpty())
            loadRecents(1)
        else
            synchronized(viewModel.tracks) {
                adapter.populate(viewModel.tracks)
            }
        if (!Stuff.isTv) {

            if (username != null) {
                coordinatorBinding.heroInfo.visibility = View.GONE
                coordinatorBinding.heroRandomize.visibility = View.VISIBLE
            } else {
                coordinatorBinding.heroInfo.visibility = View.VISIBLE
                coordinatorBinding.heroRandomize.visibility = View.GONE
            }

            coordinatorBinding.heroPlay.visibility = View.VISIBLE
            coordinatorBinding.heroCalendar.visibility = View.VISIBLE
            coordinatorBinding.heroShare.visibility = View.VISIBLE

        }

        // old value found, could be a uiMode change
        viewModel.paletteColors.value?.setDarkModeFrom(context!!)

        viewModel.paletteColors.observe(viewLifecycleOwner) { colors ->
            if (colors == null) {
                // applicationcontext doesn't know about dark mode
                viewModel.paletteColors.value = PaletteColors(context!!)
                return@observe
            }

            if (!isResumed)
                return@observe


            val contentBgFrom = (binding.recentsSwipeRefresh.background as ColorDrawable).color
            val tintFrom = coordinatorBinding.sparklineHorizontalLabel.textColors.defaultColor
            val animSetList = mutableListOf<Animator>()

            heroButtonsGroup.forEach { button ->
                animSetList += ValueAnimator.ofArgb(
                    tintFrom,
                    colors.foreground
                ).apply {
                    addUpdateListener {
                        button.iconTint = ColorStateList.valueOf(it.animatedValue as Int)
                    }
                }
            }

            sparklineGroup.forEach {
                val textView = it as? TextView ?: return@forEach
                animSetList += ObjectAnimator.ofArgb(
                    textView,
                    "textColor",
                    tintFrom,
                    colors.foreground
                )
            }

            animSetList += ObjectAnimator.ofArgb(
                coordinatorBinding.sparkline,
                "lineColor",
                tintFrom,
                ColorUtils.setAlphaComponent(colors.foreground,
                    if (context!!.resources.getBoolean(R.bool.is_dark))
                        179 // 0.7
                    else
                        230 // 0.9
                )
            )

            if (activity.billingViewModel.proStatus.value != true ||
                prefs.themeTintBackground
            ) {
                if (coordinatorBinding.coordinator.paddingStart > 0)
                    animSetList += ObjectAnimator.ofArgb(
                        activity.binding.navView,
                        "backgroundColor",
                        contentBgFrom,
                        colors.background
                    )
                animSetList += ValueAnimator.ofArgb(contentBgFrom, colors.background).apply {
                    addUpdateListener {
                        //setNavigationBarColor uses binders and lags
                        activity.window.navigationBarColor = it.animatedValue as Int
                    }
                }
                animSetList += ObjectAnimator.ofArgb(
                    binding.recentsSwipeRefresh,
                    "backgroundColor",
                    contentBgFrom,
                    colors.background
                )
                activity.binding.coordinatorMain.ctl.setContentScrimColor(colors.background)
            } else {
                animSetList += ObjectAnimator.ofArgb(
                    binding.recentsSwipeRefresh, "backgroundColor", contentBgFrom,
                    MaterialColors.getColor(context, android.R.attr.colorBackground, null)
                )
            }

            val arrowBgColor = if (colors.primary != colors.foreground)
                colors.primary
            else
                colors.muted

            coordinatorBinding.toolbar.setArrowColors(colors.foreground, arrowBgColor)

            animSet?.apply {
                cancel()
                playTogether(animSetList)
                interpolator = AccelerateDecelerateInterpolator()
                duration = 1000
                start()
            }
        }
    }

    private fun setGraph(points: List<Int>) {
        _binding ?: return
        if (points.isEmpty() || points.all { it == 0 })
            sparklineGroup.forEach { it.visibility = View.INVISIBLE }
        else {
            val sparklineAdapter = coordinatorBinding.sparkline.adapter as SparkLineAdapter
            sparklineAdapter.setData(points)
            val max = sparklineAdapter.max()
            val min = sparklineAdapter.min()
            coordinatorBinding.sparklineTickTop.text = Stuff.humanReadableNum(max)

            if (max != min)
                coordinatorBinding.sparklineTickBottom.text = Stuff.humanReadableNum(min)
            else
                coordinatorBinding.sparklineTickBottom.text = ""
            sparklineGroup.forEach { it.visibility = View.VISIBLE }
        }
    }

    private fun loadRecents(page: Int, force: Boolean = false): Boolean {
        _binding ?: return false

        if (page <= viewModel.totalPages) {
            val firstVisible =
                ((binding.recentsList.layoutManager ?: return false) as LinearLayoutManager)
                    .findFirstVisibleItemPosition()
            if (force || (page == 1 && firstVisible < 5) || page > 1) {
                if (isShowingLoves)
                    viewModel.loadLoves(page)
                else
                    viewModel.loadRecents(page)
            }
            if (adapter.itemCount == 0 || page > 1)
                adapter.setLoading(true)
        } else {
            adapter.setLoading(false)
            adapter.loadMoreListener.isAllPagesLoaded = true
            return false
        }
        return true
    }

    override fun onStop() {
        refreshHandler.removeCallbacks(timedRefresh)
        super.onStop()
    }

    override fun onSetHero(position: Int, track: Track, fullSize: Boolean) {
        _binding ?: return

        //check diff
        val oldTrack = coordinatorBinding.heroImg.tag as Track?

        val trackCopy = Track(track.name, track.url, track.album, track.artist)
            .apply {
                imageUrlsMap = track.imageUrlsMap
                playedWhen = track.playedWhen
                isLoved = track.isLoved
                isNowPlaying = track.isNowPlaying
            }
        //TODO: check
        coordinatorBinding.heroImg.tag = trackCopy
        val imgUrl = if (fullSize)
            track.getWebpImageURL(ImageSize.EXTRALARGE)?.replace("300x300", "600x600")
        else
            track.getWebpImageURL(ImageSize.LARGE)

        if (!fullSize &&
            (oldTrack?.artist != track.artist || oldTrack?.album != track.album || oldTrack?.name != track.name)
        ) {
            viewModel.loadListenerTrend(track.url)
        }

        if (!imgUrl.isNullOrEmpty() && imgUrl == oldTrack?.getWebpImageURL(ImageSize.LARGE)) {
            return
        }
        //load img, animate colors
        val errorDrawable = context!!.getTintedDrawable(
            R.drawable.vd_wave,
            Objects.hash(track.artist, track.name)
        )

        if (!imgUrl.isNullOrEmpty()) {
            coordinatorBinding.heroImg.load(imgUrl) {

                if (coordinatorBinding.heroDarkOverlay.visibility == View.VISIBLE) {
                    placeholderMemoryCacheKey(coordinatorBinding.heroImg.memoryCacheKey)
                    placeholder(R.drawable.vd_wave)
                }
                error(errorDrawable)
                allowHardware(false)
                if (!fullSize)
                    transitionFactory(PaletteTransition.Factory { palette ->
                        if (context != null) {
                            viewModel.paletteColors.value = PaletteColors(context!!, palette)
                            onSetHero(position, track, true)
                        }
                    })
                listener(
                    onError = { imageRequest, errorResult ->
                        if (!fullSize)
                            onSetHero(position, track, true)
                        Stuff.log("Coil err for ${imageRequest.data} : ${errorResult.throwable.message}")
                    }
                )
            }
        } else {
            coordinatorBinding.heroImg.dispose()
            val color = UiUtils.getMatColor(
                coordinatorBinding.heroImg.context,
                Objects.hash(track.artist, track.name)
            )
            val swatch = Palette.Swatch(color, 1)
            val palette = Palette.from(listOf(swatch))
            coordinatorBinding.heroImg.load(errorDrawable)
            viewModel.paletteColors.value = PaletteColors(context!!, palette)
        }
        if (coordinatorBinding.heroDarkOverlay.visibility != View.VISIBLE) {
            TransitionManager.beginDelayedTransition(coordinatorBinding.ctl, Fade())
            coordinatorBinding.heroDarkOverlay.visibility = View.VISIBLE
        }
    }

    override fun onItemClick(view: View, position: Int) {
        val item = adapter.getItem(position) ?: return
        val dateFrame = (view.parent as ViewGroup).findViewById<FrameLayout>(R.id.date_frame)
        if (item !is Track) {
            if (view.id == R.id.recents_menu)
                PopupMenuUtils.openPendingPopupMenu(dateFrame,
                    viewLifecycleOwner.lifecycleScope,
                    item,
                    {
                        viewModel.loadPending(2, false)
                    }
                )
            return
        }
        when (view.id) {
            R.id.recents_menu -> openTrackPopupMenu(dateFrame, item)
            R.id.recents_img_overlay -> {
                loveToggle(view, item)
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            }
            else -> {
                val lastClickedPos = viewModel.selectedPos
                viewModel.selectedPos = position
                adapter.notifyItemChanged(lastClickedPos)
                adapter.notifyItemChanged(viewModel.selectedPos)

                if (!coordinatorBinding.appBar.isExpanded) {
//                  recents_list.smoothScrollToPosition(position) //wont work even with smoothscroller

                    val smoothScroller =
                        object : LinearSmoothScroller(binding.recentsList.context) {
                            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics?) =
                                super.calculateSpeedPerPixel(displayMetrics) * 3

                            override fun getVerticalSnapPreference() = SNAP_TO_START
                        }
                    smoothScroller.targetPosition = position
                    binding.recentsList.layoutManager?.startSmoothScroll(smoothScroller)
                    coordinatorBinding.appBar.setExpanded(true, animate = true)
                }

                if (!view.isInTouchMode)
                    openTrackPopupMenu(dateFrame, item)
            }
        }
    }

    private fun openTrackPopupMenu(anchor: View, track: Track) {
        val popup = PopupMenu(context!!, anchor)


        if (username == null) {
            popup.menuInflater.inflate(R.menu.recents_item_menu, popup.menu)
            val loveMenu = popup.menu.findItem(R.id.menu_love)

            if (track.isLoved) {
                loveMenu.title = getString(R.string.unlove)
                loveMenu.icon =
                    ContextCompat.getDrawable(context!!, R.drawable.vd_heart_break_outline)
            }
            if (track.playedWhen == null)
                popup.menu.removeItem(R.id.menu_delete)

            if (isShowingLoves) {
                popup.menu.removeItem(R.id.menu_delete)
                popup.menu.removeItem(R.id.menu_edit)
            }
        }
        if (!anchor.isInTouchMode)
            popup.menuInflater.inflate(R.menu.recents_item_tv_menu, popup.menu)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_love -> loveToggle(
                    (anchor.parent as ViewGroup).findViewById(R.id.recents_img_overlay),
                    track
                )
                R.id.menu_edit -> PopupMenuUtils.editScrobble(activity!!, track)
                R.id.menu_delete -> PopupMenuUtils.deleteScrobble(activity!!, track) { succ ->
                    if (succ)
                        adapter.removeTrack(track)
                    else
                        activity!!.toast(R.string.network_error)
                }
                R.id.menu_play -> coordinatorBinding.heroPlay.callOnClick()
                R.id.menu_info -> coordinatorBinding.heroInfo.callOnClick()
                R.id.menu_share -> coordinatorBinding.heroShare.callOnClick()
                R.id.menu_calendar -> coordinatorBinding.heroCalendar.callOnClick()
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
            .setTitleText(R.string.past_scrobbles)
            .setCalendarConstraints(
                CalendarConstraints.Builder()
                    .setStart(activityViewModel.currentUser.registeredTime)
                    .setEnd(endTime)
                    .setOpenAt(time)
                    .setValidator(object : CalendarConstraints.DateValidator {
                        override fun describeContents() = 0

                        override fun writeToParcel(p0: Parcel, p1: Int) {}

                        override fun isValid(date: Long) = date in activityViewModel.currentUser.registeredTime..endTime
                    })
                    .build()
            )
            .setSelection(time)
            .build()
        dpd.addOnPositiveButtonClickListener {
            viewModel.toTime = Stuff.timeToLocal(it) + (24 * 60 * 60 - 1) * 1000
//                Stuff.log("time=" + Date(viewModel.toTime))
            loadRecents(1, true)
        }

        dpd.addOnNegativeButtonClickListener {
            if (viewModel.toTime != null) {
                viewModel.toTime = null
                loadRecents(1, true)
            }
        }

        dpd.show(parentFragmentManager, null)
    }

    private fun loveToggle(loveIcon: View, track: Track) {
        val alphaAnimator = ObjectAnimator.ofFloat(loveIcon, "alpha", 0f)
        val scalexAnimator = ObjectAnimator.ofFloat(loveIcon, "scaleX", 0f)
        val scaleyAnimator = ObjectAnimator.ofFloat(loveIcon, "scaleY", 0f)
        val rotAnimator = ObjectAnimator.ofFloat(loveIcon, "rotation", 0f)

        val isRtl = resources.getBoolean(R.bool.is_rtl)
        if (track.isLoved) {
            LFMRequester(context!!, lifecycleScope).loveOrUnlove(track, false)

            alphaAnimator.setFloatValues(0f)
            scalexAnimator.setFloatValues(1f, 2f)
            scaleyAnimator.setFloatValues(1f, 2f)
            val startRot = if (isRtl) -10f else 10f
            val toRot = if (isRtl) 50f else -50f
            rotAnimator.setFloatValues(startRot, toRot)
        } else {
            if (loveIcon.background == null)
                loveIcon.background =
                    ContextCompat.getDrawable(context!!, R.drawable.vd_heart_stroked)

            LFMRequester(context!!, lifecycleScope).loveOrUnlove(track, true)

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

        track.isLoved = !track.isLoved
    }

    private fun showTrackInfo(track: Track) {
        val info = InfoFragment()
        info.arguments = track.toBundle().apply {
            val pkgName = if (track.playedWhen != null)
                viewModel.pkgMap[track.playedWhen.time]
            else if (track.isNowPlaying)
                viewModel.pkgMap[0]
            else
                null
            putString(Stuff.ARG_PKG, pkgName)
        }
        info.show(activity!!.supportFragmentManager, null)
    }
}
