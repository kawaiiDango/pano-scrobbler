package com.arn.scrobble.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.core.net.toUri
import com.arn.scrobble.R
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.navigation.DeepLinkUtils
import com.arn.scrobble.navigation.PanoDialog
import com.arn.scrobble.pref.SpecificWidgetPrefs
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.format
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

object ChartsListUtils {

    fun createHeader(periodName: String): RemoteViews {
        val headerView =
            RemoteViews(AndroidStuff.application.packageName, R.layout.appwidget_list_header)
        headerView.setTextViewText(R.id.appwidget_period, periodName)
        return headerView
    }

    fun createMusicItem(
        tab: Int,
        idx: Int,
        item: ChartsWidgetListItem,
        user: UserCached,
    ): RemoteViews {
        val rv = RemoteViews(AndroidStuff.application.packageName, R.layout.appwidget_charts_item)
        rv.setTextViewText(
            R.id.appwidget_charts_serial, (idx + 1).format() + "."
        )
        rv.setTextViewText(R.id.appwidget_charts_title, item.title)
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

        var deepLinkUri: String? = null

        when (tab) {
            Stuff.TYPE_ARTISTS -> {
                val artist = Artist(item.title)
                deepLinkUri = DeepLinkUtils.buildDeepLink(
                    PanoDialog.MusicEntryInfo(
                        artist = artist,
                        user = user
                    )
                )
            }

            Stuff.TYPE_ALBUMS -> {
                if (item.subtitle != null) {
                    val album = Album(item.title, Artist(item.subtitle))

                    deepLinkUri = DeepLinkUtils.buildDeepLink(
                        PanoDialog.MusicEntryInfo(
                            album = album,
                            user = user
                        )
                    )
                }
            }

            Stuff.TYPE_TRACKS -> {
                if (item.subtitle != null) {
                    val track = Track(item.title, null, Artist(item.subtitle))

                    deepLinkUri = DeepLinkUtils.buildDeepLink(
                        PanoDialog.MusicEntryInfo(
                            track = track,
                            user = user
                        )
                    )
                }
            }
        }

        if (deepLinkUri != null) {
            val fillInIntent = Intent().setData(deepLinkUri.toUri())
            rv.setOnClickFillInIntent(R.id.appwidget_charts_item, fillInIntent)
        }

        return rv
    }

    fun readList(prefs: SpecificWidgetPrefs): List<ChartsWidgetListItem> {
        val tab = prefs.tab
        val period = prefs.period

        val chartsData = runBlocking {
            AndroidStuff.widgetPrefs.data.map {
                it.chartsData[period]?.get(tab)
            }
                .first()
        } ?: emptyList()

        return chartsData
    }

    fun updateWidgets(appWidgetIds: IntArray) {
        val i = Intent(AndroidStuff.application, ChartsWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        }
        AndroidStuff.application.sendBroadcast(i)
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