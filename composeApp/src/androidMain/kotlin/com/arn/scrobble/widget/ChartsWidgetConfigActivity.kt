package com.arn.scrobble.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.arn.scrobble.pref.SpecificWidgetPrefs
import com.arn.scrobble.themes.AppTheme
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.applyAndroidLocaleLegacy
import kotlinx.coroutines.launch

class ChartsWidgetConfigActivity : ComponentActivity() {
    private val appWidgetId by lazy {
        intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
    }
    private val isPinned by lazy {
        intent?.extras?.getBoolean(
            Stuff.EXTRA_PINNED,
            false
        ) ?: false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                val widgetPrefs by
                AndroidStuff.widgetPrefs.data.collectAsStateWithLifecycle(null)

                ChartsWidgetConfigScreen(
                    isPinned = isPinned,
                    prefs = widgetPrefs?.widgets[appWidgetId] ?: SpecificWidgetPrefs(),
                    onSave = ::savePrefs,
                    onCancel = ::cancel
                )
            }
        }
    }

    private fun savePrefs(prefs: SpecificWidgetPrefs, reFetch: Boolean) {
        lifecycleScope.launch {
            var exists = false

            AndroidStuff.widgetPrefs.updateData {
                exists = it.widgets.containsKey(appWidgetId)
                it.copy(widgets = it.widgets + (appWidgetId to prefs))
            }

            ChartsListUtils.updateWidgets(intArrayOf(appWidgetId))

            if (!exists || reFetch)
                ChartsWidgetUpdaterWorker.checkAndSchedule(
                    this@ChartsWidgetConfigActivity.applicationContext,
                    true
                )
            setResult(RESULT_OK, Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            })

            setResult(true)
            finish()
        }
    }

    private fun cancel() {
        setResult(false)
        finish()
    }

    private fun setResult(positive: Boolean) {
        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(
            if (positive) RESULT_OK else RESULT_CANCELED,
            resultValue
        )

    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase ?: return)
        applyAndroidLocaleLegacy()
    }
}