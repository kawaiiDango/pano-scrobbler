package com.arn.scrobble.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.core.widget.RemoteViewsCompat
import com.arn.scrobble.R
import com.arn.scrobble.main.MainDialogActivity
import com.arn.scrobble.pref.WidgetPrefs
import com.arn.scrobble.pref.WidgetTheme
import com.arn.scrobble.utils.Stuff
import java.util.Objects


class ChartsWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val prefs = WidgetPrefs(context)

        // There may be multiple widgets active, so update all of them
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId, prefs[appWidgetId])
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
        val prefs = WidgetPrefs(context)
        appWidgetIds.forEach { prefs[it].clear() }

    }

    override fun onReceive(context: Context, intent: Intent) {

        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE == intent.action) {
            val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS) ?: intArrayOf()

            ids.filter { it != AppWidgetManager.INVALID_APPWIDGET_ID }
                .forEach { appWidgetId ->
                    val tab = intent.getIntExtra(WidgetPrefs.PREF_WIDGET_TAB, -1)
                    if (tab != -1)
                        WidgetPrefs(context)[appWidgetId].tab = tab
                }
        }

        super.onReceive(context, intent)
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    prefs: WidgetPrefs.SpecificWidgetPrefs,
    scrollToTop: Boolean = false
) {

    val tab = prefs.tab ?: Stuff.TYPE_ARTISTS
    val period = prefs.period
    val bgAlpha = prefs.bgAlpha
    val theme = WidgetTheme.entries[prefs.theme]
    val hasShadow = prefs.shadow

    val layoutId = when {
        theme == WidgetTheme.DARK && hasShadow -> R.layout.appwidget_charts_dark_shadow
        theme == WidgetTheme.DARK && !hasShadow -> R.layout.appwidget_charts_dark
        theme == WidgetTheme.LIGHT && hasShadow -> R.layout.appwidget_charts_light_shadow
        theme == WidgetTheme.LIGHT && !hasShadow -> R.layout.appwidget_charts_light
        theme == WidgetTheme.DYNAMIC && hasShadow -> R.layout.appwidget_charts_dynamic_shadow
        theme == WidgetTheme.DYNAMIC && !hasShadow -> R.layout.appwidget_charts_dynamic
        else -> R.layout.appwidget_charts_dark_shadow
    }
    val rv = RemoteViews(context.packageName, layoutId)

    if (period != null) {
        val items = RemoteViewsCompat.RemoteCollectionItems.Builder().apply {
            setHasStableIds(true)
            setViewTypeCount(2)
            addItem(0, ChartsListUtils.createHeader(prefs))

            ChartsListUtils.readList(prefs)
                .forEachIndexed { i, item ->
                    addItem(
                        Objects.hash(item.title, item.subtitle, tab).toLong(),
                        ChartsListUtils.createMusicItem(tab, i, item)
                    )
                }
        }.build()
        RemoteViewsCompat.setRemoteAdapter(context, rv, appWidgetId, R.id.appwidget_list, items)
    }
    // The empty view is displayed when the collection has no items. It should be a sibling
    // of the collection view.
    rv.setEmptyView(R.id.appwidget_list, R.id.appwidget_status)

    if (scrollToTop)
        rv.setScrollPosition(R.id.appwidget_list, 0)

    if (period == null || WidgetPrefs(context).chartsData(tab, period).dataJson == null)
        rv.setInt(R.id.appwidget_status, "setText", R.string.appwidget_loading)
    else {
        val text = context.getString(R.string.charts_no_data) + "\n\n" +
                prefs.periodName
        rv.setTextViewText(R.id.appwidget_status, text)
    }


    // Here we setup the a pending intent template. Individuals items of a collection
    // cannot setup their own pending intents, instead, the collection as a whole can
    // setup a pending intent template, and the individual items can set a fillInIntent
    // to create unique before on an item to item basis.
    val infoIntent = Intent(context, MainDialogActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
        putExtra(MainDialogActivity.ARG_DESTINATION, R.id.infoFragment)
    }

    val infoPendingIntent = PendingIntent.getActivity(
        context, Objects.hash(appWidgetId, -10), infoIntent,
        Stuff.updateCurrentOrMutable
    )
    rv.setPendingIntentTemplate(R.id.appwidget_list, infoPendingIntent)

    val tabIntent = Intent(context, ChartsWidgetProvider::class.java)
    tabIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
    tabIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))

    tabIntent.putExtra(WidgetPrefs.PREF_WIDGET_TAB, Stuff.TYPE_ARTISTS)
    var tabIntentPending = PendingIntent.getBroadcast(
        context, Objects.hash(appWidgetId, 1), tabIntent,
        Stuff.updateCurrentOrImmutable
    )
    rv.setOnClickPendingIntent(R.id.appwidget_artists, tabIntentPending)

    tabIntent.putExtra(WidgetPrefs.PREF_WIDGET_TAB, Stuff.TYPE_ALBUMS)
    tabIntentPending = PendingIntent.getBroadcast(
        context, Objects.hash(appWidgetId, 2), tabIntent,
        Stuff.updateCurrentOrImmutable
    )
    rv.setOnClickPendingIntent(R.id.appwidget_albums, tabIntentPending)

    tabIntent.putExtra(WidgetPrefs.PREF_WIDGET_TAB, Stuff.TYPE_TRACKS)
    tabIntentPending = PendingIntent.getBroadcast(
        context, Objects.hash(appWidgetId, 3), tabIntent,
        Stuff.updateCurrentOrImmutable
    )
    rv.setOnClickPendingIntent(R.id.appwidget_tracks, tabIntentPending)

    rv.setInt(R.id.appwidget_bg, "setImageAlpha", (bgAlpha * 255).toInt())

    val tabShadowIds =
        arrayOf(R.id.appwidget_tracks_glow, R.id.appwidget_albums_glow, R.id.appwidget_artists_glow)
    val tabIndicatorShadowIds = arrayOf(
        R.id.appwidget_tracks_glow_shadow,
        R.id.appwidget_albums_glow_shadow,
        R.id.appwidget_artists_glow_shadow
    )
    val glowId = when (tab) {
        Stuff.TYPE_TRACKS -> R.id.appwidget_tracks_glow
        Stuff.TYPE_ALBUMS -> R.id.appwidget_albums_glow
        else -> R.id.appwidget_artists_glow
    }
    tabShadowIds.forEachIndexed { i, it ->
        val visibility = if (it == glowId)
            View.VISIBLE
        else
            View.INVISIBLE
        rv.setViewVisibility(it, visibility)
        rv.setViewVisibility(tabIndicatorShadowIds[i], visibility)
    }

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, rv)
}