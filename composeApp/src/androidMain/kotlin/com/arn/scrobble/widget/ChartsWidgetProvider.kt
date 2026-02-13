package com.arn.scrobble.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.widget.RemoteViewsCompat
import com.arn.scrobble.R
import com.arn.scrobble.main.MainDialogActivity
import com.arn.scrobble.pref.SpecificWidgetPrefs
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Objects


class ChartsWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val prefs = runBlocking {
            AndroidStuff.widgetPrefs.data.first()
        }

        // There may be multiple widgets active, so update all of them
        appWidgetIds.forEach { appWidgetId ->
            val specificWidgetPrefs = prefs.widgets[appWidgetId] ?: return
            val chartsData = prefs.charts[specificWidgetPrefs.period]
            val specificChartsData = when (specificWidgetPrefs.tab) {
                Stuff.TYPE_ARTISTS -> chartsData?.artists
                Stuff.TYPE_ALBUMS -> chartsData?.albums
                Stuff.TYPE_TRACKS -> chartsData?.tracks
                else -> null
            }
            val timePeriodString = chartsData?.timePeriodString

            updateAppWidget(
                context,
                appWidgetManager,
                appWidgetId,
                specificWidgetPrefs,
                specificChartsData,
                timePeriodString
            )
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
        ChartsWidgetUpdaterWorker.cancel(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        runBlocking {
            AndroidStuff.widgetPrefs.updateData {
                it.copy(widgets = it.widgets.toMutableMap().minus(appWidgetIds.toSet()))
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {

        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE == intent.action) {
            val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS) ?: intArrayOf()

            ids.filter { it != AppWidgetManager.INVALID_APPWIDGET_ID }
                .forEach { appWidgetId ->
                    val tab = intent.getIntExtra(ChartsListUtils.EXTRA_TAB, -1)

                    if (tab != -1) {
                        runBlocking {
                            AndroidStuff.widgetPrefs.updateData { prefs ->
                                val widgets = prefs.widgets.toMutableMap()
                                val specificWidgetPrefs =
                                    widgets[appWidgetId] ?: SpecificWidgetPrefs()
                                widgets[appWidgetId] = specificWidgetPrefs.copy(tab = tab)
                                prefs.copy(widgets = widgets)
                            }
                        }
                    }
                }
        }

        super.onReceive(context, intent)
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    prefs: SpecificWidgetPrefs,
    specificChartsData: List<ChartsWidgetListItem>?,
    timePeriodString: String?,
    scrollToTop: Boolean = false,
) {

    val tab = prefs.tab
    val bgAlpha = prefs.bgAlpha
    val hasShadow = prefs.shadow

    val layoutId = if (hasShadow)
        R.layout.appwidget_charts_dynamic_shadow
    else
        R.layout.appwidget_charts_dynamic

    val rv = RemoteViews(context.packageName, layoutId)

    val items = RemoteViewsCompat.RemoteCollectionItems.Builder().apply {
        setHasStableIds(true)
        setViewTypeCount(2)
        addItem(0, ChartsListUtils.createHeader(timePeriodString ?: ""))

        specificChartsData
            ?.forEachIndexed { i, item ->
                addItem(
                    item.hashCode().toLong(),
                    ChartsListUtils.createMusicItem(tab, i, item)
                )
            }
    }.build()
    RemoteViewsCompat.setRemoteAdapter(context, rv, appWidgetId, R.id.appwidget_list, items)
    // The empty view is displayed when the collection has no items. It should be a sibling
    // of the collection view.
    rv.setEmptyView(R.id.appwidget_list, R.id.appwidget_status)

    if (scrollToTop)
        rv.setScrollPosition(R.id.appwidget_list, 0)

    if (specificChartsData == null)
        rv.setInt(R.id.appwidget_status, "setText", R.string.appwidget_loading)
    else {
        val text = context.getString(R.string.charts_no_data) + "\n\n" +
                timePeriodString
        rv.setTextViewText(R.id.appwidget_status, text)
    }


    // Here we set up the pending intent template. Individuals items of a collection
    // cannot set up their own pending intents, instead, the collection as a whole can
    // set up a pending intent template, and the individual items can set a fillInIntent
    // to create unique before on an item to item basis.
    val infoIntent = Intent(context, MainDialogActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
    }

    val infoPendingIntent = PendingIntent.getActivity(
        context, Objects.hash(appWidgetId, -10), infoIntent,
        AndroidStuff.updateCurrentOrMutable
    )
    rv.setPendingIntentTemplate(R.id.appwidget_list, infoPendingIntent)

    val tabIntent = Intent(context, ChartsWidgetProvider::class.java)
    tabIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
    tabIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))

    tabIntent.putExtra(ChartsListUtils.EXTRA_TAB, Stuff.TYPE_ARTISTS)
    var tabIntentPending = PendingIntent.getBroadcast(
        context, Objects.hash(appWidgetId, 1), tabIntent,
        AndroidStuff.updateCurrentOrImmutable
    )
    rv.setOnClickPendingIntent(R.id.appwidget_artists, tabIntentPending)

    tabIntent.putExtra(ChartsListUtils.EXTRA_TAB, Stuff.TYPE_ALBUMS)
    tabIntentPending = PendingIntent.getBroadcast(
        context, Objects.hash(appWidgetId, 2), tabIntent,
        AndroidStuff.updateCurrentOrImmutable
    )
    rv.setOnClickPendingIntent(R.id.appwidget_albums, tabIntentPending)

    tabIntent.putExtra(ChartsListUtils.EXTRA_TAB, Stuff.TYPE_TRACKS)
    tabIntentPending = PendingIntent.getBroadcast(
        context, Objects.hash(appWidgetId, 3), tabIntent,
        AndroidStuff.updateCurrentOrImmutable
    )
    rv.setOnClickPendingIntent(R.id.appwidget_tracks, tabIntentPending)

    rv.setInt(R.id.appwidget_bg, "setImageAlpha", (bgAlpha * 255).toInt())

    val tabIds =
        arrayOf(R.id.appwidget_tracks, R.id.appwidget_albums, R.id.appwidget_artists)
    val tabShadowIds = arrayOf(
        R.id.appwidget_tracks_shadow, R.id.appwidget_albums_shadow, R.id.appwidget_artists_shadow
    )
    val selectedTabIdx = when (tab) {
        Stuff.TYPE_TRACKS -> 0
        Stuff.TYPE_ALBUMS -> 1
        else -> 2
    }
    (0..2).forEach { idx ->
        val tabAlpha = if (idx == selectedTabIdx)
            1f
        else
            0.55f
        rv.setInt(tabIds[idx], "setImageAlpha", (tabAlpha * 255).toInt())
        rv.setInt(tabShadowIds[idx], "setImageAlpha", (tabAlpha * 255).toInt())
    }

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, rv)
}