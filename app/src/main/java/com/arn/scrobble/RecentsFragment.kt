package com.arn.scrobble

import android.app.Fragment
import android.app.LoaderManager
import android.content.Intent
import android.content.Loader
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.AdapterView
import android.widget.ListView
import com.arn.scrobble.ui.EndlessScrollListener
import com.jjoe64.graphview.DefaultLabelFormatter
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.GridLabelRenderer
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import de.umass.lastfm.PaginatedResult
import de.umass.lastfm.Track
import kotlinx.android.synthetic.main.content_recents.*
import kotlinx.android.synthetic.main.coordinator_main.*
import kotlinx.android.synthetic.main.footer_loading.view.*
import kotlinx.android.synthetic.main.header_default.view.*


/**
 * Created by arn on 09/07/2017.
 */

class RecentsFragment : Fragment(), LoaderManager.LoaderCallbacks<Any?>{
    private lateinit var adapter: RecentsAdapter
    private var footer: View? = null
    private var firstLoad = true
    private var runnable = Stuff.TimedRefresh(this, Stuff.GET_RECENTS.hashCode())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.content_recents, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val inflater = activity.layoutInflater

        recents_list.background.mutate()
        footer = inflater.inflate(R.layout.footer_loading, recents_list, false)
        recents_list.addFooterView(footer, null, false)

        val pHeader = inflater.inflate(R.layout.header_pending, recents_list, false)
        recents_list.addHeaderView(pHeader, null, false)

        val header = inflater.inflate(R.layout.header_default, recents_list, false)
        recents_list.addHeaderView(header, null, false)

        adapter = RecentsAdapter(activity, R.layout.list_item_recents)
        adapter.heroInfoLoader = loaderManager.initLoader(Stuff.HERO_INFO.hashCode(), arrayOf("","").toArgsBundle(), this) as LFMRequester.Loader
        recents_list.adapter = adapter
        loaderManager.initLoader(Stuff.GET_RECENTS.hashCode(), arrayOf("1").toArgsBundle(), this).startLoading()

        recents_list.setOnScrollListener(loadMoreListener)
        recents_list.onItemClickListener = itemClickListener

        Stuff.setProgressCircleColor(recents_swipe_refresh)
        recents_swipe_refresh.setOnRefreshListener { loadRecents(1) }

        activity.hero_share.setOnClickListener(shareClickListener)

        val graph = activity.graph
        graph.gridLabelRenderer.gridStyle = GridLabelRenderer.GridStyle.NONE
        graph.gridLabelRenderer.isHorizontalLabelsVisible = false
        graph.gridLabelRenderer.isVerticalLabelsVisible = false
        graph.gridLabelRenderer.numVerticalLabels = 3
        graph.gridLabelRenderer.labelsSpace = Stuff.sp2px(10, activity)
        val series = LineGraphSeries<DataPoint>()
        series.isDrawDataPoints = true
        series.thickness = Stuff.dp2px(6, activity)
        series.dataPointsRadius = Stuff.dp2px(6, activity).toFloat()
        series.color = ContextCompat.getColor(activity, R.color.colorAccent)
        series.setAnimated(true)
        graph.addSeries(series)
        graph.setOnClickListener{
            toggleGraphDetails(graph)
        }
        graph.tag = PreferenceManager.getDefaultSharedPreferences(activity)
                .getBoolean(Stuff.PREF_GRAPH_DETAILS,  false)
        toggleGraphDetails(graph)
        graph.gridLabelRenderer.gridStyle = GridLabelRenderer.GridStyle.NONE
        graph.gridLabelRenderer.labelFormatter = object : DefaultLabelFormatter() {
            override fun formatLabel(value: Double, isValueX: Boolean): String {
                return if (isValueX) {
                    super.formatLabel(value, isValueX)
                } else {
                    Stuff.humanReadableNum(value.toLong())
                }
            }
        }

