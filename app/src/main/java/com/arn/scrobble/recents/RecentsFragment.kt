package com.arn.scrobble.recents

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.util.DisplayMetrics
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.view.SupportMenuInflater
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import coil.dispose
import coil.load
import com.arn.scrobble.*
import com.arn.scrobble.Stuff.dp
import com.arn.scrobble.Stuff.getTintedDrwable
import com.arn.scrobble.Stuff.memoryCacheKey
import com.arn.scrobble.Stuff.setArrowColors
import com.arn.scrobble.Stuff.setProgressCircleColors
import com.arn.scrobble.databinding.ContentRecentsBinding
import com.arn.scrobble.databinding.CoordinatorMainBinding
import com.arn.scrobble.info.InfoFragment
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.ui.*
import com.google.android.material.color.MaterialColors
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.robinhood.spark.animation.MorphSparkAnimator
import de.umass.lastfm.ImageSize
import de.umass.lastfm.Track
import java.net.URLEncoder
import java.util.*
import kotlin.math.max


/**
 * Created by arn on 09/07/2017.
 */

open class RecentsFragment : Fragment(), ItemClickListener, RecentsAdapter.SetHeroTrigger {
    private lateinit var adapter: RecentsAdapter
    private val prefs by lazy { MainPrefs(context!!) }
    private var timedRefresh = Runnable {
        if (viewModel.toTime == 0L)
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
    private var _binding: ContentRecentsBinding? = null
    private val binding
        get() = _binding!!
    private var lastRefreshTime = System.currentTimeMillis()
    private val refreshHandler by lazy { Handler(Looper.getMainLooper()) }
    private val username: String?
        get() = parentFragment?.arguments?.getString(Stuff.ARG_USERNAME)
    private lateinit var viewModel: TracksVM
    private var animSet: AnimatorSet? = null
    private var smoothScroller: LinearSmoothScroller? = null
    open val isShowingLoves = false

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
            coordinatorBinding.heroCalendar.visibility = View.INVISIBLE
            coordinatorBinding.heroCalendar.isEnabled = false
        } else if (!MainActivity.isTV) {
            coordinatorBinding.heroCalendar.visibility = View.VISIBLE
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
        Stuff.setCtlHeight(activity)
//      https://stackoverflow.com/questions/31759171/recyclerview-and-java-lang-indexoutofboundsexception-inconsistency-detected-in
        val llm = object : LinearLayoutManager(context!!) {
            override fun onLayoutChildren(recycler: Recycler?, state: RecyclerView.State?) {
                try {
                    super.onLayoutChildren(recycler, state)
                } catch (e: IndexOutOfBoundsException) {
                    Stuff.log("meet a IOOBE in RecyclerView")
                }
            }
        }
        binding.recentsList.layoutManager = llm

        viewModel = VMFactory.getVM(this, TracksVM::class.java)
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

        binding.recentsList.addItemDecoration(SimpleHeaderDecoration(0, 25.dp))
        binding.recentsSwipeRefresh.setProgressCircleColors()
        binding.recentsSwipeRefresh.setOnRefreshListener {
            viewModel.toTime = 0
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
            toggleGraphDetails(true)
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
            if (viewModel.page != it.page && MainActivity.isTV)
                loadRecents(1, true)
            loadMoreListener.currentPage = it.page

            if (!viewModel.loadedNw) {
                loadRecents(1)
                viewModel.loadedNw = true
            } else if (it.page == 1 && !isShowingLoves) {
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
                    activity.mainNotifierViewModel.editData.value = null
                }
            }

        viewModel.listenerTrend
            .observe(viewLifecycleOwner) {
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

        coordinatorBinding.sparkline.setOnClickListener {
            toggleGraphDetails()
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
                }
                startActivity(Intent.createChooser(i, getString(R.string.share_this_song)))
            }
        }

