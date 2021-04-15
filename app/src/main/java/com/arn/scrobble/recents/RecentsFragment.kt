package com.arn.scrobble.recents

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.*
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
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
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.view.SupportMenuInflater
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.*
import androidx.recyclerview.widget.RecyclerView.Recycler
import com.arn.scrobble.*
import com.arn.scrobble.Stuff.dp
import com.arn.scrobble.databinding.ContentRecentsBinding
import com.arn.scrobble.databinding.CoordinatorMainBinding
import com.arn.scrobble.info.InfoFragment
import com.arn.scrobble.pending.PendingMenu
import com.arn.scrobble.pref.MultiPreferences
import com.arn.scrobble.ui.*
import com.google.android.material.color.MaterialColors
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.robinhood.spark.animation.MorphSparkAnimator
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import de.umass.lastfm.ImageSize
import de.umass.lastfm.Track
import java.net.URLEncoder
import java.util.*
import kotlin.math.max


/**
 * Created by arn on 09/07/2017.
 */

open class RecentsFragment : Fragment(),
        ItemClickListener, ItemLongClickListener, FocusChangeListener, RecentsAdapter.SetHeroTrigger {
    private lateinit var adapter: RecentsAdapter
    private lateinit var appPrefs: SharedPreferences
    private var timedRefresh = Runnable {
        if (viewModel.toTime == 0L)
            loadRecents(1)
        val ps = coordinatorBinding.coordinator.paddingStart
        if (activity != null && ps > 0)
            LFMRequester(context!!).getDrawerInfo().asAsyncTask()
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
    private lateinit var animSet: AnimatorSet
    private var smoothScroller: LinearSmoothScroller? = null
    open val isShowingLoves = false

    private var colorPrimDark = 0
    private var colorLightWhite = 0
    private var colorMutedDark = 0
    private var colorMutedBlack = 0

    private val editReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent) {
            val extras = i.extras!!
            adapter.editTrack(extras.getString(NLService.B_ARTIST)!!,
                    extras.getString(NLService.B_ALBUM)!!,
                    extras.getString(NLService.B_TITLE)!!,
                    extras.getLong(NLService.B_TIME))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ContentRecentsBinding.inflate(inflater, container, false)
        coordinatorBinding = (activity as Main).binding.coordinatorMain
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
                    viewModel.page == 1)
                timedRefresh.run()
        }
        activity ?: return
        if (isShowingLoves) {
            coordinatorBinding.heroCalendar.visibility = View.INVISIBLE
            coordinatorBinding.heroCalendar.isEnabled = false
        } else if (!Main.isTV){
            coordinatorBinding.heroCalendar.visibility = View.VISIBLE
            coordinatorBinding.heroCalendar.isEnabled = true
        }
    }

    override fun onPause() {
        if (isShowingLoves)
            try {
                adapter.removeHandlerCallbacks()
            }catch (e: Exception){}
        super.onPause()
    }

    private fun postInit() {
        val activity = activity as Main? ?: return
        coordinatorBinding.toolbar.title = null
        Stuff.setAppBarHeight(activity)
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
        adapter.isShowingAlbums = MultiPreferences(context!!).getBoolean(Stuff.PREF_SHOW_RECENTS_ALBUM, false)

//        adapter.setStatusHeader()

        animSet = AnimatorSet()
        animSet.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(p0: Animator?) {}

            override fun onAnimationEnd(p0: Animator?) {
                lastColorLightWhite = colorLightWhite
                lastColorMutedDark = colorMutedDark
                lastColorMutedBlack = colorMutedBlack
            }

            override fun onAnimationCancel(p0: Animator?) {}

            override fun onAnimationStart(p0: Animator?) {}

        })

        binding.recentsList.addItemDecoration(SimpleHeaderDecoration(0, 25.dp))
        Stuff.setProgressCircleColor(binding.recentsSwipeRefresh)
        binding.recentsSwipeRefresh.setOnRefreshListener {
            viewModel.toTime = 0
            loadRecents(1)
        }
        binding.recentsSwipeRefresh.isRefreshing = false
        binding.recentsList.adapter = adapter
        (binding.recentsList.itemAnimator as DefaultItemAnimator?)?.supportsChangeAnimations = false

        val loadMoreListener = object : EndlessRecyclerViewScrollListener(llm) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView) {
                loadRecents(page)
            }
        }
        loadMoreListener.currentPage = viewModel.page

        binding.recentsList.addOnScrollListener(loadMoreListener)
        adapter.loadMoreListener = loadMoreListener
        adapter.itemClickListener = this
        adapter.itemLongClickListener = this
        adapter.focusChangeListener = this
        adapter.setHeroListener = this
        appPrefs = context!!.getSharedPreferences(Stuff.ACTIVITY_PREFS, Context.MODE_PRIVATE)

        if (coordinatorBinding.sparkline.adapter == null) { // not inited
            coordinatorBinding.sparkline.sparkAnimator = MorphSparkAnimator()
            coordinatorBinding.sparkline.adapter = SparkLineAdapter()
            toggleGraphDetails(true)
        }
        viewModel.tracksReceiver.observe(viewLifecycleOwner, {
            it ?: return@observe
            synchronized(viewModel.tracks) {
                val oldList = mutableListOf<Track>()
                oldList.addAll(viewModel.tracks)
                viewModel.totalPages = max(1, it.totalPages) //dont let totalpages be 0
                if (it.page == 1)
                    viewModel.tracks.clear()
                it.forEach { track ->
                    if (viewModel.deletedTracksStringSet.contains(it.toString()))
                        return@forEach
                    if (!track.isNowPlaying || it.page == 1)
                        viewModel.tracks.add(track)
                    if (isShowingLoves && viewModel.imgMap[Stuff.genHashCode(track.artist, track.name)] != null)
                        track.imageUrlsMap = viewModel.imgMap[Stuff.genHashCode(track.artist, track.name)]
                }
                adapter.populate(oldList)
            }
            if (viewModel.page != it.page && Main.isTV)
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
        })
        viewModel.loadHero(null)
                .observe(viewLifecycleOwner, {
                    it ?: return@observe
                    setGraph(it[0])
                })
        if (isShowingLoves)
            viewModel.trackInfo.observe(viewLifecycleOwner, {
                it ?: return@observe
                adapter.setImg(it.first, it.second?.imageUrlsMap)
            })
        else if (username == null)
            viewModel.loadPending(2, !activity.pendingSubmitAttempted)
                    .observe(viewLifecycleOwner, {
                        it ?: return@observe
                        activity.pendingSubmitAttempted = true
                        adapter.setPending(activity.supportFragmentManager, it)
                    })

        coordinatorBinding.sparkline.setOnClickListener{
            toggleGraphDetails()
        }

        coordinatorBinding.heroShare.setOnClickListener{
            val track = coordinatorBinding.heroImg.tag
            if (track is Track) {
                var shareText: String
                if (username == null)
                    shareText = getString(R.string.share_text,
                            track.artist + " - " + track.name, Stuff.myRelativeTime(context!!, track.playedWhen))
                else
                    shareText = getString(
                        R.string.share_text_username, username,
                            track.artist + " - " + track.name, Stuff.myRelativeTime(context!!, track.playedWhen))
                shareText += "\n\n" + getString(R.string.share_sig)
                val i = Intent(Intent.ACTION_SEND)
                i.type = "text/plain"
                i.putExtra(Intent.EXTRA_SUBJECT, shareText)
                i.putExtra(Intent.EXTRA_TEXT, shareText)
                startActivity(Intent.createChooser(i, getString(R.string.share_this_song)))
            }
        }

        coordinatorBinding.heroInfo.setOnClickListener {
            val t = coordinatorBinding.heroImg.tag
            if (t is Track) {
                val info = InfoFragment()
                val b = Bundle()
                b.putString(NLService.B_ARTIST, t.artist)
                if (!t.album.isNullOrEmpty())
                    b.putString(NLService.B_ALBUM, t.album)
                b.putString(NLService.B_TITLE, t.name)
                b.putString(Stuff.ARG_USERNAME, username)
                info.arguments = b
                info.show(activity.supportFragmentManager, null)
                if (!appPrefs.getBoolean(Stuff.PREF_ACTIVITY_LONG_PRESS_LEARNT, false)) {
                    Stuff.toast(context, getString(R.string.info_long_press_guide), Toast.LENGTH_LONG)
                    appPrefs.edit()
                            .putBoolean(Stuff.PREF_ACTIVITY_LONG_PRESS_LEARNT, true)
                            .apply()
                }
            }
        }

        if (BuildConfig.DEBUG)
            coordinatorBinding.heroInfo.setOnLongClickListener {
                val t = coordinatorBinding.heroImg.tag
                if (t is Track) {
                    Stuff.openInBrowser("https://en.touhouwiki.net/index.php?search=" +
                            URLEncoder.encode("${t.artist} - ${t.name}", "UTF-8"),
                        context
                    )
                }
                true
            }

            coordinatorBinding.heroPlay.setOnClickListener {
            val t =  coordinatorBinding.heroImg.tag
            if (t is Track)
                Stuff.launchSearchIntent(t.artist, t.name, context!!)
        }

        coordinatorBinding.heroCalendar.setOnClickListener { view ->
            view.isEnabled = false
            val time = if (viewModel.toTime > 0)
                viewModel.toTime
            else
                System.currentTimeMillis()
            val startTime = if (username == null)
                appPrefs.getLong(Stuff.PREF_ACTIVITY_SCROBBLING_SINCE, 0)
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
                                    .setValidator(object: CalendarConstraints.DateValidator{
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
                    .build()
            dpd.addOnPositiveButtonClickListener {
                val tzOffset = TimeZone.getDefault().getOffset(System.currentTimeMillis())
                viewModel.toTime = it  - tzOffset + (24 * 60 * 60 - 1) * 1000
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
        if (!Main.isTV) {
            coordinatorBinding.heroInfo.visibility = View.VISIBLE
            coordinatorBinding.heroPlay.visibility = View.VISIBLE
            coordinatorBinding.heroShare.visibility = View.VISIBLE
        }
    }

    private fun setGraph(pointsStr: String?) {
        _binding ?: return
        val frame = coordinatorBinding.sparklineFrame
        if (pointsStr.isNullOrBlank()) {
            frame.visibility = View.INVISIBLE
        } else {
            val points = mutableListOf<Int>()
            pointsStr.split(", ")
                    .forEach {
                        points.add(it.toInt())
                    }
            if (points.isEmpty())
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
    }

    private fun toggleGraphDetails(init: Boolean = false){
        _binding ?: return
        var show = appPrefs.getBoolean(Stuff.PREF_ACTIVITY_GRAPH_DETAILS, true)
        if (!init)
            show = !show

        if (show) {
            coordinatorBinding.sparkline.setPadding(2 * resources.getDimensionPixelSize(R.dimen.graph_margin_details),
                    0, 0,
                    resources.getDimensionPixelSize(R.dimen.graph_margin_details))
            coordinatorBinding.sparklineHorizontalLabel.visibility = View.VISIBLE
            coordinatorBinding.sparklineTickTop.visibility = View.VISIBLE
            coordinatorBinding.sparklineTickBottom.visibility = View.VISIBLE
        } else {
            coordinatorBinding.sparkline.setPadding(0, 0, 0, 0)
            coordinatorBinding.sparklineHorizontalLabel.visibility = View.GONE
            coordinatorBinding.sparklineTickTop.visibility = View.GONE
            coordinatorBinding.sparklineTickBottom.visibility = View.GONE
        }
        appPrefs.edit()
                .putBoolean(Stuff.PREF_ACTIVITY_GRAPH_DETAILS, show)
                .apply()
    }

    private fun loadRecents(page: Int, force: Boolean = false): Boolean {
        _binding ?: return false

        if (page <= viewModel.totalPages) {
            val firstVisible = ((binding.recentsList.layoutManager ?: return false) as LinearLayoutManager)
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

    override fun onStart() {
        super.onStart()
        if(!isShowingLoves)
            context?.registerReceiver(editReceiver, IntentFilter(NLService.iEDITED))
    }

    override fun onStop() {
        refreshHandler.removeCallbacks(timedRefresh)
        try {
            context?.unregisterReceiver(editReceiver)
        } catch (e: IllegalArgumentException) {}

        super.onStop()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (isVisible)
            Stuff.setAppBarHeight(activity!!)
    }

    override fun onSetHero(position: Int, track: Track, fullSize: Boolean) {
        _binding ?: return

        //check diff
        val oldTrack = coordinatorBinding.heroImg.tag as Track?

        //TODO: check
        coordinatorBinding.heroImg.tag = track
        val imgUrl = if(fullSize)
            track.getWebpImageURL(ImageSize.EXTRALARGE)?.replace("300x300", "600x600")
        else
            track.getWebpImageURL(ImageSize.LARGE)

        if (!fullSize &&
                (oldTrack?.artist != track.artist || oldTrack?.album != track.album || oldTrack?.name != track.name)){
            viewModel.loadHero(track.url)
        }

        if (!imgUrl.isNullOrEmpty() && imgUrl == oldTrack?.getWebpImageURL(ImageSize.LARGE)) {
            if ((coordinatorBinding.ctl.contentScrim as? ColorDrawable?)?.color != lastColorMutedBlack)
                setPaletteColors()
            return
        }
        //load img, animate colors
        if (!imgUrl.isNullOrEmpty()) {
            val req = Picasso.get()
                    .load(imgUrl)
                    .error(R.drawable.vd_wave)

            if (fullSize)
                req.placeholder(coordinatorBinding.heroImg.drawable)
            else
                req.noPlaceholder()
            req.into(coordinatorBinding.heroImg, object : Callback {
                override fun onSuccess() {
//                    hero.background = null
                    coordinatorBinding.heroImg.clearColorFilter()
                    setPaletteColors()
                    if (!fullSize)
                        onSetHero(position, track, true)
                }

                override fun onError(e: Exception) {
                    if (!fullSize)
                        onSetHero(position, track, true)
                    Stuff.log("Picasso err: $e")
                }
            })
        } else {
            Picasso.get().cancelRequest(coordinatorBinding.heroImg)
            val color = Stuff.getMatColor(coordinatorBinding.heroImg.context, Stuff.genHashCode(track.artist, track.name).toLong())
            coordinatorBinding.heroImg.setColorFilter(color)
            coordinatorBinding.heroImg.setImageResource(R.drawable.vd_wave)
            setPaletteColors(color)
        }
    }

    private fun setPaletteColors(oneColor: Int? = null){
        _binding ?: return
        val activityBinding = (activity as Main?)?.binding ?: return
        val content = binding.recentsSwipeRefresh

        fun set(palette: Palette?) {
            palette ?: return
            context ?: return

            colorPrimDark = palette.getDominantColor(Color.WHITE)
            if (!Stuff.isDark(colorPrimDark))
                colorPrimDark = palette.getDarkVibrantColor(MaterialColors.getColor(context, R.attr.colorPrimary, null))
            colorLightWhite = palette.getLightMutedColor(ContextCompat.getColor(context!!, android.R.color.primary_text_dark))
            colorMutedDark = palette.getDarkMutedColor(MaterialColors.getColor(context, R.attr.colorPrimary, null))
            colorMutedBlack = palette.getDarkMutedColor(Color.BLACK)

            activityBinding.coordinatorMain.ctl.setContentScrimColor(colorMutedBlack)

            val contentBgFrom = (content.background as ColorDrawable).color
            val contentBgAnimator = ObjectAnimator.ofArgb(content, "backgroundColor", contentBgFrom, colorMutedBlack)
            val navBgAnimator = ObjectAnimator.ofArgb((activity as Main).binding.navView, "backgroundColor", contentBgFrom, colorMutedBlack)
            val shareBgAnimator = ObjectAnimator.ofArgb(coordinatorBinding.heroShare, "colorFilter", lastColorLightWhite, colorLightWhite)
            val calendarColorAnimator = ObjectAnimator.ofArgb(coordinatorBinding.heroCalendar, "colorFilter", lastColorLightWhite, colorLightWhite)
            val infoBgAnimator = ObjectAnimator.ofArgb(coordinatorBinding.heroInfo, "colorFilter", lastColorLightWhite, colorLightWhite)
            val searchBgAnimator = ObjectAnimator.ofArgb(coordinatorBinding.heroPlay, "colorFilter", lastColorLightWhite, colorLightWhite)
            val sparklineTickTopAnimator = ObjectAnimator.ofArgb(coordinatorBinding.sparklineTickTop, "textColor", lastColorLightWhite, colorLightWhite)
            val sparklineTickBottomAnimator = ObjectAnimator.ofArgb(coordinatorBinding.sparklineTickBottom, "textColor", lastColorLightWhite, colorLightWhite)
            val sparklineHorizontalLabel = ObjectAnimator.ofArgb(coordinatorBinding.sparklineHorizontalLabel, "textColor", lastColorLightWhite, colorLightWhite)
            val sparklineAnimator = ObjectAnimator.ofArgb(coordinatorBinding.sparkline, "lineColor", lastColorLightWhite, colorLightWhite)
            val navbarBgAnimator = ValueAnimator.ofArgb(contentBgFrom, colorMutedBlack)
            navbarBgAnimator.addUpdateListener{
                val activity = activity ?: return@addUpdateListener
                activity.window.navigationBarColor = it.animatedValue as Int
            }
            //setNavigationBarColor uses binders and lags
            val animSetList = mutableListOf(contentBgAnimator,
                    calendarColorAnimator, shareBgAnimator,
                    searchBgAnimator, infoBgAnimator,
                    navbarBgAnimator, sparklineAnimator, sparklineHorizontalLabel,
                    sparklineTickBottomAnimator, sparklineTickTopAnimator)
            if (coordinatorBinding.coordinator.paddingStart > 0)
                animSetList.add(navBgAnimator)

            for (i in 0..coordinatorBinding.toolbar.childCount){
                val child = coordinatorBinding.toolbar.getChildAt(i)
                if (child is ImageButton){

                    val bgColor = if (colorPrimDark != colorLightWhite)
                        colorPrimDark
                    else
                        colorMutedDark

                    (child.drawable as ShadowDrawerArrowDrawable).setColors(colorLightWhite, bgColor)
                    break
                }
            }
            animSet.cancel()
            animSet.playTogether(animSetList.toList())
            animSet.interpolator = AccelerateDecelerateInterpolator()
            animSet.duration = 1500
            animSet.start()
        }

        val d = coordinatorBinding.heroImg.drawable
        if (oneColor != null) {
            val swatch = Palette.Swatch(oneColor, 1)
            val palette = Palette.from(listOf(swatch))
            set(palette)
        } else if (d is BitmapDrawable && d.bitmap != null)
            Palette.from(d.bitmap).generate{ set(it) }
    }

    override fun onItemClick(view: View, position: Int) {
        val item = adapter.getItem(position) ?: return
        val dateFrame = (view.parent as ViewGroup).findViewById<FrameLayout>(R.id.date_frame)
        if (item !is Track) {
            if (view.id == R.id.recents_menu)
                PendingMenu.openPendingPopupMenu(dateFrame, item,
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
                        smoothScroller = object : LinearSmoothScroller(binding.recentsList.context) {
                            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics?) =
                                    super.calculateSpeedPerPixel(displayMetrics) * 3

                            override fun getVerticalSnapPreference() = SNAP_TO_START
                        }
                    smoothScroller?.targetPosition = position
                    binding.recentsList.layoutManager?.startSmoothScroll(smoothScroller)
                    coordinatorBinding.appBar.setExpanded(true, true)
                }

                if (!view.isInTouchMode)
                    openTrackPopupMenu(dateFrame, item)
            }
        }
    }

    override fun onItemLongClick(view: View, position: Int) {
        val t = adapter.getItem(position) as? Track ?: return
        val info = InfoFragment()
        val b = Bundle()
        b.putString(NLService.B_ARTIST, t.artist)
        if (!t.album.isNullOrEmpty())
            b.putString(NLService.B_ALBUM, t.album)
        b.putString(NLService.B_TITLE, t.name)
        b.putString(Stuff.ARG_USERNAME, username)
        info.arguments = b
        info.show(activity!!.supportFragmentManager, null)
    }

    //only called when !view.isInTouchMode
    override fun onFocus(view: View, position: Int) {
        if (!view.isInTouchMode) {
            val pos = IntArray(2)
            view.getLocationInWindow(pos)

            if (pos[1] + view.height > coordinatorBinding.coordinator.height && coordinatorBinding.appBar.isExpanded)
                coordinatorBinding.appBar.setExpanded(false, true)
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
                loveMenu.icon = ContextCompat.getDrawable(context!!, R.drawable.vd_heart_break_outline)
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
        fun csrfTokenExists(): Boolean {
            val prefs = MultiPreferences(context!!)
            val exists = LastfmUnscrobbler(context)
                    .checkCsrf(prefs.getString(Stuff.PREF_LASTFM_USERNAME, null))
            if (!exists) {
                MaterialAlertDialogBuilder(context!!)
                        .setMessage(R.string.lastfm_reauth)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            val wf = WebViewFragment()
                            val b = Bundle()
                            b.putString(Stuff.ARG_URL, Stuff.LASTFM_AUTH_CB_URL)
                            b.putBoolean(Stuff.ARG_SAVE_COOKIES, true)
                            wf.arguments = b
                            activity!!.supportFragmentManager.beginTransaction()
                                    .replace(R.id.frame, wf)
                                    .addToBackStack(null)
                                    .commit()
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
            }
            return exists
        }
        menuBuilder.setCallback(object : MenuBuilder.Callback {
            override fun onMenuItemSelected(menu: MenuBuilder, item: MenuItem): Boolean {
                when (item.itemId) {
                    R.id.menu_love -> {
                        loveToggle((anchor.parent as ViewGroup).findViewById(R.id.recents_img_overlay), track)
                    }
                    R.id.menu_edit -> {
                        if (!Main.isOnline)
                            Stuff.toast(context, getString(R.string.unavailable_offline))
                        else if (csrfTokenExists()) {
                            val b = Bundle()
                            b.putString(NLService.B_ARTIST, track.artist)
                            b.putString(NLService.B_ALBUM, track.album)
                            b.putString(NLService.B_TITLE, track.name)

                            val millis = track.playedWhen?.time
                            if (millis != null)
                                b.putLong(NLService.B_TIME, millis)

                            val ef = EditFragment()
                            ef.arguments = b
                            ef.show(activity!!.supportFragmentManager, null)
                        }
                    }
                    R.id.menu_delete -> {
                        if (!Main.isOnline)
                            Stuff.toast(context, getString(R.string.unavailable_offline))
                        else if (csrfTokenExists()) {
                            LFMRequester(context!!).delete(track) { succ ->
                                if (succ) {
                                    view?.post {
                                        adapter.removeTrack(track)
                                    }
                                } else
                                    Stuff.toast(context, getString(R.string.network_error))
                            }
                                    .asAsyncTask()
                        }
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
            LFMRequester(context!!).loveOrUnlove(false, track.artist, track.name).asAsyncTask()

            alphaAnimator.setFloatValues(0f)
            scalexAnimator.setFloatValues(1f, 2f)
            scaleyAnimator.setFloatValues(1f, 2f)
            val startRot = if (isRtl) -10f else 10f
            val toRot = if (isRtl) 50f else -50f
            rotAnimator.setFloatValues(startRot, toRot)
        } else {
            if (loveIcon.background == null)
                loveIcon.background = ContextCompat.getDrawable(context!!, R.drawable.vd_heart_stroked)

            LFMRequester(context!!).loveOrUnlove(true, track.artist, track.name).asAsyncTask()

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

    companion object {
        private var lastColorLightWhite = Color.WHITE
        private var lastColorMutedDark = Color.BLACK
        var lastColorMutedBlack = Color.BLACK
    }
}
