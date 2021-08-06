package com.arn.scrobble.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.databinding.*

class ChartsWidgetActivity: AppCompatActivity() {
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

    lateinit var binding: ActivityAppwidgetChartsConfigBinding
    lateinit var previewBinding: AppwidgetChartsContentBinding
    private val pref by lazy { getSharedPreferences(Stuff.WIDGET_PREFS, Context.MODE_PRIVATE) }
    private val periodChipIds = arrayOf(R.id.charts_7day, R.id.charts_1month, R.id.charts_3month, R.id.charts_6month, R.id.charts_12month, R.id.charts_overall)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAppwidgetChartsConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setResult(false)
        val editor = pref.edit()
        binding.widgetPeriod.chartsChooseWeek.visibility = View.GONE
        binding.widgetPeriod.charts7day.text = resources.getQuantityString(R.plurals.num_weeks, 1, 1)
        binding.widgetPeriod.charts1month.text = resources.getQuantityString(R.plurals.num_months, 1, 1)
        binding.widgetPeriod.charts3month.text = resources.getQuantityString(R.plurals.num_months, 3, 3)
        binding.widgetPeriod.charts6month.text = resources.getQuantityString(R.plurals.num_months, 6, 6)
        binding.widgetPeriod.charts12month.text = resources.getQuantityString(R.plurals.num_years, 1, 1)
        binding.widgetPeriod.chartsOverall.text = getString(R.string.charts_overall)

        binding.widgetPeriod.chartsPeriod.setOnCheckedChangeListener { group, checkedId ->
            val idx = periodChipIds.indexOf(checkedId)
            if (idx != -1)
                editor.putInt(Stuff.getWidgetPrefName(Stuff.PREF_WIDGET_PERIOD, appWidgetId), idx)
        }

        binding.widgetPeriod.charts7day.isChecked = true

        binding.widgetTheme.setOnCheckedChangeListener { group, checkedId ->
            val isDark = checkedId == R.id.chip_dark
            editor.putBoolean(Stuff.getWidgetPrefName(Stuff.PREF_WIDGET_DARK, appWidgetId), isDark)
            initPreview(isDark, binding.widgetShadow.isChecked)
            previewAlpha(binding.widgetBgAlpha.value/100)

        }

        binding.widgetBgAlpha.addOnChangeListener { slider, value, fromUser ->
            editor.putFloat(Stuff.getWidgetPrefName(Stuff.PREF_WIDGET_BG_ALPHA, appWidgetId),
                value/100)
            previewAlpha(value/100)
        }

        binding.widgetShadow.setOnCheckedChangeListener { compoundButton, checked ->
            editor.putBoolean(Stuff.getWidgetPrefName(Stuff.PREF_WIDGET_SHADOW, appWidgetId),
                checked)
            initPreview(binding.widgetTheme.checkedChipId == R.id.chip_dark, checked)
            previewAlpha(binding.widgetBgAlpha.value/100)
        }

        binding.okButton.setOnClickListener {
            setResult(true)
            val idx = periodChipIds.indexOf(binding.widgetPeriod.chartsPeriod.checkedChipId)
            if (idx != -1)
                editor.putInt(Stuff.getWidgetPrefName(Stuff.PREF_WIDGET_PERIOD, appWidgetId), idx)
            editor.apply()
            updateWidget()
            finish()
        }
        binding.cancelButton.setOnClickListener {
            finish()
        }

        if (isPinned)
            binding.cancelButton.visibility = View.GONE

        initPreview(true, true)
        previewAlpha(binding.widgetBgAlpha.value/100)
    }

    override fun onDestroy() {
        if (isPinned)
            binding.okButton.callOnClick()
        super.onDestroy()
    }

    private fun setResult(positive: Boolean) {
        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(if (positive)
                Activity.RESULT_OK
            else
                Activity.RESULT_CANCELED
            , resultValue)

    }

    private fun initPreview(isDark: Boolean, hasShadow: Boolean) {
        val b: ViewBinding
        when {
            isDark && hasShadow ->{
                b = AppwidgetChartsDarkShadowBinding.inflate(layoutInflater)
                previewBinding = b.appwidgetOuterFrame
            }
            isDark && !hasShadow -> {
                b = AppwidgetChartsDarkBinding.inflate(layoutInflater)
                previewBinding = b.appwidgetOuterFrame
            }
            !isDark && hasShadow -> {
                b = AppwidgetChartsLightShadowBinding.inflate(layoutInflater)
                previewBinding = b.appwidgetOuterFrame
            }
            !isDark && !hasShadow -> {
                b = AppwidgetChartsLightBinding.inflate(layoutInflater)
                previewBinding = b.appwidgetOuterFrame
            }
            else -> throw Exception()
        }
            binding.widgetPreviewFrame.removeAllViews()
            binding.widgetPreviewFrame.addView(b.root)
        previewBinding.appwidgetList.emptyView = previewBinding.appwidgetStatus
        previewBinding.appwidgetList.adapter = FakeChartsAdapter(this, isDark, hasShadow)
    }

    private fun previewAlpha(alpha: Float) {
        previewBinding.appwidgetBg.alpha = alpha
    }

    private fun updateWidget() {
        val i = Intent(this, ChartsWidgetProvider::class.java)
        i.action = NLService.iUPDATE_WIDGET
        i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        i.data = Uri.parse(i.toUri(Intent.URI_INTENT_SCHEME))
        sendBroadcast(i)
    }
}