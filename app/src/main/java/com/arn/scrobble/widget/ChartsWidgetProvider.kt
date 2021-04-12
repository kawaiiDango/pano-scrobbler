package com.arn.scrobble.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.arn.scrobble.R
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.view.View
import com.arn.scrobble.Main
import com.arn.scrobble.NLService
import com.arn.scrobble.Stuff


class ChartsWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pref = context.getSharedPreferences(Stuff.WIDGET_PREFS, Context.MODE_PRIVATE)

        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, pref)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
        ChartsWidgetUpdaterJob.cancel(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val pref = context.getSharedPreferences(Stuff.WIDGET_PREFS, Context.MODE_PRIVATE)
        pruneWidgetPrefs(pref, appWidgetIds)

    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Stuff.log("widget update " + intent.action)

        if (NLService.iUPDATE_WIDGET == intent.action) {
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID)
                return
            val appWidgetManager = AppWidgetManager.getInstance(context)

            val pref = context.getSharedPreferences(Stuff.WIDGET_PREFS, Context.MODE_PRIVATE)
            val tab = intent.getIntExtra(Stuff.PREF_WIDGET_TAB, -1)
            if (tab != -1) {
                pref.edit()
                    .putInt(Stuff.getWidgetPrefName(Stuff.PREF_WIDGET_TAB, appWidgetId), tab)
                    .apply()
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.appwidget_list)
            }
            updateAppWidget(context, appWidgetManager, appWidgetId, pref)
        }
    }
}

internal fun pruneWidgetPrefs(pref: SharedPreferences, appWidgetIds: IntArray) {
    val editor = pref.edit()
    pref.all.keys.toList().forEach { prefKey ->
        appWidgetIds.forEach { appWidgetId ->
            if (prefKey.endsWith("_$appWidgetId"))
                editor.remove(prefKey)
        }
    }
    editor.apply()
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    pref: SharedPreferences
) {

    // Here we setup the intent which points to the StackViewService which will
    // provide the views for this collection.
    // Here we setup the intent which points to the StackViewService which will
    // provide the views for this collection.

    val tab = pref.getInt(Stuff.getWidgetPrefName(Stuff.PREF_WIDGET_TAB, appWidgetId), Stuff.TYPE_ARTISTS)
    val period = pref.getInt(Stuff.getWidgetPrefName(Stuff.PREF_WIDGET_PERIOD, appWidgetId), -1)
    val bgAlpha = pref.getFloat(Stuff.getWidgetPrefName(Stuff.PREF_WIDGET_BG_ALPHA, appWidgetId), 0.5f)
    val darkMode = pref.getBoolean(Stuff.getWidgetPrefName(Stuff.PREF_WIDGET_DARK, appWidgetId), true)
    val shadow = pref.getBoolean(Stuff.getWidgetPrefName(Stuff.PREF_WIDGET_SHADOW, appWidgetId), true)

    val rv = RemoteViews(
        context.packageName,
        when {
            darkMode && shadow -> R.layout.appwidget_charts_dark_shadow
            darkMode && !shadow -> R.layout.appwidget_charts_dark
            !darkMode && shadow -> R.layout.appwidget_charts_light_shadow
            !darkMode && !shadow -> R.layout.appwidget_charts_light
            else -> R.layout.appwidget_charts_dark_shadow
        }
    )

    if (period != -1) {
        val intent = Intent(context, ChartsListService::class.java)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

        // When intents are compared, the extras are ignored, so we need to embed the extras
        // into the data so that the extras will not be ignored.
        // When intents are compared, the extras are ignored, so we need to embed the extras
        // into the data so that the extras will not be ignored.
        intent.data = Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME))

        rv.setRemoteAdapter(R.id.appwidget_list, intent)
    }
    // The empty view is displayed when the collection has no items. It should be a sibling
    // of the collection view.
    // The empty view is displayed when the collection has no items. It should be a sibling
    // of the collection view.
    rv.setEmptyView(R.id.appwidget_list, R.id.appwidget_status)

    if (pref.getString("${tab}_$period", null) == null)
        rv.setInt(R.id.appwidget_status, "setText", R.string.appwidget_loading)
    else
        rv.setInt(R.id.appwidget_status, "setText", R.string.charts_no_data)


    // Here we setup the a pending intent template. Individuals items of a collection
    // cannot setup their own pending intents, instead, the collection as a whole can
    // setup a pending intent template, and the individual items can set a fillInIntent
    // to create unique before on an item to item basis.
    // Here we setup the a pending intent template. Individuals items of a collection
    // cannot setup their own pending intents, instead, the collection as a whole can
    // setup a pending intent template, and the individual items can set a fillInIntent
    // to create unique before on an item to item basis.

    val tabIntent = Intent(context, ChartsWidgetProvider::class.java)
    tabIntent.action = NLService.iUPDATE_WIDGET
    tabIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

    tabIntent.putExtra(Stuff.PREF_WIDGET_TAB, Stuff.TYPE_ARTISTS)
    var tabIntentPending = PendingIntent.getBroadcast(
        context, Stuff.genHashCode(appWidgetId, 1), tabIntent,
        Stuff.updateCurrentOrImmutable
    )
    rv.setOnClickPendingIntent(R.id.appwidget_artists, tabIntentPending)

    tabIntent.putExtra(Stuff.PREF_WIDGET_TAB, Stuff.TYPE_ALBUMS)
    tabIntentPending = PendingIntent.getBroadcast(
        context, Stuff.genHashCode(appWidgetId, 2), tabIntent,
        Stuff.updateCurrentOrImmutable
    )
    rv.setOnClickPendingIntent(R.id.appwidget_albums, tabIntentPending)

    tabIntent.putExtra(Stuff.PREF_WIDGET_TAB, Stuff.TYPE_TRACKS)
    tabIntentPending = PendingIntent.getBroadcast(
        context, Stuff.genHashCode(appWidgetId, 3), tabIntent,
        Stuff.updateCurrentOrImmutable
    )
    rv.setOnClickPendingIntent(R.id.appwidget_tracks, tabIntentPending)

    rv.setOnClickPendingIntent(
        R.id.appwidget_open_app,
        PendingIntent.getActivity(context, Stuff.genHashCode(appWidgetId, 4),
            Intent(context, Main::class.java),
            Stuff.updateCurrentOrImmutable
        )
    )

    rv.setInt(R.id.appwidget_bg, "setImageAlpha", (bgAlpha*255).toInt())

    val tabShadowIds = arrayOf(R.id.appwidget_tracks_glow, R.id.appwidget_albums_glow, R.id.appwidget_artists_glow)
    val tabIndicatorShadowIds = arrayOf(R.id.appwidget_tracks_glow_shadow, R.id.appwidget_albums_glow_shadow, R.id.appwidget_artists_glow_shadow)
    val glowId = when(tab) {
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