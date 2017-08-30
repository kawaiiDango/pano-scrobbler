package com.arn.ytscrobble

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Handler
import android.os.Message
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CollapsingToolbarLayout
import android.support.design.widget.FloatingActionButton
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.graphics.Palette
import android.text.format.DateUtils
import android.text.format.DateUtils.MINUTE_IN_MILLIS
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import de.umass.lastfm.ImageSize
import de.umass.lastfm.PaginatedResult
import de.umass.lastfm.Track
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.*

/**
 * Created by arn on 10/07/2017.
 */

internal class RecentsAdapter
//        private ArrayList<Track> tracks;

(c: Context, private val layoutResourceId: Int) : ArrayAdapter<Track>(c, layoutResourceId, ArrayList()) {

    private val hero: ImageView = (c as Activity).findViewById(R.id.img_hero) as ImageView
    var lastClicked = -1
    private var gotLoved = false

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
            convertView = inflater.inflate(layoutResourceId, parent, false)
        }
        convertView = convertView!!
        // object item based on the position
        val t = getItem(position) ?: return convertView
//        if (t== null)
//            return convertView

// get the TextView and then set the text (item name) and tag (item ID) values
        val title = convertView.findViewById(R.id.recents_title) as TextView
        val subtitle = convertView.findViewById(R.id.recents_subtitle) as TextView
        val date = convertView.findViewById(R.id.recents_date) as TextView
        var relDate: CharSequence = ""
        if (t.isNowPlaying) {
            //                String np = "▶️";
            relDate = "playing right now..."
            val playingAnim = AnimationUtils.loadAnimation(context, R.anim.playing)
            val np = convertView.findViewById(R.id.recents_playing) as TextView
            np.startAnimation(playingAnim)
        }
        title.text = t.name
        subtitle.text = t.artist

        if (t.playedWhen != null) {
            relDate = DateUtils.getRelativeTimeSpanString(
                    t.playedWhen.time, System.currentTimeMillis(), MINUTE_IN_MILLIS)
            if (relDate[0] == '0')
                relDate = "Just now"
        }
        date.text = relDate
        val love = convertView.findViewById(R.id.recents_love) as ImageView
        val play = convertView.findViewById(R.id.recents_play) as ImageView

        love.setOnClickListener(loveToggle)

        if (gotLoved) {
            if (t.isLoved) {
                love.setImageResource(R.drawable.ic_line_heart_enabled)
                love.setTag(R.id.recents_love, FILLED)
            } else {
                love.setImageResource(R.drawable.ic_line_heart_disabled)
                love.setTag(R.id.recents_love, 0)
            }
        }
        val albumArt = convertView.findViewById(R.id.recents_album_art) as ImageView

        val imgUrl = t.getImageURL(ImageSize.MEDIUM)

        if (imgUrl != null && imgUrl != "") {
            albumArt.clearColorFilter()
            Picasso.with(context)
                    .load(imgUrl)
                    .fit()
                    .centerInside()
                    .placeholder(R.drawable.ic_lastfm)
                    .error(R.drawable.ic_placeholder_music)
                    .into(albumArt)

        } else {
            albumArt.setImageResource(R.drawable.ic_placeholder_music)
            albumArt.setColorFilter(Stuff.getMatColor(context, "500", t.name.hashCode().toLong()))
        }

        if ((position == 0 && lastClicked == -1 || position == lastClicked) && t.url != hero.tag) {
            val fab = (context as Activity).findViewById(R.id.fab) as FloatingActionButton
            fab.setOnClickListener {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(t.url))
                context.startActivity(browserIntent)
            }
            Scrobbler(context, handler).execute(Stuff.TRACK_HERO, t.url)

            hero.tag = t.url
            //                if (imgUrl != null && !imgUrl.equals("")) {
            setHero(t) //better set a blurred one
            //                }
            play.visibility = View.VISIBLE
            play.setTag(R.id.recents_play, t.artist + " - " + t.name)
            play.setOnClickListener(playClickListener)
        } else if (position != lastClicked) {
            play.visibility = View.INVISIBLE
        }
        return convertView
    }

    fun loadURL(page: Int) {
        Scrobbler(context, handler).execute(Stuff.GET_RECENTS, page.toString() + "")
    }

    fun setHero(imgUrl: String) {
        setHero(null, imgUrl)
    }

    private fun setHero(t: Track?, imgUrl: String? = null) {
        var imgUrl = imgUrl
        val ctl = (context as Activity).findViewById(R.id.toolbar_layout) as CollapsingToolbarLayout

        if (t != null) {
            val text = t.artist + " - " + t.name
            if (Main.heroExpanded)
                ctl.title = text
            ctl.tag = text
        }

        if (imgUrl == null && t != null)
            imgUrl = t.getImageURL(ImageSize.MEDIUM)
        if (imgUrl != null && imgUrl != "")
            Picasso.with(context)
                    .load(imgUrl)
                    .error(R.drawable.ic_placeholder_music)
                    .fit()
                    .centerCrop()
                    .noFade()
                    .into(hero, object : Callback {
                        override fun onSuccess() {
                            val fab = (context as Activity).findViewById(R.id.fab) as FloatingActionButton
                            val list = (context as Activity).findViewById(R.id.recents_list) as ListView
                            val b = (hero.drawable as BitmapDrawable).bitmap
                            Palette.generateAsync(b) { palette ->
                                val c1 = palette.getDominantColor(context.resources.getColor(R.color.colorPrimary))
                                val c2 = palette.getDarkMutedColor(context.resources.getColor(R.color.colorPrimaryDark))

                                ctl.setContentScrimColor(c1)
                                ctl.setStatusBarScrimColor(c2)
                                if (Stuff.isDark(c1)) {
                                    ctl.setCollapsedTitleTextColor(context.resources.getColor(android.R.color.white))
                                    fab.imageTintList = null
                                } else {
                                    ctl.setCollapsedTitleTextColor(context.resources.getColor(android.R.color.black))
                                    fab.imageTintList = ColorStateList.valueOf(0xff000000.toInt())
                                }

                                fab.backgroundTintList = ColorStateList.valueOf(c1)
                                list.setBackgroundColor(palette.getDarkMutedColor(context.resources.getColor(android.R.color.background_dark)))

                                val ab = (context as Activity).findViewById(R.id.app_bar) as AppBarLayout
                                ab.setExpanded(true, true)

                                list.smoothScrollToPosition(lastClicked)
                                if (lastClicked > list.lastVisiblePosition - 5)
                                    list.setSelection(lastClicked)
                                //                                        list.post(new Runnable() {
                                //                                            @Override
                                //                                            public void run() {
                                //
                                //
                                //                                            }
                                //                                        });
                                //
                            }
                            /*
                                BlurTransform bt = new BlurTransform(getContext());
                                Bitmap b = ((BitmapDrawable)hero.getDrawable()).getBitmap();
                                b = bt.transform(b, list.getWidth(), list.getHeight());
                                BitmapDrawable bd = new BitmapDrawable(getContext().getResources(), b);
                                bd.setGravity(Gravity.CENTER | Gravity.CENTER_VERTICAL);
                                list.setBackground(bd);
                                */
                        }

                        override fun onError() {
                            Stuff.log(context, "onerr")
                        }
                    })
        else {
            hero.setImageResource(R.drawable.ic_placeholder_music)

        }
    }

    fun populate(res: PaginatedResult<Track>, page: Int = 1) {
        val refresh = (context as Activity).findViewById(R.id.swiperefresh) as SwipeRefreshLayout
        refresh.isRefreshing = false
        if (page == 1) {
            //                gotLoved = false;
            clear()
        }
        res.forEach { add(it) }
        lastClicked = -1
        gotLoved = false
        notifyDataSetChanged()
    }

    fun markLoved(res: PaginatedResult<Track>) {
        val loved = res.filterNotNullTo(mutableListOf<Track>())
        for (i in loved.indices) {
            (0 until count)
                    .filter { loved[i].name == getItem(it).name && loved[i].artist == getItem(it).artist }
                    .forEach { getItem(it).isLoved = true }
        }
        gotLoved = true
        notifyDataSetChanged()
    }

    private val loveToggle = View.OnClickListener { v ->
        val ib = v as ImageView
        val parentRow = v.getParent() as View
        val listView = parentRow.parent as ListView
        val pos = listView.getPositionForView(parentRow) - 1

        if (v.getTag(R.id.recents_love) == FILLED) {
            Scrobbler(context, handler).execute(Stuff.UNLOVE,
                    getItem(pos).artist, getItem(pos).name)
            ib.setImageResource(R.drawable.ic_line_heart_disabled)
            ib.setTag(R.id.recents_love, 0)
        } else {
            Scrobbler(context, handler).execute(Stuff.LOVE,
                    getItem(pos).artist, getItem(pos).name)
            ib.setImageResource(R.drawable.ic_line_heart_enabled)
            ib.setTag(R.id.recents_love, FILLED)
            //                    Animatable anim = (Animatable) ib.getDrawable();
            //                    anim.start();
        }
    }

    private val playClickListener = View.OnClickListener { v ->
        var url = v.getTag(R.id.recents_play) as String
        try {
            url = "http://www.youtube.com/results?search_query=" + URLEncoder.encode(url, "UTF-8")
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(browserIntent)
        } catch (e: UnsupportedEncodingException) {
            Stuff.toast(context, "failed to encode url")
        }
    }

    private val handler: Handler = @SuppressLint("HandlerLeak")
    object: Handler(){
        override fun handleMessage(m: Message) {
            //usually:
            // obj = command, paginatedresult; arg1 = page num
            val pair = m.obj as Pair<String, Any>
            val command = pair.first
            val data = pair.second
            when(command){
                Stuff.GET_LOVED -> markLoved(data as PaginatedResult<Track>)
                Stuff.TRACK_HERO -> setHero(data as String)
                Stuff.GET_RECENTS -> populate(data as PaginatedResult<Track>, m.arg1)

            }
        }
    }

    companion object {
        private val FILLED = 5
    }

}