package com.arn.scrobble.charts

import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Bundle
import android.text.format.DateFormat
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.arn.scrobble.*
import com.arn.scrobble.info.InfoFragment
import com.arn.scrobble.ui.*
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import de.umass.lastfm.*
import kotlinx.android.synthetic.main.chips_charts_period.*
import kotlinx.android.synthetic.main.content_charts.*
import java.util.*

open class ChartsPeriodFragment: Fragment(), EntryItemClickListener {
    protected val viewModel by lazy { VMFactory.getVM(this, ChartsVM::class.java) }
    protected val periodChipIds = arrayOf(R.id.charts_choose_week, R.id.charts_7day, R.id.charts_1month, R.id.charts_3month, R.id.charts_6month, R.id.charts_12month, R.id.charts_overall)
    open val type = 0
    open val username: String?
        get() = parentFragment?.arguments?.getString(Stuff.ARG_USERNAME)
    open val registeredTime: Long
        get() = parentFragment!!.arguments?.getLong(Stuff.ARG_REGISTERED_TIME, 0) ?: 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        charts_period.post { setChipGroupGravity() }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setChipGroupGravity()
    }

    open fun removeHandlerCallbacks() {}

    open fun loadFirstPage() {}

    open fun loadWeeklyCharts() {}

    open fun postInit() {
        val pref = context?.getSharedPreferences(Stuff.ACTIVITY_PREFS, Context.MODE_PRIVATE)
        val periodIdx = pref?.getInt(Stuff.PREF_ACTIVITY_LAST_CHARTS_PERIOD, 1) ?: 1
        var firstLoad = charts_period.checkedChipId == View.NO_ID || charts_period.checkedChipId != periodChipIds.indexOf(periodIdx)

        viewModel.username = username
        viewModel.periodIdx = periodIdx
        viewModel.type = type

        charts_period.check(periodChipIds[periodIdx])

        val ccl = ChipGroup.OnCheckedChangeListener { group, checkedId ->
            if (!firstLoad) {
                removeHandlerCallbacks()
                viewModel.removeAllInfoTasks()
            }
            viewModel.reachedEnd = false
            val idx = periodChipIds.indexOf(checkedId)
            if (checkedId != R.id.charts_choose_week) {
                if (viewModel.periodIdx != idx || firstLoad) {
                    viewModel.periodIdx = idx
                    loadFirstPage()
                    pref?.edit()?.putInt(Stuff.PREF_ACTIVITY_LAST_CHARTS_PERIOD, idx)?.apply()
                }
                setWeeklyChipName(false)
            } else if (firstLoad) {
                val from = pref?.getLong(Stuff.PREF_ACTIVITY_LAST_CHARTS_WEEK_FROM, 0) ?: 0
                val to = pref?.getLong(Stuff.PREF_ACTIVITY_LAST_CHARTS_WEEK_TO, 0) ?: 0
                viewModel.weeklyChart = Chart(Date(from), Date(to), emptyList())
                loadWeeklyCharts()
                setWeeklyChipName(true)
            }
            firstLoad = false

            val hsv = group.parent as HorizontalScrollView
            val chip = group.findViewById<Chip>(checkedId)
            group.post {
                val scrollBounds = Rect()
                hsv.getDrawingRect(scrollBounds)
                val left = chip.x
                val right = left + chip.width
                val isChipVisible = scrollBounds.left < left && scrollBounds.right > right
                if (!isChipVisible)
                    hsv.smoothScrollTo(chip.left, chip.top)
            }
        }

        charts_period.setOnCheckedChangeListener(ccl)
        ccl.onCheckedChanged(charts_period, charts_period.checkedChipId)
        charts_choose_week.setOnClickListener {
            if (Main.isOnline)
                charts_progress.visibility = View.VISIBLE
            viewModel.loadWeeklyChartsList(registeredTime)
        }

        viewModel.weeklyListReceiver.observe(viewLifecycleOwner, { weeklyList ->
            weeklyList ?: return@observe
            val weeklyStrList = arrayListOf<String>()
            weeklyList.forEach {
                weeklyStrList += getString(
                        R.string.a_to_b,
                        DateFormat.getMediumDateFormat(context).format(it.from.time),
                        DateFormat.getMediumDateFormat(context).format(it.to.time)
                )
            }
            val dialog = AlertDialog.Builder(context!!, R.style.DarkDialog)
                    .setTitle(Stuff.getColoredTitle(context!!, getString(R.string.charts_choose_week)))
                    .setItems(weeklyStrList.toTypedArray()) { dialogInterface, i ->
                        val item = weeklyList[i]
                        viewModel.weeklyChart = item
                        viewModel.periodIdx = 0
                        loadWeeklyCharts()
                        pref?.edit()
                                ?.putLong(Stuff.PREF_ACTIVITY_LAST_CHARTS_WEEK_FROM, item.from.time)
                                ?.putLong(Stuff.PREF_ACTIVITY_LAST_CHARTS_WEEK_TO, item.to.time)
                                ?.putInt(Stuff.PREF_ACTIVITY_LAST_CHARTS_PERIOD, 0)
                                ?.apply()
                        setWeeklyChipName(true)
                    }
                    .setOnCancelListener {
                        charts_period?.check(periodChipIds[viewModel.periodIdx])
                    }
                    .setNegativeButton(android.R.string.cancel) { dialogInterface, i ->
                        charts_period?.check(periodChipIds[viewModel.periodIdx])
                    }
                    .show()
            var pos = 0
            for (i in weeklyList.indices) {
                val chart = weeklyList[i]
                if (viewModel.weeklyChart == null)
                    break
                if (chart.from.time == viewModel.weeklyChart!!.from.time && chart.to.time == viewModel.weeklyChart!!.to.time) {
                    pos = i
                    break
                }
            }
            if (pos < weeklyList.size)
                dialog.listView.setSelection(pos)
            dialog.listView.onItemSelectedListener = ListViewItemHighlightTvHack()

            charts_progress.visibility = View.GONE
            viewModel.weeklyListReceiver.value = null
        })
    }

    override fun onItemClick(view: View, entry: MusicEntry) {
        when (entry) {
            is Artist -> {
                val info = InfoFragment()
                val b = Bundle()
                b.putString(NLService.B_ARTIST, entry.name)
                b.putString(Stuff.ARG_USERNAME, username)
                info.arguments = b
                info.show(activity!!.supportFragmentManager, null)
            }
            is Album -> {
                val info = InfoFragment()
                val b = Bundle()
                b.putString(NLService.B_ARTIST, entry.artist)
                b.putString(NLService.B_ALBUM, entry.name)
                b.putString(Stuff.ARG_USERNAME, username)
                info.arguments = b
                info.show(activity!!.supportFragmentManager, null)
            }
            is Track -> {
                val info = InfoFragment()
                val b = Bundle()
                b.putString(NLService.B_ARTIST, entry.artist)
                b.putString(NLService.B_ALBUM, entry.album)
                b.putString(NLService.B_TITLE, entry.name)
                b.putString(Stuff.ARG_USERNAME, username)
                info.arguments = b
                info.show(activity!!.supportFragmentManager, null)
            }
        }
    }

    private fun setWeeklyChipName(set: Boolean) {
        if (set)
            charts_choose_week.text = getString(
                    R.string.a_to_b,
                    DateFormat.getMediumDateFormat(context).format(viewModel.weeklyChart!!.from.time),
                    DateFormat.getMediumDateFormat(context).format(viewModel.weeklyChart!!.to.time)
            )
        else
            charts_choose_week.text = getString(R.string.charts_choose_week)
        charts_choose_week.invalidate()
    }

    private fun setChipGroupGravity() {
        val lp = charts_period.layoutParams as FrameLayout.LayoutParams
        if (resources.displayMetrics.widthPixels > charts_period.width && lp.gravity != Gravity.CENTER_HORIZONTAL) {
            lp.gravity = Gravity.CENTER_HORIZONTAL
            charts_period.layoutParams = lp
        } else if (resources.displayMetrics.widthPixels <= charts_period.width && lp.gravity != Gravity.START) {
            lp.gravity = Gravity.START
            charts_period.layoutParams = lp
        }
    }
}