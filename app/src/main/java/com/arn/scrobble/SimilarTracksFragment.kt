package com.arn.scrobble

import android.app.Fragment
import android.app.LoaderManager
import android.content.Loader
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.transition.Slide
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.GridView
import de.umass.lastfm.Track
import kotlinx.android.synthetic.main.content_similar.*
import kotlinx.android.synthetic.main.content_similar.view.*
import kotlinx.android.synthetic.main.coordinator_main.*
import kotlinx.android.synthetic.main.coordinator_main.view.*
import kotlinx.android.synthetic.main.header_default.view.*

/**
 * Created by arn on 29/12/2017.
 */
class SimilarTracksFragment : Fragment(), LoaderManager.LoaderCallbacks<Any?>{
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = Slide(Gravity.TOP)
        returnTransition = Slide(Gravity.TOP)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.content_similar, container, false)

        val adapter = SimilarTracksAdapter(activity, R.layout.grid_item_recents)
        view.similar_grid.adapter = adapter
        view.similar_grid.onItemClickListener = itemClickListener
        view.header_text.text = getString(R.string.similar_tracks)
        activity.app_bar.hero_similar
                .animate()
                .translationYBy(Stuff.dp2px(50, activity).toFloat())
                .setInterpolator(DecelerateInterpolator())
                .setDuration(400)
                .withEndAction {
                    Stuff.setAppBarHeight(activity, view.measuredHeight)
                    activity.app_bar.hero_similar.isEnabled = true
                    activity.ctl.hero_similar.visibility = View.GONE
                    activity.ctl.hero_title.visibility = View.VISIBLE
                }
                .start()
        return view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M){
            view!!
            view.y = -Stuff.dp2px(300, activity).toFloat()
            view.animate()
                    .translationY(0f)
                    .setInterpolator(DecelerateInterpolator())
                    .setDuration(500)
                    .start()
        }
        loaderManager.initLoader(Stuff.GET_SIMILAR.hashCode(),
                arrayOf(arguments.getString("artist"), arguments.getString("track")).toArgsBundle(), this)
                .forceLoad()
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        activity.ctl.hero_similar.visibility = View.VISIBLE
        activity.ctl.hero_title.visibility = View.GONE
        Stuff.setAppBarHeight(activity)
        activity.app_bar.hero_similar.animate()
                .translationYBy(-Stuff.dp2px(50, activity).toFloat())
                .setInterpolator(DecelerateInterpolator())
                .setDuration(400)
                .start()
        super.onDestroyView()
//        activity.app_bar.hero_similar.visibility = View.VISIBLE
    }

    override fun onCreateLoader(id: Int, b: Bundle?): Loader<Any?>? {
        b ?: return null
        val args = b.getStringArray("args")
        return when(id){
            Stuff.GET_SIMILAR.hashCode() -> LFMRequester(activity, Stuff.GET_SIMILAR, *args)
            else -> null
        }
    }

    override fun onLoadFinished(loader: Loader<Any?>, data: Any?) {
        data ?: return
        if (Main.isOnline)
            similar_grid_container.header_text.text = getString(R.string.similar_tracks)
        else
            similar_grid_container.header_text.text = getString(R.string.offline)
        when(loader.id) {
            Stuff.GET_SIMILAR.hashCode() -> {
                var pdata: ArrayList<Track>
                try {
                    pdata = data as ArrayList<Track>

                } catch (e: ClassCastException){
                    pdata = arrayListOf()
                }

                (similar_grid.adapter as SimilarTracksAdapter).populate(pdata)
            }
        }
    }

    override fun onLoaderReset(loader: Loader<Any?>) {
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        (similar_grid?.adapter as ArrayAdapter<*>?)?.notifyDataSetChanged()
    }

    private var lastClickedPos = 0
    private val itemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
        parent as GridView
        if (lastClickedPos == position) {
            val track = parent.getItemAtPosition(position) as Track
            Stuff.openSearchURL(track.artist + " - " + track.name, view, activity)
        } else {
            (parent.adapter as ArrayAdapter<*>?)?.notifyDataSetChanged()
        }
        lastClickedPos = position
    }
}