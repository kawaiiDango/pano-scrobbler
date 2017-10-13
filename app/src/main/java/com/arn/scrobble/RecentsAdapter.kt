@file:Suppress("DEPRECATION")

package com.arn.scrobble

import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.*
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Message
import android.support.design.widget.CollapsingToolbarLayout
import android.support.graphics.drawable.Animatable2Compat
import android.support.graphics.drawable.AnimatedVectorDrawableCompat
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.graphics.Palette
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.*
import com.arn.scrobble.db.PendingScrobble
import com.arn.scrobble.db.PendingScrobblesDb
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import de.umass.lastfm.ImageSize
import de.umass.lastfm.PaginatedResult
import de.umass.lastfm.Track
import java.io.UnsupportedEncodingException
import java.net.URLEncoder

/**
 * Created by arn on 10/07/2017.
 */

class RecentsAdapter

(c: Context, private val layoutResourceId: Int) : ArrayAdapter<Track>(c, layoutResourceId, mutableListOf()) {

    private val hero = (c as Activity).findViewById<ImageView>(R.id.img_hero)
    private var heroInfoLoader: LFMRequester? = null
    private val handler = ResponseHandler(this)
    val timedRefresh = Runnable { loadRecents(1) }

    init {
        setNotifyOnChange(false)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView : View? = convertView
        /*
         * The convertView argument is essentially a "ScrapView" as described is Lucas post
         * http://lucasr.org/2012/04/05/performance-tips-for-androids-listview/
         * It will have a non-null value when ListView is asking you recycle the row layout.
         * So, when convertView is not null, you should simply update its contents instead of inflating a new row layout.
         */
        if (convertView == null) {
            // inflate the layout
            val inflater = (context as Activity).layoutInflater
            convertView = inflater.inflate(layoutResourceId, parent, false)!!
        }
        // object item based on the position
        val t = getItem(position) ?: return convertView
        parent as ListView
        val selectedId: Long = if (parent.checkedItemIds.isEmpty()) NP_ID else parent.checkedItemIds[0]

// get the TextView and then set the text (item name) and tag (item ID) values
        val title = convertView.findViewById<TextView>(R.id.recents_title)
        val subtitle = convertView.findViewById<TextView>(R.id.recents_subtitle)
        val date = convertView.findViewById<TextView>(R.id.recents_date)
        val np = convertView.findViewById<ImageView>(R.id.recents_playing)
        var relDate: CharSequence = ""

        if (t.isNowPlaying) {
            relDate = "   now"
            np.visibility = View.VISIBLE
            val anim = np.drawable
            if (anim is AnimatedVectorDrawableCompat && !anim.isRunning) {
                anim.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                    override fun onAnimationEnd(drawable: Drawable?) {
                        drawable as AnimatedVectorDrawableCompat?
                        drawable?.unregisterAnimationCallback(this)
                        if (drawable != null && drawable.isVisible) {
                            val newAnim = AnimatedVectorDrawableCompat.create(context, R.drawable.avd_eq)
                            np.setImageDrawable(newAnim)
                            newAnim?.start()
                            newAnim?.registerAnimationCallback(this)
                        }
                    }
                })
                anim.start()
            } else if (Build.VERSION.SDK_INT >= 23 && anim is AnimatedVectorDrawable && !anim.isRunning) {
                anim.registerAnimationCallback(object : Animatable2.AnimationCallback() {
                    override fun onAnimationEnd(drawable: Drawable?) {
                        if (drawable != null && drawable.isVisible)
                            (drawable as AnimatedVectorDrawable).start()
                    }
                })
                anim.start()
            }
        } else
            np.visibility = View.GONE
        title.text = t.name
        subtitle.text = t.artist

        if (t.playedWhen != null) {
            relDate = Stuff.myRelativeTime(t.playedWhen)
        }
        date.text = relDate
        val love = convertView.findViewById<ImageView>(R.id.recents_love)
        val play = convertView.findViewById<ImageView>(R.id.recents_play)

        love.setOnClickListener(loveToggle)

        if (t.isLoved) {
            love.setImageDrawable(AnimatedVectorDrawableCompat.create(context, R.drawable.avd_heart_break))
            love.setTag(R.id.recents_love, FILLED)
        } else {
            love.setImageDrawable(AnimatedVectorDrawableCompat.create(context, R.drawable.avd_heart_make))
            love.setTag(R.id.recents_love, 0)
        }

        val albumArt = convertView.findViewById<ImageView>(R.id.recents_album_art)

        val imgUrl = t.getImageURL(ImageSize.MEDIUM)

        if (imgUrl != null && imgUrl != "") {
            albumArt.clearColorFilter()
            Picasso.with(context)
                    .load(imgUrl)
                    .fit()
                    .centerInside()
//                    .placeholder(R.drawable.ic_lastfm)
//                    .error(R.drawable.ic_placeholder_music)
                    .into(albumArt)

        } else {
            albumArt.setImageResource(R.drawable.ic_placeholder_music)
            albumArt.setColorFilter(Stuff.getMatColor(context, "500", t.name.hashCode().toLong()))
        }

        if ( getItemId(position) == selectedId && t.url != hero.tag) {
            val heroInfo = (context as Activity).findViewById<ImageButton>(R.id.hero_info)
            val heroYt = (context as Activity).findViewById<ImageButton>(R.id.hero_yt)

            heroInfo.setOnClickListener {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(t.url))
                context.startActivity(browserIntent)
            }
            heroYt.setOnClickListener {
                var ytUrl = "https://www.youtube.com/results?search_query="
                try {
                    ytUrl += URLEncoder.encode(t.artist + " - " + t.name, "UTF-8")
                } catch (e: UnsupportedEncodingException) {
                    Stuff.toast(context, "failed to encode url")
                }
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(ytUrl))
                context.startActivity(browserIntent)
            }
            heroInfoLoader?.cancel(true)
            heroInfoLoader = LFMRequester(context, handler)
            heroInfoLoader?.execute(Stuff.HERO_INFO, t.url, t.getImageURL(ImageSize.EXTRALARGE))

            hero.tag = t.url
            setHero(t, imgUrl) //better set a blurred one

            play.visibility = View.VISIBLE
            play.setOnClickListener({heroYt.callOnClick()})
        } else if (getItemId(position) != selectedId) {
            play.visibility = View.INVISIBLE
        } else
            play.visibility = View.VISIBLE

        return convertView
    }

    override fun getItemId(position: Int): Long {
        return getItem(position)?.playedWhen?.time ?: NP_ID
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    fun loadRecents(page: Int) {
        val list = (context as Activity).findViewById<ListView?>(R.id.recents_list) ?: return

        Stuff.log("loadRecents $page")
        LFMRequester(context, handler).execute(Stuff.GET_RECENTS, page.toString())
        list.findViewById<View>(R.id.recents_progressbar).visibility = View.VISIBLE
    }
    fun firstLoad() {
        Stuff.log("firstLoad")
        LFMRequester(context, handler).execute(Stuff.GET_RECENTS_CACHED, (1).toString())
    }

    fun loadPending() {
        Thread{
            val dao = PendingScrobblesDb.getDb(context).getDao()
            val count = dao.count
            val limit = 2
            var aFew = listOf<PendingScrobble>()

            if (count > 0) {
                aFew = dao.all(limit)
                OfflineScrobbleJob.checkAndSchedule(context, true)
            }

            (context as Activity).runOnUiThread {
                Stuff.log("loadPending")
                val list = (context as Activity).findViewById<ListView?>(R.id.recents_list) ?: return@runOnUiThread
                val layout = list.findViewById<LinearLayout>(R.id.header_pending)
                if (count > 0) {
                    val pendingItems = layout.findViewById<LinearLayout>(R.id.header_pending_items)
                    pendingItems.removeAllViews()
                    val inflater = LayoutInflater.from(context)
                    aFew.forEach {
                        val v = inflater.inflate(R.layout.list_item_recents, pendingItems, false)
                        v.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        pendingItems.addView(PendingScroblesAdapter.populateItem(v, it))
                    }
                    val pendingSummary = layout.findViewById<LinearLayout>(R.id.pending_summary)
                    val diff = count - aFew.size
                    if (diff > 0) {
                        pendingSummary.findViewById<TextView>(R.id.pending_more).text = context.getString(R.string.pending_scrobbles_summary, diff)
                        pendingSummary.setOnClickListener {
                            val fm = (context as Activity).fragmentManager
                                    fm.beginTransaction()
                                    .hide(fm.findFragmentByTag(Stuff.GET_RECENTS))
                                    .add(R.id.frame, PendingFragment())
                                    .addToBackStack(null)
                                    .commit()
                        }
                        pendingSummary.visibility = View.VISIBLE
                    } else
                        pendingSummary.visibility = View.GONE
                    layout.visibility = View.VISIBLE
                } else
                    layout.visibility = View.GONE
            }

        }.start()
    }

    fun setHero(imgUrl: String?) {
        setHero(null, imgUrl)
    }

    private fun setHero(t: Track?, imgUrl: String? = null) {
        val ctl = (context as Activity).findViewById<CollapsingToolbarLayout>(R.id.toolbar_layout)

        if (t != null) {
            val text = t.artist + " - " + t.name
//            if (Main.heroExpanded)
                ctl.title = text
            ctl.tag = text
        }

        if (imgUrl != null && imgUrl != "") {
            Picasso.with(context)
                    .load(imgUrl)
                    .noPlaceholder()
                    .error(R.drawable.ic_placeholder_music)
                    .fit()
                    .centerCrop()
                    .into(hero, object : Callback {
                        override fun onSuccess() {
                            hero.clearColorFilter()
                            setPaletteColors()
                        }

                        override fun onError() {
                            Stuff.log("Picasso onError")
                        }
                    })
        } else {
            if (t!= null)
                hero.setColorFilter(Stuff.getMatColor(context, "500", t.name.hashCode().toLong()))
            hero.setImageResource(R.drawable.ic_placeholder_music)
            hero.setBackgroundResource(android.R.color.black)
            setPaletteColors()

        }
    }

    private fun setPaletteColors(){
        val ctl = (context as Activity).findViewById<CollapsingToolbarLayout>(R.id.toolbar_layout) ?: return
        val list = (context as Activity).findViewById<ListView?>(R.id.recents_list) ?: return
        val heroShare = (context as Activity).findViewById<ImageButton>(R.id.hero_share)
        val heroInfo = (context as Activity).findViewById<ImageButton>(R.id.hero_info)
        val heroYt = (context as Activity).findViewById<ImageButton>(R.id.hero_yt)
        val graph = (context as Activity).findViewById<GraphView>(R.id.graph)

        val b = (hero.drawable as BitmapDrawable).bitmap
        Palette.generateAsync(b) { palette ->
            val colorDomPrimary = palette.getDominantColor(ContextCompat.getColor(context, R.color.colorPrimary))
            val colorLightWhite = palette.getLightMutedColor(ContextCompat.getColor(context, android.R.color.primary_text_dark))
            val colorMutedDark = palette.getDarkMutedColor(ContextCompat.getColor(context, R.color.colorPrimaryDark))
            val colorMutedBlack = palette.getDarkMutedColor(ContextCompat.getColor(context, android.R.color.background_dark))

            ctl.setContentScrimColor(colorDomPrimary)
            ctl.setExpandedTitleTextColor(ColorStateList.valueOf(colorLightWhite))

            if (Stuff.isDark(colorDomPrimary)) {
                ctl.setCollapsedTitleTextColor(colorLightWhite)
            } else {
                ctl.setCollapsedTitleTextColor(colorMutedDark)
            }
            (graph.series.get(0) as  LineGraphSeries<*>).color = colorLightWhite
            graph.gridLabelRenderer.horizontalAxisTitleColor = colorLightWhite
            graph.gridLabelRenderer.verticalLabelsColor = colorLightWhite
            val listBgFrom = (list.background as ColorDrawable).color

            val listBgAnimator = ObjectAnimator.ofObject(list, "backgroundColor", ArgbEvaluator(), listBgFrom, colorMutedBlack)
            val shareBgAnimator = ObjectAnimator.ofObject(heroShare, "colorFilter", ArgbEvaluator(), lastColorLightWhite, colorLightWhite)
            val infoBgAnimator = ObjectAnimator.ofObject(heroInfo, "colorFilter", ArgbEvaluator(), lastColorLightWhite, colorLightWhite)
            val ytBgAnimator = ObjectAnimator.ofObject(heroYt, "colorFilter", ArgbEvaluator(), lastColorLightWhite, colorLightWhite)
            val animSet = AnimatorSet()
            animSet.playTogether(listBgAnimator, shareBgAnimator, infoBgAnimator, ytBgAnimator)
            animSet.interpolator = AccelerateDecelerateInterpolator()
            animSet.duration = 1500
            animSet.start()

            lastColorDomPrimary = colorDomPrimary
            lastColorLightWhite = colorLightWhite
            lastColorMutedDark = colorMutedDark
            lastColorMutedBlack = colorMutedBlack
        }
    }

    private fun setGraph(points: String?) {
        if (points == null)
            return

        val graph = (context as Activity).findViewById<GraphView>(R.id.graph)
        val series = graph.series[0] as LineGraphSeries<DataPoint>
        val dps = mutableListOf<DataPoint>()
        var i = 0.0
        points.split(", ").forEach{
                    dps.add(DataPoint(i++, it.toDouble()))
                }

        Stuff.log("points: $points")
        series.resetData(dps.toTypedArray())

        graph.alpha = 0f
        graph.onDataChanged(false, false)
        graph.animate()
                .alpha(0.7f)
                .setInterpolator(DecelerateInterpolator())
                .setDuration(500)
                .start()
    }

    fun populate(res: PaginatedResult<Track>, page: Int = 1) {
        val refresh = (context as Activity).findViewById<SwipeRefreshLayout?>(R.id.swiperefresh) ?: return
        val list = (context as Activity).findViewById<ListView>(R.id.recents_list)
        if (list.visibility != View.VISIBLE)
            return
        val selectedId = if (list.checkedItemIds.isEmpty()) NP_ID else list.checkedItemIds[0]
        var selectedPos = 2
        refresh.isRefreshing = false
        list.removeCallbacks(timedRefresh)
        if (page == 1) {
            clear()
            list.postDelayed(timedRefresh, Stuff.RECENTS_REFRESH_INTERVAL)
            if (res.isEmpty){
                val headerTv = list.findViewById<TextView>(R.id.header_text)
                headerTv.text = context.getString(R.string.no_scrobbles)
            }
        }
        res.forEach {
            if (!it.isNowPlaying || page==1) {
                add(it)
                if (getItemId(count - 1) == selectedId)
                    selectedPos = count + 1
            }
        }
        list.setItemChecked(selectedPos, true)
        list.findViewById<View>(R.id.recents_progressbar).visibility = View.INVISIBLE
        notifyDataSetChanged()
    }

    fun markLoved(res: PaginatedResult<Track>) {
        val loved = res.filterNotNullTo(mutableListOf<Track>())
        for (i in loved.indices) {
            (0 until count)
                    .filter { loved[i].name == getItem(it).name && loved[i].artist == getItem(it).artist }
                    .forEach { getItem(it).isLoved = true }
        }
        notifyDataSetChanged()
    }

    private val loveToggle = View.OnClickListener { v ->
        val parentRow = v.parent as View
        val listView = parentRow.parent as ListView
        val pos = listView.getPositionForView(parentRow) - 2
        val anim = (v as ImageView).drawable as AnimatedVectorDrawableCompat
        val nextDrawable: AnimatedVectorDrawableCompat?

        if (v.getTag(R.id.recents_love) == FILLED) {
            LFMRequester(context, handler).execute(Stuff.UNLOVE,
                    getItem(pos).artist, getItem(pos).name)
            getItem(pos).isLoved = false
            v.setTag(R.id.recents_love, 0)
            nextDrawable = AnimatedVectorDrawableCompat.create(context, R.drawable.avd_heart_make)
        } else {
            LFMRequester(context, handler).execute(Stuff.LOVE,
                    getItem(pos).artist, getItem(pos).name)
            getItem(pos).isLoved = true
            v.setTag(R.id.recents_love, FILLED)
            nextDrawable = AnimatedVectorDrawableCompat.create(context, R.drawable.avd_heart_break)
        }
        anim.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
            override fun onAnimationEnd(drawable: Drawable?) {
                anim.unregisterAnimationCallback(this)
                v.setImageDrawable(nextDrawable)
            }
        })
        anim.start()
    }

    companion object {
        var lastColorDomPrimary:Int = Color.rgb(0x88, 0x0e, 0x4f)
        var lastColorLightWhite = Color.WHITE
        var lastColorMutedDark = Color.BLACK
        private var lastColorMutedBlack = Color.BLACK

        private val FILLED = 5
        private val NP_ID: Long = -5

        class ResponseHandler(private val recentsAdapter:RecentsAdapter):Handler(){
            override fun handleMessage(m: Message) {
                //usually:
                // obj = command, paginatedresult;
                val pair = m.obj as Pair<String, Any>
                val command = pair.first
                var data = pair.second
                when(command){
                    Stuff.GET_LOVED -> recentsAdapter.markLoved(data as PaginatedResult<Track>)
                    Stuff.HERO_INFO -> {
                        data = data as MutableList<String?>
                        recentsAdapter.setHero(data[0])
                        recentsAdapter.setGraph(data[1])
                    }
                    Stuff.GET_RECENTS -> {
                        recentsAdapter.populate(data as PaginatedResult<Track>, data.page)
                        LFMRequester(recentsAdapter.context, this).execute(Stuff.GET_LOVED)
                    }
                    Stuff.GET_RECENTS_CACHED -> {
                        recentsAdapter.populate(data as PaginatedResult<Track>, data.page)
                        recentsAdapter.loadRecents(1)
                    }
                    Stuff.IS_ONLINE -> {
                        val list = (recentsAdapter.context as Activity).findViewById<ListView?>(R.id.recents_list) ?: return
                        val headerTv = list.findViewById<TextView>(R.id.header_text)
                        if (data as Boolean)
                            headerTv.text = recentsAdapter.context.getString(R.string.recently_scrobbled)
                        else
                            headerTv.text = recentsAdapter.context.getString(R.string.offline)
                    }
                }
            }
        }
    }

}