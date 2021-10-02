package com.arn.scrobble.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.arn.scrobble.*
import com.arn.scrobble.LocaleUtils.getLocaleContextWrapper
import com.arn.scrobble.billing.BillingViewModel
import com.arn.scrobble.databinding.*
import com.arn.scrobble.pref.WidgetPrefs
import com.arn.scrobble.pref.WidgetTheme
import com.arn.scrobble.themes.ColorPatchUtils

class ChartsWidgetActivity : AppCompatActivity() {
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
    private val prefs by lazy { WidgetPrefs(this)[appWidgetId] }
    private val periodChipIds = arrayOf(
        R.id.charts_7day,
        R.id.charts_1month,
        R.id.charts_3month,
        R.id.charts_6month,
        R.id.charts_12month,
        R.id.charts_overall
    )
    private var widgetExists = false
    private val billingViewModel by lazy { VMFactory.getVM(this, BillingViewModel::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {

        if (billingViewModel.proStatus.value == true)
            ColorPatchUtils.setTheme(this)
        else
            theme.applyStyle(R.style.ColorPatch_Pink_Main, true)

        super.onCreate(savedInstanceState)

        binding = ActivityAppwidgetChartsConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setResult(false)
        binding.widgetPeriod.chartsChooseWeek.visibility = View.GONE
        binding.widgetPeriod.charts7day.text =
            resources.getQuantityString(R.plurals.num_weeks, 1, 1)
        binding.widgetPeriod.charts1month.text =
            resources.getQuantityString(R.plurals.num_months, 1, 1)
        binding.widgetPeriod.charts3month.text =
            resources.getQuantityString(R.plurals.num_months, 3, 3)
        binding.widgetPeriod.charts6month.text =
            resources.getQuantityString(R.plurals.num_months, 6, 6)
        binding.widgetPeriod.charts12month.text =
            resources.getQuantityString(R.plurals.num_years, 1, 1)
        binding.widgetPeriod.chartsOverall.text = getString(R.string.charts_overall)

        if (!BuildConfig.DEBUG && Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
            binding.chipDynamic.visibility = View.GONE

        binding.widgetPeriod.chartsPeriod.setOnCheckedChangeListener { group, checkedId ->
            val idx = periodChipIds.indexOf(checkedId)
            if (idx != -1)
                prefs.period = idx
        }

        binding.widgetTheme.setOnCheckedChangeListener { group, checkedId ->
            val theme = themeFromChip(checkedId)
            prefs.theme = theme.ordinal
            initPreview(theme, binding.widgetShadow.isChecked)
            previewAlpha(binding.widgetBgAlpha.value / 100)
        }

        binding.widgetBgAlpha.addOnChangeListener { slider, value, fromUser ->
            prefs.bgAlpha = value / 100
            previewAlpha(value / 100)
        }

        binding.widgetShadow.setOnCheckedChangeListener { compoundButton, checked ->
            prefs.shadow = checked
            initPreview(themeFromChip(binding.widgetTheme.checkedChipId), checked)
            previewAlpha(binding.widgetBgAlpha.value / 100)
        }

        binding.okButton.setOnClickListener {
            setResult(true)
            val idx = periodChipIds.indexOf(binding.widgetPeriod.chartsPeriod.checkedChipId)
            if (idx != -1)
                prefs.period = idx
            updateWidget()

            if (widgetExists)
                ChartsWidgetUpdaterJob.checkAndSchedule(applicationContext, true)

            finish()
        }
        binding.cancelButton.setOnClickListener {
            finish()
        }

        if (isPinned)
            binding.cancelButton.visibility = View.GONE

        initFromPrefs()

        previewAlpha(binding.widgetBgAlpha.value / 100)
    }

    private fun themeFromChip(checkedId: Int): WidgetTheme {
        return when (checkedId) {
            R.id.chip_dark -> WidgetTheme.DARK
            R.id.chip_light -> WidgetTheme.LIGHT
            R.id.chip_dynamic -> WidgetTheme.DYNAMIC
            else -> WidgetTheme.DARK
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        if (newBase != null)
            super.attachBaseContext(newBase.getLocaleContextWrapper())
    }

    private fun initFromPrefs() {
        val period = prefs.period
        if (period != null)
            binding.widgetPeriod.chartsPeriod.check(periodChipIds[period])
        else {
            binding.widgetPeriod.charts7day.isChecked = true
            initPreview(WidgetTheme.DARK, hasShadow = true)
            return
        }

        val theme = WidgetTheme.values()[prefs.theme]
        val hasShadow = prefs.shadow

        initPreview(theme, hasShadow)

        when (theme) {
            WidgetTheme.DARK -> binding.chipDark.isChecked = true
            WidgetTheme.LIGHT -> binding.chipLight.isChecked = true
            WidgetTheme.DYNAMIC -> binding.chipDynamic.isChecked = true
        }

        binding.widgetShadow.isChecked = hasShadow

        val alpha = prefs.bgAlpha
        binding.widgetBgAlpha.value = alpha * 100

        widgetExists = true
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
        setResult(
            if (positive)
                Activity.RESULT_OK
            else
                Activity.RESULT_CANCELED, resultValue
        )

    }

    private fun initPreview(theme: WidgetTheme, hasShadow: Boolean) {
        val b: ViewBinding

        when {
            theme == WidgetTheme.DARK && hasShadow -> {
                b = AppwidgetChartsDarkShadowBinding.inflate(layoutInflater)
                previewBinding = b.appwidgetOuterFrame
            }
            theme == WidgetTheme.DARK && !hasShadow -> {
                b = AppwidgetChartsDarkBinding.inflate(layoutInflater)
                previewBinding = b.appwidgetOuterFrame
            }
            theme == WidgetTheme.LIGHT && hasShadow -> {
                b = AppwidgetChartsLightShadowBinding.inflate(layoutInflater)
                previewBinding = b.appwidgetOuterFrame
            }
            theme == WidgetTheme.LIGHT && !hasShadow -> {
                b = AppwidgetChartsLightBinding.inflate(layoutInflater)
                previewBinding = b.appwidgetOuterFrame
            }
            theme == WidgetTheme.DYNAMIC && hasShadow -> {
                b = AppwidgetChartsDynamicShadowBinding.inflate(layoutInflater)
                previewBinding = b.appwidgetOuterFrame
            }
            theme == WidgetTheme.DYNAMIC && !hasShadow -> {
                b = AppwidgetChartsDynamicBinding.inflate(layoutInflater)
                previewBinding = b.appwidgetOuterFrame
            }
            else -> throw IllegalArgumentException("Invalid theme")
        }

        binding.widgetPreviewFrame.removeAllViews()
        binding.widgetPreviewFrame.addView(b.root)
        previewBinding.appwidgetList.emptyView = previewBinding.appwidgetStatus
        previewBinding.appwidgetList.adapter = FakeChartsAdapter(previewBinding.appwidgetBg.context)
    }

    private fun previewAlpha(alpha: Float) {
        previewBinding.appwidgetBg.alpha = alpha
    }

    private fun updateWidget() {
        val i = Intent(this, ChartsWidgetProvider::class.java).apply {
            action = NLService.iUPDATE_WIDGET
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        sendBroadcast(i)
    }
}