        activity.hero_info.setOnClickListener { v:View ->
            val t = activity.hero_img.tag
            if (t is Track)
                Stuff.openInBrowser(t.url, activity, v)
        }
        activity.hero_play.setOnClickListener { v:View ->
            val t = activity.hero_img.tag
            if (t is Track)
                Stuff.openSearchURL(t.artist + " - " + t.name, v, activity)
        }
        activity.hero_similar.setOnClickListener { v:View ->
            v.isEnabled = false
            val t = activity.hero_img.tag
            if (t is Track) {
                val simFragment = SimilarTracksFragment()
                val b = Bundle()
                b.putString("artist", t.artist)
                b.putString("track", t.name)
                simFragment.arguments = b

                fragmentManager.beginTransaction()
                        .hide(this)
                        .add(R.id.frame, simFragment, Stuff.GET_SIMILAR)
                        .addToBackStack(null)
                        .commit()
            }
        }
    }
    private fun setGraph(points: String?) {
        val graph = activity.graph
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

    private fun toggleGraphDetails(graph: GraphView){
        val show = graph.tag as Boolean? ?: false
        if (show) {
            graph.gridLabelRenderer.horizontalAxisTitle = getString(R.string.graph_info)
        } else {
            graph.gridLabelRenderer.horizontalAxisTitle = null
        }
        graph.gridLabelRenderer.isVerticalLabelsVisible = show
        graph.tag = !show
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(Stuff.PREF_GRAPH_DETAILS, show)
                .apply()
        graph.onDataChanged(false, false)
    }

    override fun onCreateLoader(id: Int, b: Bundle?): Loader<Any?>? {
        b ?: return null
        val args = b.getStringArray("args")
        return when(id){
            Stuff.HERO_INFO.hashCode() -> LFMRequester(Stuff.HERO_INFO, *args).asLoader(activity)
            Stuff.GET_RECENTS.hashCode() -> LFMRequester(Stuff.GET_RECENTS_CACHED, *args).asLoader(activity)
            else -> null
        }
    }

    private fun loadRecents(page: Int):Boolean {
        recents_list ?: return false
        if (page <= adapter.totalPages) {
            if ((page == 1 && recents_list.firstVisiblePosition < 5) || page > 1) {
                val getRecents = loaderManager.getLoader<Any>(Stuff.GET_RECENTS.hashCode()) as LFMRequester.Loader?
                getRecents ?: return false
                getRecents.args[0] = page.toString()
                getRecents.forceLoad()
                adapter.loadPending()
            } else {
                recents_list?.postDelayed(runnable, Stuff.RECENTS_REFRESH_INTERVAL)
            }
            if (adapter.count == 0 || page > 1)
                recents_list.footer_progressbar.visibility = View.VISIBLE
            return true
        } else
            return false
    }

    override fun onLoadFinished(loader: Loader<Any?>, data: Any?) {
        data ?: return
        if (Main.isOnline)
            recents_list.header_text.text = getString(R.string.recently_scrobbled)
        else
            recents_list.header_text.text = getString(R.string.offline)
        Stuff.log("onLoadFinished " + (loader as LFMRequester.Loader).command)
        when(loader.id) {
            Stuff.HERO_INFO.hashCode() -> {
                data as MutableList<String?>
                adapter.setHero(data[0])
                setGraph(data[1])
            }
            Stuff.GET_RECENTS.hashCode() -> {
                data as PaginatedResult<Track>
                adapter.populate(data, data.page)
                if (firstLoad) {
                    loadRecents(1)
                    firstLoad = false
                } else if (data.page == 1){
                    recents_list?.postDelayed(runnable, Stuff.RECENTS_REFRESH_INTERVAL)
                }
            }
        }
    }

    override fun onLoaderReset(loader: Loader<Any?>?) {
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) {
            activity.ctl.setContentScrimColor(RecentsAdapter.lastColorDomPrimary)
            if (Stuff.isDark(RecentsAdapter.lastColorDomPrimary)) {
                activity.ctl.setCollapsedTitleTextColor(RecentsAdapter.lastColorLightWhite)
            } else {
                activity.ctl.setCollapsedTitleTextColor(RecentsAdapter.lastColorMutedDark)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        activity.ctl.tag = getString(R.string.recently_scrobbled)
    }

    override fun onStop() {
        recents_list.removeCallbacks(runnable)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        loadRecents(1)
    }

    private val loadMoreListener = object : EndlessScrollListener() {
        override fun onLoadMore(page: Int, totalItemsCount: Int): Boolean {
            return loadRecents(page)
            //true ONLY if more data is actually being loaded; false otherwise.
        }
    }

    private val itemClickListener = AdapterView.OnItemClickListener { adapterView, v, pos1, l ->
        adapterView as ListView
        adapterView.setItemChecked(pos1, true)
        if (!activity.app_bar.isExpanded)
            adapterView.smoothScrollToPositionFromTop(pos1, Stuff.dp2px(5, activity), 500)

        activity.app_bar?.setExpanded(true, true)
        adapter.notifyDataSetChanged()
    }

    private val shareClickListener = View.OnClickListener {
        val track = activity.hero_img?.tag
        if (track is Track) {
            Stuff.log("shareClickListener $track")

            val shareText = getString(R.string.share_text,
                    track.artist + " - " + track.name, Stuff.myRelativeTime(activity, track.playedWhen))
            val i = Intent(Intent.ACTION_SEND)
            i.type = "text/plain"
            i.putExtra(Intent.EXTRA_SUBJECT, shareText)
            i.putExtra(Intent.EXTRA_TEXT, shareText + "\n" + getString(R.string.share_sig, getString(R.string.share_link)))
            startActivity(Intent.createChooser(i, getString(R.string.share_this_song)))
        }
    }
}
