@file:Suppress("DEPRECATION")

package com.arn.scrobble

import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.*
import android.os.Handler
import android.os.Message
import android.support.graphics.drawable.Animatable2Compat
import android.support.graphics.drawable.AnimatedVectorDrawableCompat
import android.support.v4.content.ContextCompat
import android.support.v7.graphics.Palette
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.*
import com.arn.scrobble.db.PendingScrobble
import com.arn.scrobble.db.PendingScrobblesDb
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import de.umass.lastfm.ImageSize
import de.umass.lastfm.PaginatedResult
import de.umass.lastfm.Track
import kotlinx.android.synthetic.main.content_recents.*
import kotlinx.android.synthetic.main.coordinator_main.*
import kotlinx.android.synthetic.main.footer_loading.view.*
import kotlinx.android.synthetic.main.header_default.view.*
import kotlinx.android.synthetic.main.header_pending.view.*
import kotlinx.android.synthetic.main.list_item_recents.view.*
import java.io.UnsupportedEncodingException
import java.net.URLEncoder

/**
 * Created by arn on 10/07/2017.
 */

class RecentsAdapter

(c: Context, private val layoutResourceId: Int) : ArrayAdapter<Track>(c, layoutResourceId, mutableListOf()) {

    private val hero = (c as Activity).hero_img
    private var heroInfoLoader: LFMRequester? = null
    val handler = ResponseHandler(this)
    private var totalPages:Int = 1

    init {
        setNotifyOnChange(false)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view : View? = convertView
        val activity = context as Activity
        if (view == null) {
            // inflate the layout
            val inflater = activity.layoutInflater
            view = inflater.inflate(layoutResourceId, parent, false)!!
        }
        // object item based on the position
        val t = getItem(position) ?: return view
        parent as ListView
        val selectedId: Long = if (parent.checkedItemIds.isEmpty()) NP_ID else parent.checkedItemIds[0]

        val np = view.recents_playing

        Stuff.nowPlayingAnim(np, t.isNowPlaying)
        view.recents_title.text = t.name
        view.recents_subtitle.text = t.artist
        view.recents_date.text = Stuff.myRelativeTime(context, t.playedWhen)

        val love = view.recents_love
        val play = view.recents_play

        love.setOnClickListener(loveToggle)

        if (t.isLoved) {
            love.setImageDrawable(AnimatedVectorDrawableCompat.create(context, R.drawable.avd_heart_break))
            love.setTag(R.id.recents_love, FILLED)
        } else {
            love.setImageDrawable(AnimatedVectorDrawableCompat.create(context, R.drawable.avd_heart_make))
            love.setTag(R.id.recents_love, 0)
        }

        val albumArt = view.recents_album_art

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
        val heroTag = if (hero.tag is Track) hero.tag as Track else null
        if ( getItemId(position) == selectedId &&
                (heroTag?.playedWhen != t.playedWhen || t.playedWhen == null )) {
            activity.hero_info.setOnClickListener { v:View ->
                Stuff.openInBrowser(t.url, context, v)
            }
            activity.hero_yt.setOnClickListener { v:View ->
                var ytUrl = "https://www.youtube.com/results?search_query="
                try {
                    ytUrl += URLEncoder.encode(t.artist + " - " + t.name, "UTF-8")
                } catch (e: UnsupportedEncodingException) {
                    Stuff.toast(context, context.getString(R.string.failed_encode_url))
                }
                Stuff.openInBrowser(ytUrl, context, v)
            }
            if (heroTag?.getImageURL(ImageSize.MEDIUM) != imgUrl) {
                setHero(t, imgUrl) //better set a blurred one
            }

            heroInfoLoader?.cancel(true)
            heroInfoLoader = LFMRequester(context, handler)
            heroInfoLoader?.execute(Stuff.HERO_INFO, t.url, t.getImageURL(ImageSize.EXTRALARGE))

            val ctl = (context as Activity).ctl
            ctl.title = t.name
            ctl.tag = t.name

            hero.tag = t

            play.visibility = View.VISIBLE
            play.setOnClickListener({activity.hero_yt.callOnClick()})
        } else if (getItemId(position) != selectedId) {
            play.visibility = View.INVISIBLE
        } else
            play.visibility = View.VISIBLE

        return view
    }

    override fun getItemId(position: Int): Long {
        return getItem(position)?.playedWhen?.time ?: NP_ID
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    fun loadRecents(page: Int):Boolean {
        val list = (context as Activity).recents_list ?: return false

        if (page <= totalPages) {
            Stuff.log("loadRecents $page")
            if ((page == 1 && list.firstVisiblePosition < 5) || page > 1)
                LFMRequester(context, handler).execute(Stuff.GET_RECENTS, page.toString())
            else {
                val msg = handler.obtainMessage(Stuff.CANCELLABLE_MSG, Pair(Stuff.RELOAD_LIST_DATA, ""))
                handler.sendMessageDelayed(msg, Stuff.RECENTS_REFRESH_INTERVAL)
            }
            if (count == 0 || page > 1)
                list.footer_progressbar.visibility = View.VISIBLE
            return true
        } else
            return false
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
                val activity = context as Activity
                val list = activity.recents_list ?: return@runOnUiThread
                val layout = list.header_pending
                if (count > 0) {
                    val pendingItems = layout.header_pending_items
                    pendingItems.removeAllViews()
                    val inflater = LayoutInflater.from(context)
                    aFew.forEach {
                        val v = inflater.inflate(R.layout.list_item_recents, pendingItems, false)
                        v.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        pendingItems.addView(PendingScroblesAdapter.populateItem(context, v, it))
                    }
                    val pendingSummary = layout.pending_summary
                    val diff = count - aFew.size
                    if (diff > 0) {
                        pendingSummary.pending_more.text = context.getString(R.string.pending_scrobbles_summary, diff)
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
        val activity = context as Activity
        val ctl = activity.ctl ?: return
        val list = activity.recents_list ?: return
        val graph = activity.graph

        val b = (hero.drawable as BitmapDrawable).bitmap
        Palette.generateAsync(b) { palette ->
            val colorDomPrimary = palette.getDominantColor(ContextCompat.getColor(context, R.color.colorPrimary))
            val colorLightWhite = palette.getLightMutedColor(ContextCompat.getColor(context, android.R.color.primary_text_dark))
            val colorMutedDark = palette.getDarkMutedColor(ContextCompat.getColor(context, R.color.colorPrimaryDark))
            val colorMutedBlack = palette.getDarkMutedColor(ContextCompat.getColor(context, android.R.color.background_dark))

            ctl.setContentScrimColor(colorDomPrimary)
            ctl.setExpandedTitleTextColor(ColorStateList.valueOf(colorLightWhite))

            if (Stuff.isDark(colorDomPrimary))
                ctl.setCollapsedTitleTextColor(colorLightWhite)
            else
                ctl.setCollapsedTitleTextColor(colorMutedDark)

            (graph.series[0] as  LineGraphSeries<*>).color = colorLightWhite
            graph.gridLabelRenderer.horizontalAxisTitleColor = colorLightWhite
            graph.gridLabelRenderer.verticalLabelsColor = colorLightWhite
            graph.onDataChanged(false, false)

            val listBgFrom = (list.background as ColorDrawable).color

            val listBgAnimator = ObjectAnimator.ofObject(list, "backgroundColor", ArgbEvaluator(), listBgFrom, colorMutedBlack)
            val shareBgAnimator = ObjectAnimator.ofObject(activity.hero_share, "colorFilter", ArgbEvaluator(), lastColorLightWhite, colorLightWhite)
            val infoBgAnimator = ObjectAnimator.ofObject(activity.hero_info, "colorFilter", ArgbEvaluator(), lastColorLightWhite, colorLightWhite)
            val ytBgAnimator = ObjectAnimator.ofObject(activity.hero_yt, "colorFilter", ArgbEvaluator(), lastColorLightWhite, colorLightWhite)
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
        val graph = (context as Activity).graph
        if (points == null) {
            graph.visibility = View.INVISIBLE
        } else {
            graph.visibility = View.VISIBLE

            val series = graph.series[0] as LineGraphSeries<DataPoint>
            val dps = mutableListOf<DataPoint>()
            var i = 0.0
            points.split(", ").forEach {
                dps.add(DataPoint(i++, it.toDouble()))
            }

            Stuff.log("points: $points")
            series.resetData(dps.toTypedArray())

            graph.alpha = 0f
//            graph.onDataChanged(false, false)
            graph.animate()
                    .alpha(0.7f)
                    .setInterpolator(DecelerateInterpolator())
                    .setDuration(500)
                    .start()
        }
    }

    fun populate(res: PaginatedResult<Track>, page: Int = 1) {
        val refresh = (context as Activity).recents_swipe_refresh ?: return
        val list = (context as Activity).recents_list
        if (list.visibility != View.VISIBLE)
            return
        val selectedId = if (list.checkedItemIds.isEmpty()) NP_ID else list.checkedItemIds[0]
        var selectedPos = 2
        refresh.isRefreshing = false
        totalPages = res.totalPages
        handler.removeMessages(Stuff.CANCELLABLE_MSG)
        if (page == 1) {
            clear()
            val msg = handler.obtainMessage(Stuff.CANCELLABLE_MSG, Pair(Stuff.RELOAD_LIST_DATA, ""))
            handler.sendMessageDelayed(msg, Stuff.RECENTS_REFRESH_INTERVAL)
            if (res.isEmpty){
                list.header_text.text = context.getString(R.string.no_scrobbles)
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
        list.footer_progressbar.visibility = View.INVISIBLE
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

    class ResponseHandler(private val recentsAdapter:RecentsAdapter):Handler(){
        override fun handleMessage(m: Message) {
            //usually:
            // obj = command, paginatedresult;
            val pair = m.obj as Pair<String, Any>
            val command = pair.first
            var data = pair.second
            when(command){
                Stuff.HERO_INFO -> {
                    data = data as MutableList<String?>
                    recentsAdapter.setHero(data[0])
                    recentsAdapter.setGraph(data[1])
                }
                Stuff.GET_RECENTS -> {
                    recentsAdapter.populate(data as PaginatedResult<Track>, data.page)
                }
                Stuff.GET_RECENTS_CACHED -> {
                    recentsAdapter.populate(data as PaginatedResult<Track>, data.page)
                    recentsAdapter.loadRecents(1)
                }
                Stuff.IS_ONLINE -> {
                    val list = (recentsAdapter.context as Activity).recents_list ?: return
                    if (data as Boolean)
                        list.header_text.text = recentsAdapter.context.getString(R.string.recently_scrobbled)
                    else
                        list.header_text.text = recentsAdapter.context.getString(R.string.offline)
                }
                Stuff.RELOAD_LIST_DATA -> recentsAdapter.loadRecents(1)
            }
        }
    }
    companion object {
        var lastColorDomPrimary:Int = Color.rgb(0x88, 0x0e, 0x4f)
        var lastColorLightWhite = Color.WHITE
        var lastColorMutedDark = Color.BLACK
        private var lastColorMutedBlack = Color.BLACK

        private val FILLED = 5
        private val NP_ID: Long = -5
    }

}