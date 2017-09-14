@file:Suppress("DEPRECATION")

package com.arn.scrobble

import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.*
import android.net.Uri
import android.os.Handler
import android.os.Message
import android.support.design.widget.CollapsingToolbarLayout
import android.support.design.widget.FloatingActionButton
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.graphics.Palette
import android.text.format.DateUtils
import android.text.format.DateUtils.MINUTE_IN_MILLIS
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.*
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

    init {
        setNotifyOnChange(false)
    }

    private val hero: ImageView = (c as Activity).findViewById<ImageView>(R.id.img_hero)

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
        var selectedId = if (parent.checkedItemIds.size == 0) -5 else parent.checkedItemIds[0]

// get the TextView and then set the text (item name) and tag (item ID) values
        val title = convertView.findViewById<TextView>(R.id.recents_title)
        val subtitle = convertView.findViewById<TextView>(R.id.recents_subtitle)
        val date = convertView.findViewById<TextView>(R.id.recents_date)
        val np = convertView.findViewById<ImageView>(R.id.recents_playing)
        var relDate: CharSequence = ""

        if (t.isNowPlaying) {
            relDate = "   now"
            np.visibility = View.VISIBLE
            val anim = np.getDrawable() as AnimatedVectorDrawable
            anim.registerAnimationCallback(object : Animatable2.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable?) {
                    if (drawable != null && drawable.isVisible)
                        anim.start()
                }
            })
            anim.start()
        } else
            np.visibility = View.GONE
        title.text = t.name
        subtitle.text = t.artist

        if (t.playedWhen != null) {
            relDate = DateUtils.getRelativeTimeSpanString(
                    t.playedWhen.time, System.currentTimeMillis(), MINUTE_IN_MILLIS)
            if (relDate[0] == '0')
                relDate = "Just now"
        }
        date.text = relDate
        val love = convertView.findViewById<ImageView>(R.id.recents_love)
        val play = convertView.findViewById<ImageView>(R.id.recents_play)

        love.setOnClickListener(loveToggle)

        if (t.isLoved) {
            love.setImageResource(R.drawable.ic_line_heart_enabled)
            love.setTag(R.id.recents_love, FILLED)
        } else {
            love.setImageResource(R.drawable.ic_line_heart_disabled)
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
            val fab = (context as Activity).findViewById<FloatingActionButton>(R.id.fab)
            val heroInfo = (context as Activity).findViewById<ImageButton>(R.id.hero_info)
            val heroYt = (context as Activity).findViewById<ImageButton>(R.id.hero_yt)
            fab.setOnClickListener {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(t.url))
                context.startActivity(browserIntent)
            }
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
//            play.setTag(R.id.recents_play, t.artist + " - " + t.name)
            play.setOnClickListener({heroYt.callOnClick()})
        } else if (getItemId(position) != selectedId) {
            play.visibility = View.INVISIBLE
        } else
            play.visibility = View.VISIBLE

        return convertView
    }

    override fun getItemId(position: Int): Long {
        return getItem(position)?.playedWhen?.time ?: -5
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    fun loadRecents(page: Int) {
        Stuff.log("loadRecents $page")
        LFMRequester(context, handler).execute(Stuff.GET_RECENTS, page.toString())
    }
    fun firstLoad() {
        Stuff.log("firstLoad")
        LFMRequester(context, handler).execute(Stuff.GET_RECENTS_CACHED, (1).toString())
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

//        if (imgUrl == null && t != null) //called from getview
//            imgUrl = t.getImageURL(ImageSize.MEDIUM)
//        else if (imgUrl == null && t == null) // called from handler
//            imgUrl = t.getImageURL(ImageSize.LARGE)
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
                            Stuff.log("onerr")
                        }
                    })
        } else {
            if (t!= null)
                hero.setColorFilter(Stuff.getMatColor(context, "500", t.name.hashCode().toLong()))
            hero.setImageResource(R.drawable.ic_placeholder_music)
            setPaletteColors()

        }
    }

    private fun setPaletteColors(){
        val ctl = (context as Activity).findViewById<CollapsingToolbarLayout>(R.id.toolbar_layout) ?: return
        val list = (context as Activity).findViewById<ListView?>(R.id.recents_list) ?: return
//        val fab = (context as Activity).findViewById<FloatingActionButton?>(R.id.fab)
        val heroShare = (context as Activity).findViewById<ImageButton>(R.id.hero_share)
        val heroInfo = (context as Activity).findViewById<ImageButton>(R.id.hero_info)
        val heroYt = (context as Activity).findViewById<ImageButton>(R.id.hero_yt)
        val graph = (context as Activity).findViewById<GraphView>(R.id.graph)

        val b = (hero.drawable as BitmapDrawable).bitmap
        Palette.generateAsync(b) { palette ->
            val colorDomPrimary = palette.getDominantColor(context.resources.getColor(R.color.colorPrimary))
            val colorLightWhite = palette.getLightMutedColor(context.resources.getColor(android.R.color.primary_text_dark))
            val colorMutedDark = palette.getDarkMutedColor(context.resources.getColor(R.color.colorPrimaryDark))
            val colorMutedBlack = palette.getDarkMutedColor(context.resources.getColor(android.R.color.background_dark))

            ctl.setContentScrimColor(colorDomPrimary)
            ctl.setStatusBarScrimColor(colorMutedDark)
            ctl.setExpandedTitleTextColor(ColorStateList.valueOf(colorLightWhite))

            if (Stuff.isDark(colorDomPrimary)) {
                ctl.setCollapsedTitleTextColor(colorLightWhite)
//                fab?.imageTintList = null
            } else {
                ctl.setCollapsedTitleTextColor(colorMutedDark)
//                fab?.imageTintList = ColorStateList.valueOf(0xff000000.toInt())
            }
            (graph.series.get(0) as  LineGraphSeries<*>).color = colorLightWhite
            graph.gridLabelRenderer.horizontalAxisTitleColor = colorLightWhite
            graph.gridLabelRenderer.verticalAxisTitleColor = colorLightWhite
//                                fab.backgroundTintList = ColorStateList.valueOf(colorDomPrimary)
            val listBgFrom = (list.background as ColorDrawable).color

            val listBgAnimator = ObjectAnimator.ofObject(list, "backgroundColor", ArgbEvaluator(), listBgFrom, colorMutedBlack)
//                                val fabBgAnimator = ObjectAnimator.ofArgb(fab.contentBackground.mutate(), "tint", lastColorDomPrimary, colorDomPrimary)
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
        val series = graph.series.get(0) as LineGraphSeries<DataPoint>
        val dps = mutableListOf<DataPoint>()
        var i: Double = 0.0
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
        val selectedId = if (list.checkedItemIds.size == 0) -5 else list.checkedItemIds[0]
        var selectedPos = 1
        refresh.isRefreshing = false
        list.removeCallbacks(timedRefresh)
        if (page == 1) {
            clear()
            list.postDelayed(timedRefresh, Stuff.RECENTS_REFRESH_INTERVAL)
        }
        res.forEach {
            if (!it.isNowPlaying || page==1) {
                add(it)
                if (getItemId(count - 1) == selectedId)
                    selectedPos = count
            }
        }
        list.setItemChecked(selectedPos, true)
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
        val love = v as ImageView
        val parentRow = v.getParent() as View
        val listView = parentRow.parent as ListView
        val pos = listView.getPositionForView(parentRow) - 1

        if (v.getTag(R.id.recents_love) == FILLED) {
            LFMRequester(context, handler).execute(Stuff.UNLOVE,
                    getItem(pos).artist, getItem(pos).name)
            getItem(pos).isLoved = false
            love.setImageResource(R.drawable.ic_line_heart_disabled)
            love.setTag(R.id.recents_love, 0)

            love.animate()
                    .alpha(0f)
                    .scaleX(5f)
                    .scaleY(5f)
                    .setInterpolator(DecelerateInterpolator())
                    .setDuration(500)
                    .withEndAction {
                        love.alpha = 1f
                        love.scaleX = 1f
                        love.scaleY = 1f
//                        love.setImageResource(R.drawable.ic_line_heart_disabled)
                    }
                    .start()
        } else {
            LFMRequester(context, handler).execute(Stuff.LOVE,
                    getItem(pos).artist, getItem(pos).name)
            getItem(pos).isLoved = true
            love.setTag(R.id.recents_love, FILLED)
            love.alpha = 0f
            love.scaleX = 5f
            love.scaleY = 5f
            love.setImageResource(R.drawable.ic_line_heart_enabled)
            love.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setInterpolator(DecelerateInterpolator())
                    .setDuration(500)
                    .start()

        }
    }

    private val handler = @SuppressLint("HandlerLeak")
    object: Handler(){
        override fun handleMessage(m: Message) {
            //usually:
            // obj = command, paginatedresult;
            val pair = m.obj as Pair<String, Any>
            val command = pair.first
            var data = pair.second
            when(command){
                Stuff.GET_LOVED -> markLoved(data as PaginatedResult<Track>)
                Stuff.HERO_INFO -> {
                        data = data as MutableList<String?>
                        setHero(data[0])
                        setGraph(data[1])
                }
                Stuff.GET_RECENTS -> {
                    populate(data as PaginatedResult<Track>, data.page)
                    LFMRequester(context, this).execute(Stuff.GET_LOVED)
                }
                Stuff.GET_RECENTS_CACHED -> {
                    populate(data as PaginatedResult<Track>, data.page)
                    loadRecents(1)
                }
                Stuff.IS_ONLINE -> {
                    val list = (context as Activity).findViewById<ListView?>(R.id.recents_list) ?: return
                    val headerTv = list.findViewById<TextView>(R.id.header_text)
                    if (data as Boolean)
                        headerTv.text = context.getString(R.string.recently_scrobbled)
                    else
                        headerTv.text = context.getString(R.string.offline)
                }
            }
        }
    }

    var timedRefresh = object : Runnable {
        override fun run() {
           loadRecents(1)
        }
    }
    companion object {
        private var lastColorDomPrimary = 0
        private var lastColorLightWhite = 0
        private var lastColorMutedDark = 0
        private var lastColorMutedBlack = 0

        private val FILLED = 5

        private var heroInfoLoader: LFMRequester? = null
    }

}