        coordinatorBinding.heroInfo.setOnClickListener {
            val track = coordinatorBinding.heroImg.tag
            if (track is Track) {
                showTrackInfo(track)
                if (!prefs.longPressLearnt) {
                    Stuff.toast(
                        context,
                        getString(R.string.info_long_press_hint),
                        Toast.LENGTH_LONG
                    )
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
                            putString(Stuff.ARG_USERNAME, username)
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
                        context!!,
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
                Stuff.launchSearchIntent(context!!, track, pkgName)
            }
        }

        coordinatorBinding.heroCalendar.setOnClickListener { view ->
            view.isEnabled = false
            val time = if (viewModel.toTime > 0)
                viewModel.toTime
            else
                System.currentTimeMillis()
            val startTime = if (username == null)
                prefs.scrobblingSince
            else
                parentFragment!!.arguments?.getLong(Stuff.ARG_REGISTERED_TIME, 0) ?: 0
            val endTime = System.currentTimeMillis()
            val dpd = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.past_scrobbles)
                .setCalendarConstraints(
                    CalendarConstraints.Builder()
                        .setStart(startTime)
                        .setEnd(endTime)
                        .setOpenAt(time)
                        .setValidator(object : CalendarConstraints.DateValidator {
                            override fun describeContents(): Int {
                                return 0
                            }

                            override fun writeToParcel(p0: Parcel?, p1: Int) {
                            }

                            override fun isValid(date: Long): Boolean {
                                return date in startTime..endTime
                            }
                        })
                        .build()
                )
                .setSelection(time)
                .apply {
                    if (viewModel.toTime != 0L)
                        setNegativeButtonText(R.string.reset)
                }
                .build()
            dpd.addOnPositiveButtonClickListener {
                val tzOffset = TimeZone.getDefault().getOffset(System.currentTimeMillis())
                viewModel.toTime = it - tzOffset + (24 * 60 * 60 - 1) * 1000
//                Stuff.log("time=" + Date(viewModel.toTime))
                loadRecents(1, true)
                view.isEnabled = true
            }
            dpd.addOnNegativeButtonClickListener {
                if (viewModel.toTime != 0L) {
                    viewModel.toTime = 0
                    loadRecents(1, true)
                }
                view.isEnabled = true
            }
            dpd.addOnDismissListener {
                view.isEnabled = true
            }

            dpd.show(parentFragmentManager, null)
        }
        if (viewModel.tracks.isEmpty())
            loadRecents(1)
        else
            synchronized(viewModel.tracks) {
                adapter.populate(viewModel.tracks)
            }
        if (!MainActivity.isTV) {

            if (username != null) {
                coordinatorBinding.heroInfo.visibility = View.GONE
                coordinatorBinding.heroRandomize.visibility = View.VISIBLE
            } else {
                coordinatorBinding.heroInfo.visibility = View.VISIBLE
                coordinatorBinding.heroRandomize.visibility = View.GONE
            }

            coordinatorBinding.heroPlay.visibility = View.VISIBLE
            coordinatorBinding.heroShare.visibility = View.VISIBLE
        }

