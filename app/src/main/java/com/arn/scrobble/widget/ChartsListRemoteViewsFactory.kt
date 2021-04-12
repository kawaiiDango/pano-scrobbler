package com.arn.scrobble.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import java.text.NumberFormat
import java.util.ArrayList

class ChartsListRemoteViewsFactory(private val context: Context, intent: Intent) :
    RemoteViewsService.RemoteViewsFactory {
    private val widgetItems = mutableListOf<ChartsWidgetListItem>()
    private val appWidgetId = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )

    override fun onCreate() {
        // In onCreate() you setup any connections / cursors to your data source. Heavy lifting,
        // for example downloading or creating content etc, should be deferred to onDataSetChanged()
        // or getViewAt(). Taking more than 20 seconds in this call will result in an ANR.

        // We sleep for 3 seconds here to show how the empty view appears in the interim.
        // The empty view is set in the StackWidgetProvider and should be a sibling of the
        // collection view.

        val pref = context.getSharedPreferences(Stuff.WIDGET_PREFS, Context.MODE_PRIVATE)
        val lastUpdated = pref.getLong(
            Stuff.getWidgetPrefName(
                Stuff.PREF_WIDGET_LAST_UPDATED,
                appWidgetId
            ), -1)

        ChartsWidgetUpdaterJob.checkAndSchedule(context, lastUpdated == -1L)
    }

    override fun onDestroy() {
        // In onDestroy() you should tear down anything that was setup for your data source,
        // eg. cursors, connections, etc.
        widgetItems.clear()
    }

    override fun getCount() = widgetItems.size

    override fun getViewAt(position: Int): RemoteViews {
        // position will always range from 0 to getCount() - 1.
        // We construct a remote views item based on our widget item xml file, and set the
        // text based on the position.
        val rv = RemoteViews(context.packageName, R.layout.appwidget_charts_item)
        rv.setTextViewText(R.id.appwidget_charts_serial, NumberFormat.getInstance().format(position + 1) + ".")
        rv.setTextViewText(R.id.appwidget_charts_title, widgetItems[position].title)

        if (widgetItems[position].subtitle != "") {
            rv.setTextViewText(R.id.appwidget_charts_subtitle, widgetItems[position].subtitle)
            rv.setViewVisibility(R.id.appwidget_charts_subtitle, View.VISIBLE)
        } else
            rv.setViewVisibility(R.id.appwidget_charts_subtitle, View.GONE)

        rv.setTextViewText(
            R.id.appwidget_charts_plays,
            NumberFormat.getInstance().format(widgetItems[position].number))
        // Next, we set a fill-intent which will be used to fill-in the pending intent template
        // which is set on the collection view in StackWidgetProvider.
//        Bundle extras = new Bundle();
//        extras.putInt(StackWidgetProvider.EXTRA_ITEM, position);
//        Intent fillInIntent = new Intent();
//        fillInIntent.putExtras(extras);
//        rv.setOnClickFillInIntent(R.id.widget_item, fillInIntent);
        // You can do heaving lifting in here, synchronously. For example, if you need to
        // process an image, fetch something from the network, etc., it is ok to do it here,
        // synchronously. A loading view will show up in lieu of the actual contents in the
        // interim.
        // Return the remote views object.
        return rv
    }

    override fun getLoadingView(): RemoteViews? {
        // You can create a custom loading view (for instance when getViewAt() is slow.) If you
        // return null here, you will get the default loading view.
        return null
    }

    override fun getViewTypeCount() = 1

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
        widgetItems.clear()
        val pref = context.getSharedPreferences(Stuff.WIDGET_PREFS, Context.MODE_PRIVATE)

        val tab = pref.getInt(
            Stuff.getWidgetPrefName(Stuff.PREF_WIDGET_TAB, appWidgetId),
            Stuff.TYPE_ARTISTS
        )
        val period = pref.getInt(Stuff.getWidgetPrefName(Stuff.PREF_WIDGET_PERIOD, appWidgetId), -1)

        if (period == -1)
            return

        val list = ObjectSerializeHelper.convertFrom<ArrayList<ChartsWidgetListItem>>(
            pref.getString("${tab}_$period", null)
        ) ?: arrayListOf()
        widgetItems.addAll(list)
    }
}