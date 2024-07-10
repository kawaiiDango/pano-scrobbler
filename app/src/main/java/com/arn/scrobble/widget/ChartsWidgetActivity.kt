package com.arn.scrobble.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMarginsRelative
import androidx.viewbinding.ViewBinding
import com.arn.scrobble.R
import com.arn.scrobble.billing.BillingViewModel
import com.arn.scrobble.databinding.ActivityAppwidgetChartsConfigBinding
import com.arn.scrobble.databinding.AppwidgetChartsContentBinding
import com.arn.scrobble.databinding.AppwidgetChartsDynamicBinding
import com.arn.scrobble.databinding.AppwidgetChartsDynamicShadowBinding
import com.arn.scrobble.databinding.ListItemChipPeriodBinding
import com.arn.scrobble.pref.WidgetPrefs
import com.arn.scrobble.themes.ColorPatchUtils
import com.arn.scrobble.utils.LocaleUtils.setLocaleCompat
import com.arn.scrobble.utils.Stuff
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

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

    private lateinit var binding: ActivityAppwidgetChartsConfigBinding
    private lateinit var previewBinding: AppwidgetChartsContentBinding
    private val prefs by lazy { WidgetPrefs(this)[appWidgetId] }

    private var widgetExists = false
    private val billingViewModel by viewModels<BillingViewModel>()
    private val widgetTimePeriods by lazy { WidgetTimePeriods(this) }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        ColorPatchUtils.setTheme(this, billingViewModel.proStatus.value)

        binding = ActivityAppwidgetChartsConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setResult(false)

        widgetTimePeriods.periodsMap.forEach { (key, timePeriod) ->
            ListItemChipPeriodBinding.inflate(
                layoutInflater,
                binding.widgetPeriod,
                false
            ).root.apply {
                text = timePeriod.name
                tag = key
                id = View.generateViewId()
                updateLayoutParams<ChipGroup.LayoutParams> {
                    updateMarginsRelative(0, 0, 0, 0)
                }
                binding.widgetPeriod.addView(this)
            }
        }

        binding.widgetPeriod.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener

            val key = group.findViewById<Chip>(checkedIds[0])?.tag as? String
            if (key != null) {
                prefs.period = key
                prefs.periodName = widgetTimePeriods.periodsMap[key]?.name
            }
        }

        binding.widgetBgAlpha.addOnChangeListener { slider, value, fromUser ->
            prefs.bgAlpha = value / 100
            previewAlpha(value / 100)
        }

        binding.widgetShadow.setOnCheckedChangeListener { compoundButton, checked ->
            prefs.shadow = checked
            initPreview(checked)
            previewAlpha(binding.widgetBgAlpha.value / 100)
        }

        binding.okButton.setOnClickListener {
            setResult(true)
            val key =
                binding.widgetPeriod.findViewById<Chip>(binding.widgetPeriod.checkedChipId)?.tag as? String
            if (key != null) {
                prefs.period = key
                prefs.periodName = widgetTimePeriods.periodsMap[key]?.name
            }
            ChartsListUtils.updateWidget(intArrayOf(appWidgetId))

            if (widgetExists)
                ChartsWidgetUpdaterWorker.checkAndSchedule(applicationContext, true)

            finish()
        }
        binding.cancelButton.setOnClickListener {
            finish()
        }

        if (isPinned)
            binding.cancelButton.visibility = View.GONE

        val numMins = (Stuff.CHARTS_WIDGET_REFRESH_INTERVAL / (1000 * 60)).toInt()
        binding.appwidgetRefreshEveryText.text = getString(
            R.string.appwidget_refresh_every,
            resources.getQuantityString(R.plurals.num_minutes, numMins, numMins)
        )

        initFromPrefs()

        previewAlpha(binding.widgetBgAlpha.value / 100)
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase ?: return)
        setLocaleCompat()
    }

    private fun initFromPrefs() {
        val hasShadow = prefs.shadow

        val checkedChip = binding.widgetPeriod.children.firstOrNull { it.tag == prefs.period }
            ?: binding.widgetPeriod.children.first()
        (checkedChip as Chip).isChecked = true

        initPreview(hasShadow)
        binding.widgetShadow.isChecked = hasShadow
        binding.widgetBgAlpha.value = prefs.bgAlpha * 100
        widgetExists = prefs.period != null
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
            if (positive) Activity.RESULT_OK else Activity.RESULT_CANCELED,
            resultValue
        )

    }

    private fun initPreview(hasShadow: Boolean) {
        val b: ViewBinding

        if (hasShadow) {
            b = AppwidgetChartsDynamicShadowBinding.inflate(layoutInflater)
            previewBinding = b.appwidgetOuterFrame
        } else {
            b = AppwidgetChartsDynamicBinding.inflate(layoutInflater)
            previewBinding = b.appwidgetOuterFrame
        }

        binding.widgetPreviewFrame.removeAllViews()
        binding.widgetPreviewFrame.addView(b.root)
        previewBinding.appwidgetList.emptyView = previewBinding.appwidgetStatus
        previewBinding.appwidgetList.adapter = FakeChartsAdapter(previewBinding.appwidgetBg.context)
    }

    private fun previewAlpha(alpha: Float) {
        previewBinding.appwidgetBg.alpha = alpha
    }

}