        viewModel.paletteColors.observe(viewLifecycleOwner) { colors ->
            if (!isResumed)
                return@observe


            val contentBgFrom = (binding.recentsSwipeRefresh.background as ColorDrawable).color
            val tintFrom = coordinatorBinding.sparklineHorizontalLabel.textColors.defaultColor

            val animSetList = mutableListOf<Animator>(
                ObjectAnimator.ofArgb(
                    coordinatorBinding.heroShare,
                    "colorFilter",
                    tintFrom,
                    colors.lightWhite
                ),
                ObjectAnimator.ofArgb(
                    coordinatorBinding.heroCalendar,
                    "colorFilter",
                    tintFrom,
                    colors.lightWhite
                ),
                ObjectAnimator.ofArgb(
                    coordinatorBinding.heroInfo,
                    "colorFilter",
                    tintFrom,
                    colors.lightWhite
                ),
                ObjectAnimator.ofArgb(
                    coordinatorBinding.heroRandomize,
                    "colorFilter",
                    tintFrom,
                    colors.lightWhite
                ),
                ObjectAnimator.ofArgb(
                    coordinatorBinding.heroPlay,
                    "colorFilter",
                    tintFrom,
                    colors.lightWhite
                ),
                ObjectAnimator.ofArgb(
                    coordinatorBinding.sparklineTickTop,
                    "textColor",
                    tintFrom,
                    colors.lightWhite
                ),
                ObjectAnimator.ofArgb(
                    coordinatorBinding.sparklineTickBottom,
                    "textColor",
                    tintFrom,
                    colors.lightWhite
                ),
                ObjectAnimator.ofArgb(
                    coordinatorBinding.sparklineHorizontalLabel,
                    "textColor",
                    tintFrom,
                    colors.lightWhite
                ),
                ObjectAnimator.ofArgb(
                    coordinatorBinding.sparkline,
                    "lineColor",
                    tintFrom,
                    colors.lightWhite
                ),
            )

            if (activity.billingViewModel.proStatus.value != true ||
                prefs.themePaletteBackground
            ) {
                if (coordinatorBinding.coordinator.paddingStart > 0)
                    animSetList += ObjectAnimator.ofArgb(
                        activity.binding.navView,
                        "backgroundColor",
                        contentBgFrom,
                        colors.mutedBg
                    )
                animSetList += ValueAnimator.ofArgb(contentBgFrom, colors.mutedBg).apply {
                    addUpdateListener {
                        //setNavigationBarColor uses binders and lags
                        activity.window.navigationBarColor = it.animatedValue as Int
                    }
                }
                animSetList += ObjectAnimator.ofArgb(
                    binding.recentsSwipeRefresh,
                    "backgroundColor",
                    contentBgFrom,
                    colors.mutedBg
                )
                activity.binding.coordinatorMain.ctl.setContentScrimColor(colors.mutedBg)
            } else {
                animSetList += ObjectAnimator.ofArgb(
                    binding.recentsSwipeRefresh, "backgroundColor", contentBgFrom,
                    MaterialColors.getColor(context, android.R.attr.colorBackground, null)
                )
            }

            val arrowBgColor = if (colors.primDark != colors.lightWhite)
                colors.primDark
            else
                colors.mutedDark

            coordinatorBinding.toolbar.setArrowColors(colors.lightWhite, arrowBgColor)

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
        val frame = coordinatorBinding.sparklineFrame
        if (points.isEmpty() || points.all { it == 0 })
            frame.visibility = View.INVISIBLE
        else {
            frame.visibility = View.VISIBLE
            val sparklineAdapter = coordinatorBinding.sparkline.adapter as SparkLineAdapter
            sparklineAdapter.setData(points)
            val max = sparklineAdapter.max()
            val min = sparklineAdapter.min()
            coordinatorBinding.sparklineTickTop.text = Stuff.humanReadableNum(max)

            if (max != min)
                coordinatorBinding.sparklineTickBottom.text = Stuff.humanReadableNum(min)
            else
                coordinatorBinding.sparklineTickBottom.text = ""
        }
    }

