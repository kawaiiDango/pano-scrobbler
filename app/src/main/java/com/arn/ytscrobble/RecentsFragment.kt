package com.arn.ytscrobble

import android.app.Fragment
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CollapsingToolbarLayout
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.text.Layout
import android.view.*
import android.widget.*
import com.arn.ytscrobble.ui.EndlessScrollListener
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.GridLabelRenderer
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries


/**
 * Created by arn on 09/07/2017.
 */

class RecentsFragment : Fragment() {

    lateinit private var adapter: RecentsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.content_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ab = (activity as AppCompatActivity).supportActionBar
        ab?.setDisplayHomeAsUpEnabled(false)
        val recentsList = activity.findViewById(R.id.recents_list) as ListView
        val inflater = activity.layoutInflater

        recentsList.background.mutate()
        val footer = inflater.inflate(R.layout.recents_footer, recentsList, false) as LinearLayout
        recentsList.emptyView = footer
        recentsList.addFooterView(footer, null, false)

        val header = inflater.inflate(R.layout.recents_header, recentsList, false) as LinearLayout
        recentsList.addHeaderView(header, null, false)

        adapter = RecentsAdapter(activity, R.layout.list_item)
        recentsList.adapter = adapter
        recentsList.setOnScrollListener(loadMoreListener)
        recentsList.setOnItemClickListener{
            adapterView, view, i, l -> itemClickListener(adapterView, view, i, l)
        }

        val refresh = activity.findViewById(R.id.swiperefresh) as SwipeRefreshLayout
        refresh.setOnRefreshListener { adapter.loadURL(1) }

        val hero = activity.findViewById(R.id.img_hero) as ImageView
        hero.setImageResource(R.color.background_material_dark)

        val graph = activity.findViewById(R.id.graph) as GraphView
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
        graph.setOnClickListener({
            if (graph.tag == true) {
                graph.gridLabelRenderer.horizontalAxisTitle = getString(R.string.graph_info)
                graph.gridLabelRenderer.gridStyle = GridLabelRenderer.GridStyle.VERTICAL
                graph.gridLabelRenderer.isVerticalLabelsVisible = true
                graph.tag = false
            } else{
                graph.gridLabelRenderer.horizontalAxisTitle = null
                graph.gridLabelRenderer.gridStyle = GridLabelRenderer.GridStyle.NONE
                graph.gridLabelRenderer.isVerticalLabelsVisible = false
                graph.tag = true
            }
            graph.onDataChanged(false, false)
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_main, menu)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        val ab = (activity as AppCompatActivity).supportActionBar
        ab?.setDisplayHomeAsUpEnabled(hidden)
        if (!hidden) {
            (activity.findViewById(R.id.toolbar_layout) as CollapsingToolbarLayout).title = getString(R.string.app_name)
        }
    }

    override fun onResume() {
        super.onResume()
        adapter.loadURL(1)
    }

    private val loadMoreListener: EndlessScrollListener = object : EndlessScrollListener() {
        override fun onLoadMore(page: Int, totalItemsCount: Int): Boolean {
            // Triggered only when new data needs to be appended to the list
            // Add whatever code is needed to append new items to your AdapterView
            adapter.loadURL(page)
            // or loadNextDataFromApi(totalItemsCount);
            return true // ONLY if more data is actually being loaded; false otherwise.
        }
    }
    private fun itemClickListener(adapterView: AdapterView<*>, v: View, i: Int, l: Long) {
        adapter.lastClicked = i - 1
        adapter.notifyDataSetChanged()

        val ab = activity.findViewById(R.id.app_bar) as AppBarLayout
        ab.setExpanded(true, true)
        val list = activity.findViewById(R.id.recents_list) as ListView
        list.smoothScrollToPosition(adapter.lastClicked)
        }
}
