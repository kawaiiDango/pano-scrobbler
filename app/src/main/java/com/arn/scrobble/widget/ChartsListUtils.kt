package com.arn.scrobble.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.arn.scrobble.App
import com.arn.scrobble.MainDialogActivity
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.pref.WidgetPrefs
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.text.NumberFormat

object ChartsListUtils {

    fun createHeader(prefs: WidgetPrefs.SpecificWidgetPrefs): RemoteViews {
        val headerView = RemoteViews(App.context.packageName, R.layout.appwidget_list_header)
        headerView.setTextViewText(R.id.appwidget_period, prefs.periodName)
        return headerView
    }

    fun createMusicItem(tab: Int, idx: Int, item: ChartsWidgetListItem): RemoteViews {
        val rv = RemoteViews(App.context.packageName, R.layout.appwidget_charts_item)
        rv.setTextViewText(
            R.id.appwidget_charts_serial, NumberFormat.getInstance().format(idx + 1) + "."
        )
        rv.setTextViewText(R.id.appwidget_charts_title, item.title)
        rv.setImageViewResource(
            R.id.appwidget_charts_stonks_icon, Stuff.stonksIconForDelta(item.stonksDelta)
        )
        rv.setImageViewResource(
            R.id.appwidget_charts_stonks_icon_shadow, Stuff.stonksIconForDelta(item.stonksDelta)
        )
        rv.setContentDescription(R.id.appwidget_charts_stonks_icon, item.stonksDelta.toString())

        if (item.subtitle != "") {
            rv.setTextViewText(R.id.appwidget_charts_subtitle, item.subtitle)
            rv.setViewVisibility(R.id.appwidget_charts_subtitle, View.VISIBLE)
        } else rv.setViewVisibility(R.id.appwidget_charts_subtitle, View.GONE)

        rv.setTextViewText(
            R.id.appwidget_charts_plays, NumberFormat.getInstance().format(item.number)
        )
        // Next, we set a fill-intent which will be used to fill-in the pending intent template
        // which is set on the collection view in StackWidgetProvider.
        val navArgs = Bundle()
        when (tab) {
            Stuff.TYPE_ARTISTS -> {
                navArgs.putString(NLService.B_ARTIST, item.title)
            }

            Stuff.TYPE_ALBUMS -> {
                navArgs.putString(NLService.B_ARTIST, item.subtitle)
                navArgs.putString(NLService.B_ALBUM, item.title)
            }

            Stuff.TYPE_TRACKS -> {
                navArgs.putString(NLService.B_ARTIST, item.subtitle)
                navArgs.putString(NLService.B_TRACK, item.title)
            }
        }
        val fillInIntent = Intent().putExtra(MainDialogActivity.ARG_NAV_ARGS, navArgs)

        rv.setOnClickFillInIntent(R.id.appwidget_charts_item, fillInIntent)
        return rv
    }

    fun readList(prefs: WidgetPrefs.SpecificWidgetPrefs): ArrayList<ChartsWidgetListItem> {
        val tab = prefs.tab ?: Stuff.TYPE_ARTISTS
        val period = prefs.period ?: return arrayListOf()

        return kotlin.runCatching {
            Json.decodeFromString<ArrayList<ChartsWidgetListItem>>(
                prefs.sharedPreferences.getString("${tab}_$period", null)!!
            )
        }.getOrDefault(arrayListOf())
    }


    fun updateWidget(appWidgetId: Int) {
        val i = Intent(App.context, ChartsWidgetProvider::class.java).apply {
            action = NLService.iUPDATE_WIDGET
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        App.context.sendBroadcast(i)
    }
}