    private fun toggleGraphDetails(init: Boolean = false) {
        _binding ?: return
        var show = prefs.heroGraphDetails
        if (!init)
            show = !show

        if (show) {
            coordinatorBinding.sparkline.setPadding(
                2 * resources.getDimensionPixelSize(R.dimen.graph_margin_details),
                0, 0,
                resources.getDimensionPixelSize(R.dimen.graph_margin_details)
            )
            coordinatorBinding.sparklineHorizontalLabel.visibility = View.VISIBLE
            coordinatorBinding.sparklineTickTop.visibility = View.VISIBLE
            coordinatorBinding.sparklineTickBottom.visibility = View.VISIBLE
        } else {
            coordinatorBinding.sparkline.setPadding(0, 0, 0, 0)
            coordinatorBinding.sparklineHorizontalLabel.visibility = View.GONE
            coordinatorBinding.sparklineTickTop.visibility = View.GONE
            coordinatorBinding.sparklineTickBottom.visibility = View.GONE
        }
        prefs.heroGraphDetails = show
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (isVisible)
            Stuff.setCtlHeight(activity!!)
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
        val errorDrawable = context!!.getTintedDrwable(
            R.drawable.vd_wave,
            Stuff.genHashCode(track.artist, track.name)
        )

        if (!imgUrl.isNullOrEmpty()) {
            coordinatorBinding.heroImg.load(imgUrl) {

                placeholderMemoryCacheKey(coordinatorBinding.heroImg.memoryCacheKey)
                placeholder(R.drawable.vd_wave)
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
                    onError = { imageRequest, throwable ->
                        if (!fullSize)
                            onSetHero(position, track, true)
                        Stuff.log("Coil err for ${imageRequest.data} : $throwable")
                    }
                )
            }
        } else {
            coordinatorBinding.heroImg.dispose()
            val color = Stuff.getMatColor(
                coordinatorBinding.heroImg.context,
                Stuff.genHashCode(track.artist, track.name)
            )
            val swatch = Palette.Swatch(color, 1)
            val palette = Palette.from(listOf(swatch))
            coordinatorBinding.heroImg.load(errorDrawable)
            viewModel.paletteColors.value = PaletteColors(context!!, palette)
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
//                  recents_list.smoothScrollToPosition(position) //wont work even witrh smoothscroller

                    if (smoothScroller == null)
                        smoothScroller =
                            object : LinearSmoothScroller(binding.recentsList.context) {
                                override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics?) =
                                    super.calculateSpeedPerPixel(displayMetrics) * 3

                                override fun getVerticalSnapPreference() = SNAP_TO_START
                            }
                    smoothScroller?.targetPosition = position
                    binding.recentsList.layoutManager?.startSmoothScroll(smoothScroller)
                    coordinatorBinding.appBar.setExpanded(true, animate = true)
                }

                if (!view.isInTouchMode)
                    openTrackPopupMenu(dateFrame, item)
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun openTrackPopupMenu(anchor: View, track: Track) {
        val menuBuilder = MenuBuilder(context)
        val inflater = SupportMenuInflater(context)
        if (username == null) {
            inflater.inflate(R.menu.recents_item_menu, menuBuilder)
            val loveMenu = menuBuilder.findItem(R.id.menu_love)

            if (track.isLoved) {
                loveMenu.title = getString(R.string.unlove)
                loveMenu.icon =
                    ContextCompat.getDrawable(context!!, R.drawable.vd_heart_break_outline)
            }
            if (track.playedWhen == null)
                menuBuilder.removeItem(R.id.menu_delete)

            if (isShowingLoves) {
                menuBuilder.removeItem(R.id.menu_delete)
                menuBuilder.removeItem(R.id.menu_edit)
            }
        }
        if (!anchor.isInTouchMode)
            inflater.inflate(R.menu.recents_item_tv_menu, menuBuilder)

        menuBuilder.setCallback(object : MenuBuilder.Callback {
            override fun onMenuItemSelected(menu: MenuBuilder, item: MenuItem): Boolean {
                when (item.itemId) {
                    R.id.menu_love -> loveToggle(
                        (anchor.parent as ViewGroup).findViewById(R.id.recents_img_overlay),
                        track
                    )
                    R.id.menu_edit -> PopupMenuUtils.editScrobble(activity!!, track)
                    R.id.menu_delete -> PopupMenuUtils.deleteScrobble(activity!!, track) { succ ->
                        if (succ)
                            adapter.removeTrack(track)
                        else
                            Stuff.toast(activity, getString(R.string.network_error))
                    }
                    R.id.menu_play -> coordinatorBinding.heroPlay.callOnClick()
                    R.id.menu_info -> coordinatorBinding.heroInfo.callOnClick()
                    R.id.menu_share -> coordinatorBinding.heroShare.callOnClick()
                    R.id.menu_calendar -> coordinatorBinding.heroCalendar.callOnClick()
                    else -> return false
                }
                return true
            }

            override fun onMenuModeChange(menu: MenuBuilder) {}
        })

        val popupMenu = MenuPopupHelper(context!!, menuBuilder, anchor)
        popupMenu.setForceShowIcon(true)
        popupMenu.show()
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
        info.arguments = Bundle().apply {
            putString(NLService.B_ARTIST, track.artist)
            if (!track.album.isNullOrEmpty())
                putString(NLService.B_ALBUM, track.album)
            putString(NLService.B_TRACK, track.name)
            putString(Stuff.ARG_USERNAME, username)
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
