package com.arn.scrobble

import android.app.Fragment
import android.content.ComponentName
import android.content.Intent
import android.content.pm.LabeledIntent
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CollapsingToolbarLayout
import android.support.v4.content.FileProvider
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.view.*
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.ListView
import com.arn.scrobble.pref.PrefFragment
import com.arn.scrobble.ui.EndlessScrollListener
import com.jjoe64.graphview.DefaultLabelFormatter
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.GridLabelRenderer
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import java.io.File


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
        val list = view.findViewById<ListView>(R.id.recents_list)
        val inflater = activity.layoutInflater

        list.background.mutate()
        footer = inflater.inflate(R.layout.footer_loading, list, false)
        list.addFooterView(footer, null, false)

        val pHeader = inflater.inflate(R.layout.header_pending, list, false)
        list.addHeaderView(pHeader, null, false)

        val header = inflater.inflate(R.layout.header_default, list, false)
        list.addHeaderView(header, null, false)

        adapter = RecentsAdapter(activity, R.layout.list_item_recents)
        list.adapter = adapter
        adapter?.firstLoad()
        list.setOnScrollListener(loadMoreListener)
        list.onItemClickListener = itemClickListener

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
        series.dataPointsRadius = Stuff.dp2px(6, activity).toFloat()
        series.color = resources.getColor(R.color.colorAccent)
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
        } else if (id == R.id.action_report) {
            mailLogs()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun mailLogs(){
        Stuff.toast(activity, "Generating report...")

        val log = Stuff.getLogcat()
        val file = File(activity.filesDir, "log.txt")
        file.writeText(log)
        val uri = FileProvider.getUriForFile(activity, activity.packageName+".fileprovider", file)

        val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", "huh@huh.com", null))
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "huh?")
        val resolveInfos = activity.packageManager.queryIntentActivities(emailIntent, 0)
        val intents = arrayListOf<LabeledIntent>()
        for (info in resolveInfos) {
            val intent = Intent(Intent.ACTION_SEND)
            intent.component = ComponentName(info.activityInfo.packageName, info.activityInfo.name)
            intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(getString(R.string.email)))
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) +" - Bug report")
            intent.putExtra(Intent.EXTRA_TEXT, "[how did this happen?]")
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intents.add(LabeledIntent(intent, info.activityInfo.packageName, info.loadLabel(activity.packageManager), info.icon))
        }
        if (intents.size > 0) {
            val chooser = Intent.createChooser(intents.removeAt(intents.size - 1), "Send bug report")
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toTypedArray())
            startActivity(chooser)
        }else
            Stuff.toast(activity, "There are no email clients installed.")
    }

    fun test(){
        OfflineScrobbleJob.checkAndSchedule(activity)
    }
    override fun onHiddenChanged(hidden: Boolean) {
        val ab = (activity as AppCompatActivity).supportActionBar
        ab?.setDisplayHomeAsUpEnabled(hidden)
        if (!hidden) {
            activity.findViewById<CollapsingToolbarLayout>(R.id.toolbar_layout)?.title = getString(R.string.app_name)
        }
    }

    override fun onPause() {
        super.onPause()
        val list = activity.findViewById<ListView>(R.id.recents_list)
        list?.removeCallbacks(RecentsAdapter.timedRefresh)
    }

    override fun onResume() {
        super.onResume()
        adapter?.loadRecents(1)
        adapter?.loadPending()
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
