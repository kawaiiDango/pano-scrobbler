package com.arn.scrobble

import android.app.Fragment
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat
import android.view.*
import android.widget.AdapterView
import android.widget.HeaderViewListAdapter
import android.widget.ListView
import com.arn.scrobble.ui.EndlessScrollListener
import com.jjoe64.graphview.DefaultLabelFormatter
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.GridLabelRenderer
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import de.umass.lastfm.Track
import kotlinx.android.synthetic.main.content_recents.*
import kotlinx.android.synthetic.main.coordinator_main.*


/**
 * Created by arn on 09/07/2017.
 */

class RecentsFragment : Fragment() {

    private var adapter: RecentsAdapter? = null
    private var footer: View? = null

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
        recents_list.adapter = adapter
        adapter?.firstLoad()
        recents_list.setOnScrollListener(loadMoreListener)
        recents_list.onItemClickListener = itemClickListener

        Stuff.setProgressCircleColor(recents_swipe_refresh)
        recents_swipe_refresh.setOnRefreshListener { adapter?.loadRecents(1) }

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
                .getBoolean(Stuff.GRAPH_DETAILS_PREF,  false)
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
                .putBoolean(Stuff.GRAPH_DETAILS_PREF, show)
                .apply()
        graph.onDataChanged(false, false)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) {
            activity.ctl.title = getString(R.string.app_name)
            activity.ctl.setContentScrimColor(RecentsAdapter.lastColorDomPrimary)
            if (Stuff.isDark(RecentsAdapter.lastColorDomPrimary)) {
                activity.ctl.setCollapsedTitleTextColor(RecentsAdapter.lastColorLightWhite)
            } else {
                activity.ctl.setCollapsedTitleTextColor(RecentsAdapter.lastColorMutedDark)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        ((recents_list.adapter as HeaderViewListAdapter?)
                        ?.wrappedAdapter as RecentsAdapter)
                .handler.removeMessages(Stuff.CANCELLABLE_MSG)
    }

    override fun onResume() {
        super.onResume()
        adapter?.loadRecents(1)
        adapter?.loadPending()
    }

    private val loadMoreListener = object : EndlessScrollListener() {
        override fun onLoadMore(page: Int, totalItemsCount: Int): Boolean {
            return adapter?.loadRecents(page) ?: false
            //true ONLY if more data is actually being loaded; false otherwise.
        }
    }
    private val itemClickListener = AdapterView.OnItemClickListener { adapterView, v, pos1, l ->
        adapter?.notifyDataSetChanged()
        adapterView as ListView
        adapterView.setItemChecked(pos1, true)
        if (Main.heroExpanded)
            adapterView.smoothScrollToPositionFromTop(pos1, 40, 500)

        activity.app_bar?.setExpanded(true, true)
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
