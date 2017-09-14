package com.arn.scrobble

import android.app.Fragment
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CollapsingToolbarLayout
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.view.*
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import com.arn.scrobble.db.PendingScrobble
import com.arn.scrobble.db.PendingScrobblesDb
import com.arn.scrobble.pref.PrefFragment
import com.arn.scrobble.ui.EndlessScrollListener
import com.jjoe64.graphview.DefaultLabelFormatter
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.GridLabelRenderer
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries


/**
 * Created by arn on 09/07/2017.
 */

class RecentsFragment : Fragment() {

    private var adapter: RecentsAdapter? = null
    private var footer: View? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        if (savedInstanceState == null)
            setHasOptionsMenu(true)
        return inflater.inflate(R.layout.content_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ab = (activity as AppCompatActivity).supportActionBar ?: return
        ab.setDisplayHomeAsUpEnabled(false)
        val recentsList = view.findViewById<ListView>(R.id.recents_list)
        val inflater = activity.layoutInflater

        recentsList.background.mutate()
        footer = inflater.inflate(R.layout.footer_loading, recentsList, false) as LinearLayout
        recentsList.addFooterView(footer, null, false)

        val header = inflater.inflate(R.layout.header_default, recentsList, false) as LinearLayout
        recentsList.addHeaderView(header, null, false)

        adapter = RecentsAdapter(activity, R.layout.list_item_recents)
        recentsList.adapter = adapter
        adapter?.firstLoad()
        recentsList.setOnScrollListener(loadMoreListener)
        recentsList.onItemClickListener = itemClickListener

        val refresh = activity.findViewById<SwipeRefreshLayout>(R.id.swiperefresh)
        refresh.setOnRefreshListener { adapter?.loadRecents(1) }

        val share = activity.findViewById<ImageView>(R.id.hero_share)
        share.setOnClickListener(shareClickListener)

        val graph = activity.findViewById<GraphView>(R.id.graph)
        graph.gridLabelRenderer.gridStyle = GridLabelRenderer.GridStyle.NONE
        graph.gridLabelRenderer.isHorizontalLabelsVisible = false
        graph.gridLabelRenderer.isVerticalLabelsVisible = false
        graph.gridLabelRenderer.numVerticalLabels = 3
        graph.gridLabelRenderer.labelsSpace = Stuff.dp2px(10, activity)
        val series = LineGraphSeries<DataPoint>()
        series.isDrawDataPoints = true
        series.thickness = Stuff.dp2px(6, activity)
        series.dataPointsRadius = Stuff.dp2px(7, activity).toFloat()
        series.color = resources.getColor(R.color.colorAccent)
        series.setAnimated(true)
        graph.addSeries(series)
        graph.setOnClickListener{
            if (graph.tag == true) {
                graph.gridLabelRenderer.horizontalAxisTitle = getString(R.string.graph_info)
                graph.gridLabelRenderer.gridStyle = GridLabelRenderer.GridStyle.VERTICAL
                graph.gridLabelRenderer.isVerticalLabelsVisible = true
                graph.tag = false
            } else {
                graph.gridLabelRenderer.horizontalAxisTitle = null
                graph.gridLabelRenderer.gridStyle = GridLabelRenderer.GridStyle.NONE
                graph.gridLabelRenderer.isVerticalLabelsVisible = false
                graph.tag = true
            }
            graph.onDataChanged(false, false)
        }
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_main, menu)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        if (id == R.id.action_settings) {
            fragmentManager.beginTransaction()
                    .hide(this)
                    .add(R.id.frame, PrefFragment())
                    .addToBackStack(null)
                    .commit()
        } else if (id == R.id.action_app_list) {
            dbTest()
        }
        return super.onOptionsItemSelected(item)
    }

    fun dbTest(){
        val dao = PendingScrobblesDb.getDb(activity).getDao()
        var entry = PendingScrobble()
        entry.artist = "arrfr"
        entry.track = "traeckg"
        entry.autoCorrected = 1
        entry.timestamp  = System.currentTimeMillis()
        dao.insert(entry)
        Stuff.log("count: "+dao.count)
        entry = dao.loadLastPending
        Stuff.log(entry.toString())
        dao.delete(entry)
        Stuff.log("count: "+dao.count)

    }
    override fun onHiddenChanged(hidden: Boolean) {
        val ab = (activity as AppCompatActivity).supportActionBar
        ab?.setDisplayHomeAsUpEnabled(hidden)
        if (!hidden) {
            activity.findViewById<CollapsingToolbarLayout>(R.id.toolbar_layout)?.title = getString(R.string.app_name)
        }
    }

    override fun onResume() {
        super.onResume()
        adapter?.loadRecents(1)
    }

    private val loadMoreListener = object : EndlessScrollListener() {
        override fun onLoadMore(page: Int, totalItemsCount: Int): Boolean {
            // Triggered only when new data needs to be appended to the list
            // Add whatever code is needed to append new items to your AdapterView
            adapter?.loadRecents(page)
            // or loadNextDataFromApi(totalItemsCount);
            return true // ONLY if more data is actually being loaded; false otherwise.
        }
    }
    private val itemClickListener = AdapterView.OnItemClickListener { adapterView, v, pos1, l ->
        adapter?.notifyDataSetChanged()
        adapterView as ListView
        adapterView.setItemChecked(pos1, true)
        if (Main.heroExpanded)
            adapterView.smoothScrollToPositionFromTop(pos1, 40, 500)

        val ab = activity.findViewById<AppBarLayout>(R.id.app_bar)
        ab?.setExpanded(true, true)
    }
    private val shareClickListener = View.OnClickListener {
        val ctl = activity.findViewById<CollapsingToolbarLayout>(R.id.toolbar_layout)
        var shareText = getString(R.string.share_text)
        shareText = "I was listening to:\n " +  ctl.title.toString() + "\n" + shareText
        val i = Intent(Intent.ACTION_SEND)
        i.type = "text/plain"
        i.putExtra(Intent.EXTRA_SUBJECT, "I was listening to:")
        i.putExtra(Intent.EXTRA_TEXT, shareText)
        startActivity(Intent.createChooser(i, "Share this song"))


    }
}
