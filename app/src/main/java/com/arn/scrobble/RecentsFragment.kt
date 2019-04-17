package com.arn.scrobble

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.*
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageButton
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.*
import androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
import com.arn.scrobble.pref.MultiPreferences
import com.arn.scrobble.ui.EndlessRecyclerViewScrollListener
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.ui.ShadowDrawerArrowDrawable
import com.arn.scrobble.ui.SimpleHeaderDecoration
import com.robinhood.spark.SparkView
import com.robinhood.spark.animation.MorphSparkAnimator
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import de.umass.lastfm.Track
import kotlinx.android.synthetic.main.content_recents.*
import kotlinx.android.synthetic.main.coordinator_main.*
import kotlinx.android.synthetic.main.coordinator_main.view.*
import kotlinx.android.synthetic.main.list_item_recents.view.*


/**
 * Created by arn on 09/07/2017.
 */

class RecentsFragment : Fragment(), ItemClickListener, RecentsAdapter.SetHeroTrigger {
    private lateinit var adapter: RecentsAdapter
    private lateinit var appPrefs: SharedPreferences
    private var firstLoadCache = true
    private var firstLoadNw = true
    private var timedRefresh = Runnable { loadRecents(1) }
    private var refreshHandler = Handler()
    private lateinit var viewModel: TracksVM
    private lateinit var animSet: AnimatorSet
    private var smoothScroller: LinearSmoothScroller? = null

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
        return inflater.inflate(R.layout.content_recents, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (userVisibleHint)
            Stuff.setAppBarHeight(activity!!)

        val llm = LinearLayoutManager(context!!)
        recents_list.layoutManager = llm

        adapter = RecentsAdapter(view)
        adapter.setStatusHeader(getString(R.string.recently_scrobbled))

        animSet = AnimatorSet()
        animSet.addListener(object : Animator.AnimatorListener{
            override fun onAnimationRepeat(p0: Animator?) {}

            override fun onAnimationEnd(p0: Animator?) {
                lastColorVibrantDark = colorPrimDark
                lastColorLightWhite = colorLightWhite
                lastColorMutedDark = colorMutedDark
                lastColorMutedBlack = colorMutedBlack
            }

            override fun onAnimationCancel(p0: Animator?) {}

            override fun onAnimationStart(p0: Animator?) {}

        })

        val itemDecor = DividerItemDecoration(context!!, VERTICAL)
        itemDecor.setDrawable(ColorDrawable(ContextCompat.getColor(context!!, R.color.lightDivider)))
        recents_list.addItemDecoration(itemDecor)
        recents_list.addItemDecoration(SimpleHeaderDecoration(0, Stuff.dp2px(25, context!!)))
        Stuff.setProgressCircleColor(recents_swipe_refresh)
        recents_swipe_refresh.setOnRefreshListener { loadRecents(1) }
        recents_swipe_refresh.isRefreshing = false
        recents_list.adapter = adapter
        (recents_list.itemAnimator as DefaultItemAnimator?)?.supportsChangeAnimations = false

        val loadMoreListener = object : EndlessRecyclerViewScrollListener(llm) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView) {
                loadRecents(page)
            }
        }
        loadMoreListener.loading = false

        viewModel = VMFactory.getVM(this, TracksVM::class.java)
        viewModel.loadRecentsList(1, false)
                .observe(viewLifecycleOwner, Observer {
                        loadMoreListener.loading = false
                        it ?: return@Observer
                        adapter.populate(it, it.page, !firstLoadNw)

                        if (!firstLoadCache && firstLoadNw)
                            firstLoadNw = false

                        if (firstLoadCache) {
                            firstLoadCache = false
                            loadRecents(1)
                            toggleGraphDetails(activity!!.sparkline, true)
                        } else if (it.page == 1){
                            viewModel.loadPending(2)
                            refreshHandler.postDelayed(timedRefresh, Stuff.RECENTS_REFRESH_INTERVAL)
                        }
            })
        viewModel.loadHero(null)
                .observe(viewLifecycleOwner, Observer {
                        it ?: return@Observer
                        setGraph(it[0])
            })

        viewModel.loadPending(2)
            .observe(viewLifecycleOwner, Observer {
                    it ?: return@Observer
                    adapter.setPendingScrobbles(activity!!.supportFragmentManager, it.first, it.second)
            })

        recents_list.addOnScrollListener(loadMoreListener)
        adapter.setLoadMoreReference(loadMoreListener)
        adapter.setClickListener(this)
        adapter.setHeroListener(this)

        appPrefs = context!!.getSharedPreferences(Stuff.ACTIVITY_PREFS, Context.MODE_PRIVATE)

        activity!!.hero_share.setOnClickListener(shareClickListener)

        val sparkline = activity!!.sparkline
        if (sparkline.adapter == null) { // not inited
            sparkline.sparkAnimator = MorphSparkAnimator()
            sparkline.adapter = SparkLineAdapter()
        }
        sparkline.setOnClickListener{
            toggleGraphDetails(it as SparkView)
        }

        activity!!.hero_info.setOnClickListener { v:View ->
            val t = activity?.hero_img?.tag
            if (t is Track)
                Stuff.openInBrowser(t.url, activity, null)
        }
        activity!!.hero_play.setOnClickListener { v:View ->
            val t =  activity?.hero_img?.tag
            if (t is Track)
                Stuff.launchSearchIntent(t.artist, t.name, context!!)
        }
        activity!!.hero_similar.setOnClickListener { v:View ->
            v.isEnabled = false
            val t = activity?.hero_img?.tag
            if (t is Track) {
                val simFragment = SimilarTracksFragment()
                val b = Bundle()
                b.putString("artist", t.artist)
                b.putString("track", t.name)
                simFragment.arguments = b

                activity!!.supportFragmentManager.beginTransaction()
                        .hide(this)
                        .add(R.id.frame, simFragment, Stuff.TAG_SIMILAR)
                        .addToBackStack(null)
                        .commit()
            }
        }
    }

    private fun setGraph(pointsStr: String?) {
        val frame = activity?.sparkline_frame  ?: return
        if (pointsStr.isNullOrBlank()) {
            frame.visibility = View.INVISIBLE
        } else {
            val points = arrayListOf<Float>()
            pointsStr!!.split(", ")
                    .forEach {
                        points.add(it.toFloat())
                    }
            if (points.isEmpty())
                frame.visibility = View.INVISIBLE
            else {
                frame.visibility = View.VISIBLE
                val sparklineAdapter = activity!!.sparkline.adapter as SparkLineAdapter
                sparklineAdapter.setData(points)
                val max = sparklineAdapter.max()
                val min = sparklineAdapter.min()
                activity!!.sparkline_tick_top.text = Stuff.humanReadableNum(max)

                if (max != min)
                    activity!!.sparkline_tick_bottom.text = Stuff.humanReadableNum(min)
                else
                    activity!!.sparkline_tick_bottom.text = ""
            }

        }
    }

    private fun toggleGraphDetails(sparkline: SparkView, init: Boolean = false){
        val activity = activity!!
        var show = appPrefs.getBoolean(Stuff.PREF_ACTIVITY_GRAPH_DETAILS, true)
        if (!init)
            show = !show

        if (show) {
            sparkline.setPadding(2*resources.getDimensionPixelSize(R.dimen.graph_margin_details),
                    0,0,
                    resources.getDimensionPixelSize(R.dimen.graph_margin_details))
            activity.sparkline_horizontal_label.visibility = View.VISIBLE
            activity.sparkline_tick_top.visibility = View.VISIBLE
            activity.sparkline_tick_bottom.visibility = View.VISIBLE
        } else {
            sparkline.setPadding(0,0,0,0)
            activity.sparkline_horizontal_label.visibility = View.GONE
            activity.sparkline_tick_top.visibility = View.GONE
            activity.sparkline_tick_bottom.visibility = View.GONE
        }
        appPrefs.edit()
                .putBoolean(Stuff.PREF_ACTIVITY_GRAPH_DETAILS, show)
                .apply()
    }

    private fun loadRecents(page: Int): Boolean {
        Stuff.log("loadRecents $page")
        recents_list ?: return false

        if (page <= adapter.totalPages) {
            val firstVisible = (recents_list.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            if ((page == 1 && firstVisible < 5) || page > 1) {
                viewModel.loadRecentsList(page, true)
            }
            if (adapter.itemCount == 0 || page > 1)
                adapter.setLoading(true)
        } else {
            adapter.setLoading(false)
            return false
        }
        return true
    }

    override fun setUserVisibleHint(visible: Boolean) {
        super.setUserVisibleHint(visible)
        val activity = activity
        if (activity != null && visible/* && isResumed*/) {
            activity.ctl.setContentScrimColor(lastColorVibrantDark)
            activity.toolbar.title = " "
            Stuff.setAppBarHeight(activity)

            /*
            if (Stuff.isDark(RecentsAdapter.lastColorDomPrimary))
                activity.ctl.setCollapsedTitleTextColor(RecentsAdapter.lastColorLightWhite)
            else
                activity.ctl.setCollapsedTitleTextColor(RecentsAdapter.lastColorMutedDark)
            */
        }
    }

    override fun onStart() {
        super.onStart()
        context?.registerReceiver(editReceiver, IntentFilter(NLService.iEDITED))
    }

    override fun onStop() {
        refreshHandler.removeCallbacks(timedRefresh)
        try {
            context?.unregisterReceiver(editReceiver)
        } catch (e: IllegalArgumentException) {}

        super.onStop()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        if (userVisibleHint)
            Stuff.setAppBarHeight(activity!!)
    }

//    private fun setHero(imgUrl: String?) {
//        onSetHero(-1, null, imgUrl)
//    }

    override fun onSetHero(position: Int, track: Track, fullSize: Boolean/*, imgUrl: String?*/) {
        val ctl = activity?.ctl ?: return
        val hero = ctl.hero_img

        //check diff
        val oldTrack = hero.tag as Track?

        ctl.hero_title.text = track.name
        hero.tag = track
        val imgUrl = Stuff.getAlbumOrArtistImg(track, fullSize)

        if (!fullSize && oldTrack?.name != track.name){
            viewModel.loadHero(track.url)
        }

        if (imgUrl != "" && imgUrl == Stuff.getAlbumOrArtistImg(oldTrack, false))
            return

        //load img, animate colors
        if (!imgUrl.isNullOrEmpty()) {
            val req = Picasso.get()
                    .load(imgUrl)
                    .error(R.drawable.vd_wave)
                    .fit()
                    .centerCrop()

            if (fullSize)
                req.placeholder(hero.drawable)
            else
                req.noPlaceholder()
            req.into(hero, object : Callback {
                override fun onSuccess() {
//                    hero.background = null
                    hero.clearColorFilter()
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
            val color = Stuff.getMatColor(hero.context, "500", track.name.hashCode().toLong())
            hero.setColorFilter(color)
            hero.setImageResource(R.drawable.vd_wave)
            setPaletteColors(color)
        }
    }

    private fun setPaletteColors(oneColor: Int? = null){
        val activity = activity ?: return
        val ctl = activity.ctl
        val content = activity.recents_swipe_refresh ?: return

        fun set(palette: Palette?) {
            palette ?: return
            context ?: return

            colorPrimDark = palette.getDominantColor(Color.WHITE)
            if (!Stuff.isDark(colorPrimDark))
                colorPrimDark = palette.getDarkVibrantColor(ContextCompat.getColor(context!!, R.color.colorPrimary))
            colorLightWhite = palette.getLightMutedColor(ContextCompat.getColor(context!!, android.R.color.primary_text_dark))
            colorMutedDark = palette.getDarkMutedColor(ContextCompat.getColor(context!!, R.color.colorPrimaryDark))
            colorMutedBlack = palette.getDarkMutedColor(ContextCompat.getColor(context!!, android.R.color.background_dark))

            ctl.setContentScrimColor(colorPrimDark)
            ctl.hero_title.setTextColor(colorLightWhite)
/*
            if (Stuff.isDark(colorDomPrimary))
                ctl.setCollapsedTitleTextColor(colorLightWhite)
            else
                ctl.setCollapsedTitleTextColor(colorMutedDark)
*/

            val contentBgFrom = (content.background as ColorDrawable).color
            val contentBgAnimator = ObjectAnimator.ofArgb(content, "backgroundColor", contentBgFrom, colorMutedBlack)
            val shareBgAnimator = ObjectAnimator.ofArgb(activity.hero_share, "colorFilter", lastColorLightWhite, colorLightWhite)
            val similarColorAnimator = ObjectAnimator.ofArgb(activity.hero_similar, "colorFilter", lastColorLightWhite, colorLightWhite)
            val infoBgAnimator = ObjectAnimator.ofArgb(activity.hero_info, "colorFilter", lastColorLightWhite, colorLightWhite)
            val searchBgAnimator = ObjectAnimator.ofArgb(activity.hero_play, "colorFilter", lastColorLightWhite, colorLightWhite)
            val sparklineTickTopAnimator = ObjectAnimator.ofArgb(activity.sparkline_tick_top, "textColor", lastColorLightWhite, colorLightWhite)
            val sparklineTickBottomAnimator = ObjectAnimator.ofArgb(activity.sparkline_tick_bottom, "textColor", lastColorLightWhite, colorLightWhite)
            val sparklineHorizontalLabel = ObjectAnimator.ofArgb(activity.sparkline_horizontal_label, "textColor", lastColorLightWhite, colorLightWhite)
            val sparklineAnimator = ObjectAnimator.ofArgb(activity.sparkline, "lineColor", lastColorLightWhite, colorLightWhite)
            val navbarBgAnimator = ValueAnimator.ofArgb(contentBgFrom, colorMutedBlack)
            navbarBgAnimator.addUpdateListener{
                activity.window.navigationBarColor = it.animatedValue as Int
            }

            val animSetList = mutableListOf(contentBgAnimator,
                    similarColorAnimator, shareBgAnimator, searchBgAnimator, infoBgAnimator,
                    navbarBgAnimator, sparklineAnimator, sparklineHorizontalLabel, sparklineTickBottomAnimator, sparklineTickTopAnimator)

            for (i in 0..ctl.toolbar.childCount){
                val child = ctl.toolbar.getChildAt(i)
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



        val d = ctl.hero_img.drawable
        if (oneColor != null) {
            val swatch = Palette.Swatch(oneColor, 1)
            val palette = Palette.from(listOf(swatch))
            set(palette)
        } else if (d is BitmapDrawable && d.bitmap != null)
            Palette.from(d.bitmap).generate{ set(it) }
    }

    override fun onItemClick (view: View, position: Int) {
        val track = adapter.getTrack(position) ?: return
        when {
            view.id == R.id.recents_menu -> openPopupMenu((view.parent as ViewGroup).recents_date, track)
            view.id == R.id.recents_img_overlay -> {
                loveToggle(view, track)
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            }
            else -> {
                val lastClickedPos = adapter.selectedPos
                if (lastClickedPos != position) {
                    adapter.selectedPos = position
                    val lastHolder = recents_list.findViewHolderForAdapterPosition(lastClickedPos)
                    val curHolder = recents_list.findViewHolderForAdapterPosition(position)
                    if (lastHolder is RecentsAdapter.VHTrack? && curHolder is RecentsAdapter.VHTrack){
                        lastHolder?.setSelected(false, track)
                        curHolder.setSelected(true, track)
                    }

                    val activity = activity!!
                    if (!activity.app_bar.isExpanded) {
//                  recents_list.smoothScrollToPosition(position) //wont work even witrh smoothscroller

                        if (smoothScroller == null)
                            smoothScroller = object : LinearSmoothScroller(recents_list.context) {
                                override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics?) =
                                        super.calculateSpeedPerPixel(displayMetrics) * 3

                                override fun getVerticalSnapPreference() = LinearSmoothScroller.SNAP_TO_START
                            }
                        smoothScroller?.targetPosition = position
                        recents_list.layoutManager?.startSmoothScroll(smoothScroller)
                    }
                    activity.app_bar?.setExpanded(true, true)
                }
            }
        }
    }

    private fun openPopupMenu (anchor: View, track: Track) {
        val popup = PopupMenu(context, anchor)

        popup.inflate(R.menu.recents_item_menu)
        val loveMenu = popup.menu.findItem(R.id.menu_love)

        if (track.isLoved)
            loveMenu.title = getString(R.string.unlove)
        loveMenu.icon?.setTint(Color.WHITE)

        if (track.playedWhen == null)
            popup.menu.removeItem(R.id.menu_delete)

        fun csrfTokenExists(): Boolean {
            val prefs = MultiPreferences(context!!)
            val exists = LastfmUnscrobbler(context)
                    .checkCsrf(prefs.getString(Stuff.PREF_LASTFM_USERNAME, null)!!)
            return if (!exists) {
                val b = Bundle()
                b.putString(LoginFragment.HEADING, getString(R.string.lastfm))
                b.putString(LoginFragment.INFO, getString(R.string.lastfm_login_info))
                b.putString(LoginFragment.TEXTFL, getString(R.string.lastfm_password))

                val lf = LoginFragment()
                lf.arguments = b
                (activity as Main)
                        .supportFragmentManager!!.beginTransaction()
                        .replace(R.id.frame, lf)
                        .addToBackStack(null)
                        .commit()
                false
            } else {
                true
            }
        }

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_love -> {
                    loveToggle((anchor.parent as ViewGroup).recents_img_overlay, track)
                    true
                }
                R.id.menu_edit -> {
                    if (!Main.isOnline)
                        Stuff.toast(context!!, getString(R.string.unavailable_offline))
                    else if (csrfTokenExists()){
                        val b = Bundle()
                        b.putString(NLService.B_ARTIST, track.artist)
                        b.putString(NLService.B_ALBUM, track.album)
                        b.putString(NLService.B_TITLE, track.name)

                        val millis = track.playedWhen?.time
                        if (millis != null)
                            b.putLong(NLService.B_TIME, millis)

                        val ef = EditFragment()
                        ef.arguments = b
/*
                        (activity as Main)
                                .supportFragmentManager!!.beginTransaction()
                                .replace(R.id.frame, ef)
                                .addToBackStack(null)
                                .commit()
                                */
                        ef.show((activity as Main)
                                .supportFragmentManager, null)
                    }
                    true
                }
                R.id.menu_delete -> {
                    if (!Main.isOnline)
                        Stuff.toast(context!!, getString(R.string.unavailable_offline))
                    else if (csrfTokenExists()){
                        LFMRequester(Stuff.DELETE, track.artist, track.album, track.name,
                                track.playedWhen.time.toString(), track.duration.toString())
                                .addCallback { succ ->
                                    if (succ){
                                        view?.post {
                                            adapter.removeTrack(track)
                                        }
                                    } else
                                        Stuff.toast(context, getString(R.string.network_error))
                                }
                                .asAsyncTask(context!!)
                    }
                    true
                }

                else -> false
            }
        }
        popup.show()
    }

    private fun loveToggle(loveIcon:View, track: Track) {
        val alphaAnimator = ObjectAnimator.ofFloat(loveIcon, "alpha", 0f)
        val scalexAnimator = ObjectAnimator.ofFloat(loveIcon, "scaleX", 0f)
        val scaleyAnimator = ObjectAnimator.ofFloat(loveIcon, "scaleY", 0f)
        val rotAnimator = ObjectAnimator.ofFloat(loveIcon, "rotation", 0f)


        if (track.isLoved) {
            LFMRequester(Stuff.UNLOVE, track.artist, track.name).asAsyncTask(context!!)

            alphaAnimator.setFloatValues(0f)
            scalexAnimator.setFloatValues(1f, 0.5f)
            scaleyAnimator.setFloatValues(1f, 0.5f)
            rotAnimator.setFloatValues(10f, 60f)
        } else {
            if (loveIcon.background == null)
                loveIcon.background = context!!.getDrawable(R.drawable.vd_heart_solid)

            LFMRequester(Stuff.LOVE, track.artist, track.name).asAsyncTask(context!!)

            loveIcon.alpha = 0f
            loveIcon.visibility = View.VISIBLE
            alphaAnimator.setFloatValues(0.9f)
            scalexAnimator.setFloatValues(2f, 1f)
            scaleyAnimator.setFloatValues(2f, 1f)
            rotAnimator.setFloatValues(-50f, 10f)
        }
        val aSet = AnimatorSet()
        aSet.playTogether(alphaAnimator, scalexAnimator, scaleyAnimator, rotAnimator)
        aSet.interpolator = OvershootInterpolator()
        aSet.duration = 800
        aSet.start()

        track.isLoved = !track.isLoved
    }


    private val shareClickListener = View.OnClickListener {
        val track = activity!!.hero_img?.tag
        if (track is Track) {

            var shareText = getString(R.string.share_text,
                    track.artist + " - " + track.name, Stuff.myRelativeTime(context!!, track.playedWhen))
            shareText += "\n" +appPrefs.getString(Stuff.PREF_ACTIVITY_SHARE_SIG,
                    getString(R.string.share_sig, getString(R.string.share_link)))
            val i = Intent(Intent.ACTION_SEND)
            i.type = "text/plain"
            i.putExtra(Intent.EXTRA_SUBJECT, shareText)
            i.putExtra(Intent.EXTRA_TEXT, shareText)
            startActivity(Intent.createChooser(i, getString(R.string.share_this_song)))
        }
    }

    companion object {
        private var lastColorVibrantDark: Int = Color.rgb(0x88, 0x0e, 0x4f)
        private var lastColorLightWhite = Color.WHITE
        private var lastColorMutedDark = Color.BLACK
        var lastColorMutedBlack = Color.BLACK
    }
}
