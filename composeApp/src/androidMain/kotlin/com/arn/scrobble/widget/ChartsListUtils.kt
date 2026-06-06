package com.arn.scrobble.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import androidx.core.widget.RemoteViewsCompat.setViewBackgroundResource
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.R
import com.arn.scrobble.navigation.DeepLinkUtils
import com.arn.scrobble.pref.WidgetPrefs
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.format

object ChartsListUtils {
    const val EXTRA_TAB = "widget_tab"

    fun createHeader(periodName: String): RemoteViews {
        val headerView =
            RemoteViews(AndroidStuff.applicationContext.packageName, R.layout.appwidget_list_header)
        headerView.setTextViewText(R.id.appwidget_period, periodName)
        return headerView
    }

    fun createMusicItem(
        widgetId: Int,
        dataKey: WidgetPrefs.ChartsDataKey,
        tab: Int,
        images: Boolean,
        idx: Int,
        item: WidgetPrefs.ChartsWidgetListItem,
    ): RemoteViews {

        fun buildImageUri() =
            Uri.Builder()
                .scheme("content")
                .authority(BuildKonfig.APP_ID + ".image")
                .appendPath("image")
                .appendPath(widgetId.toString())
                .appendPath(dataKey.str)
                .appendQueryParameter(
                    "item",
                    Stuff.myJson.encodeToString(item)
                )
                .build()

        val rv =
            RemoteViews(AndroidStuff.applicationContext.packageName, R.layout.appwidget_charts_item)
        rv.setTextViewText(R.id.appwidget_charts_title, item.title)

        if (item.stonksDelta != null) {
            rv.setImageViewResource(
                R.id.appwidget_charts_stonks_icon, stonksIconForDelta(item.stonksDelta)
            )
            rv.setImageViewResource(
                R.id.appwidget_charts_stonks_icon_shadow, stonksIconForDelta(item.stonksDelta)
            )
            rv.setContentDescription(R.id.appwidget_charts_stonks_icon, item.stonksDelta.toString())
            rv.setViewVisibility(R.id.appwidget_charts_stonks_icon, View.VISIBLE)
            rv.setViewVisibility(R.id.appwidget_charts_stonks_icon_shadow, View.VISIBLE)
        } else {
            rv.setViewVisibility(R.id.appwidget_charts_stonks_icon, View.GONE)
            rv.setViewVisibility(R.id.appwidget_charts_stonks_icon_shadow, View.GONE)
        }

        if (images) {
            rv.setViewVisibility(R.id.appwidget_charts_image, View.VISIBLE)
//            rv.setViewBackgroundResource(
//                R.id.appwidget_charts_serial,
//                R.drawable.widget_bg_rounded_soild
//            )
//            rv.setTextViewText(R.id.appwidget_charts_serial, (idx + 1).format())
            rv.setViewVisibility(R.id.appwidget_charts_serial, View.GONE)

            if (item.cachedImage != null && item.cachedImage.isEmpty()) {
                rv.setImageViewResource(R.id.appwidget_charts_image, R.drawable.vd_album)
            } else {
                rv.setImageViewUri(R.id.appwidget_charts_image, buildImageUri())
            }
        } else {
            rv.setViewVisibility(R.id.appwidget_charts_image, View.GONE)
            rv.setViewVisibility(R.id.appwidget_charts_serial, View.VISIBLE)
            rv.setViewBackgroundResource(
                R.id.appwidget_charts_serial,
                0
            )
            rv.setTextViewText(R.id.appwidget_charts_serial, (idx + 1).format())
        }

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