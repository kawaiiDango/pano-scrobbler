package com.arn.scrobble.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.main.MainDialogActivity
import com.arn.scrobble.pref.SpecificWidgetPrefs
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.format
import com.arn.scrobble.utils.Stuff.putData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

object ChartsListUtils {

    fun createHeader(periodName: String): RemoteViews {
        val headerView =
            RemoteViews(PlatformStuff.application.packageName, R.layout.appwidget_list_header)
        headerView.setTextViewText(R.id.appwidget_period, periodName)
        return headerView
    }

    fun createMusicItem(tab: Int, idx: Int, item: ChartsWidgetListItem): RemoteViews {
        val rv = RemoteViews(PlatformStuff.application.packageName, R.layout.appwidget_charts_item)
        rv.setTextViewText(
            R.id.appwidget_charts_serial, (idx + 1).format() + "."
        )
        rv.setTextViewText(R.id.appwidget_charts_title, item.title)
        rv.setImageViewResource(
            R.id.appwidget_charts_stonks_icon, Stuff.stonksIconForDelta(item.stonksDelta)
        )
        rv.setImageViewResource(
            R.id.appwidget_charts_stonks_icon_shadow, Stuff.stonksIconForDelta(item.stonksDelta)
        )
        rv.setContentDescription(R.id.appwidget_charts_stonks_icon, item.stonksDelta.toString())

        if (item.subtitle != null) {
            rv.setTextViewText(R.id.appwidget_charts_subtitle, item.subtitle)
            rv.setViewVisibility(R.id.appwidget_charts_subtitle, View.VISIBLE)
        } else rv.setViewVisibility(R.id.appwidget_charts_subtitle, View.GONE)

        rv.setTextViewText(
            R.id.appwidget_charts_plays, item.number.format()
        )
        // Next, we set a fill-intent which will be used to fill-in the pending intent template
        // which is set on the collection view in StackWidgetProvider.
        val navArgs = Bundle()
        when (tab) {
            Stuff.TYPE_ARTISTS -> {
                navArgs.putData(Artist(item.title))
            }

            Stuff.TYPE_ALBUMS -> {
                if (item.subtitle != null)
                    navArgs.putData(Album(item.title, Artist(item.subtitle)))
            }

            Stuff.TYPE_TRACKS -> {
                if (item.subtitle != null)
                    navArgs.putData(Track(item.title, null, Artist(item.subtitle)))
            }
        }
        val fillInIntent = Intent().putExtra(MainDialogActivity.ARG_NAV_ARGS, navArgs)

        rv.setOnClickFillInIntent(R.id.appwidget_charts_item, fillInIntent)
        return rv
    }

    fun readList(prefs: SpecificWidgetPrefs): List<ChartsWidgetListItem> {
        val tab = prefs.tab
        val period = prefs.period

        val chartsData = runBlocking {
            Stuff.widgetPrefs.data.map {
                it.chartsData[period]?.get(tab)
            }
                .first()
        } ?: emptyList()

        return chartsData
    }

    fun updateWidgets(appWidgetIds: IntArray) {
        val i = Intent(PlatformStuff.application, ChartsWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        }
        PlatformStuff.application.sendBroadcast(i)
    }
}