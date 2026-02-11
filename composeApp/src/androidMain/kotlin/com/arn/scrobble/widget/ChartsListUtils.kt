package com.arn.scrobble.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.arn.scrobble.R
import com.arn.scrobble.navigation.DeepLinkUtils
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.format

object ChartsListUtils {

    fun createHeader(periodName: String): RemoteViews {
        val headerView =
            RemoteViews(AndroidStuff.applicationContext.packageName, R.layout.appwidget_list_header)
        headerView.setTextViewText(R.id.appwidget_period, periodName)
        return headerView
    }

    fun createMusicItem(
        tab: Int,
        idx: Int,
        item: ChartsWidgetListItem,
    ): RemoteViews {
        val rv =
            RemoteViews(AndroidStuff.applicationContext.packageName, R.layout.appwidget_charts_item)
//        rv.setTextViewText(
//            R.id.appwidget_charts_serial, (idx + 1).format() + "."
//        )
        rv.setTextViewText(R.id.appwidget_charts_title, (idx + 1).format() + ". " + item.title)
        rv.setImageViewResource(
            R.id.appwidget_charts_stonks_icon, stonksIconForDelta(item.stonksDelta)
        )
        rv.setImageViewResource(
            R.id.appwidget_charts_stonks_icon_shadow, stonksIconForDelta(item.stonksDelta)
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

        val fillInIntent = Intent()

        when (tab) {
            Stuff.TYPE_ARTISTS -> {
                DeepLinkUtils.fillInIntentForMusicEntryInfo(
                    fillInIntent,
                    artist = item.title,
                    album = null,
                    track = null,
                )
            }

            Stuff.TYPE_ALBUMS -> {
                if (item.subtitle != null) {
                    DeepLinkUtils.fillInIntentForMusicEntryInfo(
                        fillInIntent,
                        artist = item.subtitle,
                        album = item.title,
                        track = null,
                    )
                }
            }

            Stuff.TYPE_TRACKS -> {
                if (item.subtitle != null) {
                    DeepLinkUtils.fillInIntentForMusicEntryInfo(
                        fillInIntent,
                        artist = item.subtitle,
                        album = null,
                        track = item.title,
                    )
                }
            }
        }

        rv.setOnClickFillInIntent(R.id.appwidget_charts_item, fillInIntent)

        return rv
    }

    fun updateWidgets(appWidgetIds: IntArray) {
        val i = Intent(AndroidStuff.applicationContext, ChartsWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        }
        AndroidStuff.applicationContext.sendBroadcast(i)
    }

    fun stonksIconForDelta(delta: Int?) = when {
        delta == null -> 0
        delta == Int.MAX_VALUE -> R.drawable.vd_stonks_new
        delta in 1..5 -> R.drawable.vd_stonks_up
        delta > 5 -> R.drawable.vd_stonks_up_double
        delta in -1 downTo -5 -> R.drawable.vd_stonks_down
        delta < -5 -> R.drawable.vd_stonks_down_double
        delta == 0 -> R.drawable.vd_stonks_no_change
        else -> 0
    }
}