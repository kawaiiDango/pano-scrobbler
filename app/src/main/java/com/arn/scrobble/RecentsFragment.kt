package com.arn.scrobble

import android.app.Fragment
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CollapsingToolbarLayout
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.view.*
import android.widget.AdapterView
import android.widget.LinearLayout
import android.widget.ListView
import com.arn.scrobble.ui.EndlessScrollListener
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.GridLabelRenderer
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries


/**
 * Created by arn on 09/07/2017.
 */

class RecentsFragment : Fragment() {

    private var adapter: RecentsAdapter? = null

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
        val footer = inflater.inflate(R.layout.footer_loading, recentsList, false) as LinearLayout
        recentsList.emptyView = footer
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

//        val hero = activity.findViewById<ImageView>(R.id.img_hero)
//        hero.setImageResource(R.color.background_material_dark)

        val graph = activity.findViewById<GraphView>(R.id.graph)
        graph.gridLabelRenderer.gridStyle = GridLabelRenderer.GridStyle.NONE
        graph.gridLabelRenderer.isHorizontalLabelsVisible = false
        graph.gridLabelRenderer.isVerticalLabelsVisible = false
        graph.gridLabelRenderer.numVerticalLabels = 3
        val series = LineGraphSeries<DataPoint>()
        series.isDrawDataPoints = true
        series.thickness = 15
        series.dataPointsRadius = 20f
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
            Stuff.log(fragmentManager.findFragmentByTag(Stuff.GET_RECENTS).toString() +" rcefnts")
            fragmentManager.beginTransaction()
                    .hide(this)
                    .add(R.id.frame, PrefFragment())
                    .addToBackStack(null)
                    .commit()
        } else if (id == R.id.action_app_list) {
            fragmentManager.beginTransaction()
                    .hide(this)
                    .add(R.id.frame, AppListFragment())
                    .addToBackStack(null)
                    .commit()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        val ab = (activity as AppCompatActivity).supportActionBar
        ab?.setDisplayHomeAsUpEnabled(hidden)
        if (!hidden) {
            activity.findViewById<CollapsingToolbarLayout>(R.id.toolbar_layout)?.title = getString(R.string.app_name)
        }
    }

    override fun onStart() {
        super.onStart()
        adapter?.loadRecents(1)
    }

    override fun onResume() {
        super.onResume()
//        adapter.loadRecents(1)
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
        val i=l.toInt()
        adapterView as ListView
        adapterView.setItemChecked(pos1, true)
        if (Main.heroExpanded)
            adapterView.smoothScrollToPositionFromTop(i, 40, 500)

        val ab = activity.findViewById<AppBarLayout>(R.id.app_bar)
        ab?.setExpanded(true, true)
    }
}
