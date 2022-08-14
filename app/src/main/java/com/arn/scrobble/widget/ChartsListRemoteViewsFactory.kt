package com.arn.scrobble.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.pref.WidgetPrefs
import de.umass.lastfm.Period
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.text.NumberFormat

class ChartsListRemoteViewsFactory(private val context: Context, intent: Intent) :
    RemoteViewsService.RemoteViewsFactory {
    private val widgetItems = mutableListOf<ChartsWidgetListItem>()
    private val appWidgetId = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )

    private val prefs = WidgetPrefs(context)[appWidgetId]

    private var tab = Stuff.TYPE_ARTISTS
    private var period = Period.WEEK.string

    override fun onCreate() {
        // In onCreate() you setup any connections / cursors to your data source. Heavy lifting,
        // for example downloading or creating content etc, should be deferred to onDataSetChanged()
        // or getViewAt(). Taking more than 20 seconds in this call will result in an ANR.

        // We sleep for 3 seconds here to show how the empty view appears in the interim.
        // The empty view is set in the StackWidgetProvider and should be a sibling of the
        // collection view.

        val prefs = WidgetPrefs(context)[appWidgetId]
        val lastUpdated = prefs.lastUpdated
        ChartsWidgetUpdaterJob.checkAndSchedule(context, lastUpdated == null)
    }

    override fun onDestroy() {
        // In onDestroy() you should tear down anything that was setup for your data source,
        // eg. cursors, connections, etc.
        widgetItems.clear()
    }

    override fun getCount() = if (widgetItems.isEmpty()) 0 else widgetItems.size + 1

    override fun getViewAt(position: Int): RemoteViews {
        // position will always range from 0 to getCount() - 1.
        // We construct a remote views item based on our widget item xml file, and set the
        // text based on the position.

        if (position == 0) {
            val headerView = RemoteViews(context.packageName, R.layout.appwidget_list_header)
            headerView.setTextViewText(R.id.appwidget_period, prefs.periodName)
//            val fillInIntent = Intent().putExtra(Stuff.DIRECT_OPEN_KEY, Stuff.DL_CHARTS)
//            headerView.setOnClickFillInIntent(R.id.appwidget_period, fillInIntent)
            return headerView
        }

        val idx = position - 1

        val item = widgetItems[idx]
        val rv = RemoteViews(context.packageName, R.layout.appwidget_charts_item)
        rv.setTextViewText(
            R.id.appwidget_charts_serial,
            NumberFormat.getInstance().format(idx + 1) + "."
        )
        rv.setTextViewText(R.id.appwidget_charts_title, item.title)
        rv.setImageViewResource(R.id.appwidget_charts_stonks_icon, Stuff.stonksIconForDelta(item.stonksDelta))
        rv.setImageViewResource(R.id.appwidget_charts_stonks_icon_shadow, Stuff.stonksIconForDelta(item.stonksDelta))
        rv.setContentDescription(R.id.appwidget_charts_stonks_icon, item.stonksDelta.toString())

        if (item.subtitle != "") {
            rv.setTextViewText(R.id.appwidget_charts_subtitle, item.subtitle)
            rv.setViewVisibility(R.id.appwidget_charts_subtitle, View.VISIBLE)
        } else
            rv.setViewVisibility(R.id.appwidget_charts_subtitle, View.GONE)

        rv.setTextViewText(
            R.id.appwidget_charts_plays,
            NumberFormat.getInstance().format(item.number)
        )
        // Next, we set a fill-intent which will be used to fill-in the pending intent template
        // which is set on the collection view in StackWidgetProvider.
        val fillInIntent = Intent().apply {
            when (tab) {
                Stuff.TYPE_ARTISTS -> {
                    putExtra(NLService.B_ARTIST, item.title)
                }
                Stuff.TYPE_ALBUMS -> {
                    putExtra(NLService.B_ARTIST, item.subtitle)
                    putExtra(NLService.B_ALBUM, item.title)
                }
                Stuff.TYPE_TRACKS -> {
                    putExtra(NLService.B_ARTIST, item.subtitle)
                    putExtra(NLService.B_TRACK, item.title)
                }
            }
        }
        rv.setOnClickFillInIntent(R.id.appwidget_charts_item, fillInIntent)
        // You can do heaving lifting in here, synchronously. For example, if you need to
        // process an image, fetch something from the network, etc., it is ok to do it here,
        // synchronously. A loading view will show up in lieu of the actual contents in the
        // interim.
        // Return the remote views object.
        return rv
    }

    override fun getLoadingView(): RemoteViews {
        // You can create a custom loading view (for instance when getViewAt() is slow.) If you
        // return null here, you will get the default loading view.
        return RemoteViews(context.packageName, R.layout.appwidget_list_loading)
    }

    override fun getViewTypeCount() = 2

    override fun getItemId(position: Int) = position.toLong()

    override fun hasStableIds() = true

    override fun onDataSetChanged() {
        readData()

        // This is triggered when you call AppWidgetManager notifyAppWidgetViewDataChanged
        // on the collection view corresponding to this factory. You can do heaving lifting in
        // here, synchronously. For example, if you need to process an image, fetch something
        // from the network, etc., it is ok to do it here, synchronously. The widget will remain
        // in its current state while work is being done here, so you don't need to worry about
        // locking up the widget.
    }

    private fun readData() {
        tab = prefs.tab ?: Stuff.TYPE_ARTISTS
        period = prefs.period ?: return

        val list = kotlin.runCatching {
            Json.decodeFromString<ArrayList<ChartsWidgetListItem>>(
                prefs.sharedPreferences.getString("${tab}_$period", null)!!
            )
        }.getOrDefault(arrayListOf())
        widgetItems.clear()
        widgetItems.addAll(list)
    }
}