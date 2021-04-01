package com.arn.scrobble.charts

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Rect
import android.os.Bundle
import android.os.Parcel
import android.text.format.DateFormat
import android.view.View
import androidx.fragment.app.Fragment
import com.arn.scrobble.*
import com.arn.scrobble.databinding.ChipsChartsPeriodBinding
import com.arn.scrobble.info.InfoFragment
import com.arn.scrobble.ui.*
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.umass.lastfm.*
import java.util.*

abstract class ChartsPeriodFragment: Fragment(), EntryItemClickListener {
    protected val viewModel by lazy { VMFactory.getVM(this, ChartsVM::class.java) }
    protected val periodChipIds = arrayOf(R.id.charts_choose_week, R.id.charts_7day, R.id.charts_1month, R.id.charts_3month, R.id.charts_6month, R.id.charts_12month, R.id.charts_overall)
    open val type = 0
    open val username: String?
        get() = parentFragment?.arguments?.getString(Stuff.ARG_USERNAME)
    open val registeredTime: Long
        get() = parentFragment!!.arguments?.getLong(Stuff.ARG_REGISTERED_TIME, 0) ?: 0
    protected abstract val periodChipsBinding: ChipsChartsPeriodBinding
    private var showWeekPicker = false

    open fun removeHandlerCallbacks() {}

    open fun loadFirstPage() {}

    open fun loadWeeklyCharts() {}

    open fun postInit() {
        context ?: return
        val pref = context!!.getSharedPreferences(Stuff.ACTIVITY_PREFS, Context.MODE_PRIVATE)
        val periodIdx = pref.getInt(Stuff.PREF_ACTIVITY_LAST_CHARTS_PERIOD, 1)
        var firstLoad = periodChipsBinding.chartsPeriod.checkedChipId == View.NO_ID ||
                periodChipsBinding.chartsPeriod.checkedChipId != periodChipIds.indexOf(periodIdx)

        viewModel.username = username
//        viewModel.periodIdx = periodIdx
        viewModel.type = type

        periodChipsBinding.chartsPeriod.check(periodChipIds[periodIdx])

        val ccl = ChipGroup.OnCheckedChangeListener { group, checkedId ->
            if (!firstLoad) {
                removeHandlerCallbacks()
                viewModel.removeAllInfoTasks()
            }
            viewModel.reachedEnd = false
            val idx = periodChipIds.indexOf(checkedId)
            if (checkedId != R.id.charts_choose_week) {
                if (viewModel.periodIdx != idx || firstLoad) {
                    if (!firstLoad || viewModel.totalCount == 0 || viewModel.periodIdx != idx) {
                        viewModel.periodIdx = idx
                        loadFirstPage()
                    }
                    pref?.edit()?.putInt(Stuff.PREF_ACTIVITY_LAST_CHARTS_PERIOD, idx)?.apply()
                }
                setWeeklyChipName(pref, false)
            } else if (firstLoad) {
                viewModel.periodIdx = idx
                val from = pref?.getLong(Stuff.PREF_ACTIVITY_LAST_CHARTS_WEEK_FROM, 0) ?: 0
                val to = pref?.getLong(Stuff.PREF_ACTIVITY_LAST_CHARTS_WEEK_TO, 0) ?: 0

                if (!firstLoad || viewModel.totalCount == 0 || viewModel.weeklyChart?.from?.time != from) {
                    viewModel.weeklyChart = Chart(Date(from), Date(to), emptyList())
                    loadWeeklyCharts()
                }
                if (viewModel.weeklyListReceiver.value == null)
                    viewModel.loadWeeklyChartsList(registeredTime)
                setWeeklyChipName(pref, true)
            }
            firstLoad = false

            val chip = group.findViewById<Chip>(checkedId)
            group.post {
                val scrollBounds = Rect()
                periodChipsBinding.chartsPeriodScrollview.getDrawingRect(scrollBounds)
                val left = chip.x
                val right = left + chip.width
                val isChipVisible = scrollBounds.left < left && scrollBounds.right > right
                if (!isChipVisible)
                    periodChipsBinding.chartsPeriodScrollview.smoothScrollTo(chip.left, chip.top)
            }
        }

        periodChipsBinding.chartsPeriod.setOnCheckedChangeListener(ccl)
        ccl.onCheckedChanged(periodChipsBinding.chartsPeriod, periodChipsBinding.chartsPeriod.checkedChipId)
        periodChipsBinding.chartsChooseWeek.setOnClickListener {
            if (Main.isOnline)
                periodChipsBinding.chartsPeriod.alpha = Stuff.LOADING_ALPHA
            showWeekPicker = true
            viewModel.loadWeeklyChartsList(registeredTime)
        }

        periodChipsBinding.chartsWeekNext.setOnClickListener {
            if (viewModel.weeklyListReceiver.value != null && viewModel.weeklyChartIdx > 0) {
                viewModel.weeklyChart = viewModel.weeklyListReceiver.value!![--viewModel.weeklyChartIdx]
                loadWeeklyCharts()
                setWeeklyChipName(pref, true)
            }
        }

        periodChipsBinding.chartsWeekPrev.setOnClickListener {
            if (viewModel.weeklyListReceiver.value != null &&
                    viewModel.weeklyChartIdx != -1 && viewModel.weeklyChartIdx < viewModel.weeklyListReceiver.value!!.size - 1) {
                viewModel.weeklyChart = viewModel.weeklyListReceiver.value!![++viewModel.weeklyChartIdx]
                loadWeeklyCharts()
                setWeeklyChipName(pref, true)
            }
        }

        viewModel.weeklyListReceiver.observe(viewLifecycleOwner, { weeklyList ->
            weeklyList ?: return@observe
            if (!showWeekPicker) {
                if (viewModel.periodIdx == 0 &&
                        viewModel.weeklyChart != null &&
                        viewModel.weeklyListReceiver.value != null) {
                    viewModel.weeklyChartIdx = viewModel.weeklyListReceiver.value!!.indexOfFirst { viewModel.weeklyChart!!.to == it.to}
                    showArrows(true)
                }
                return@observe
            }
            showWeekPicker = false


            val time = if (viewModel.weeklyChart != null)
                viewModel.weeklyChart!!.from.time
            else
                weeklyList.first().from.time
            val startTime = weeklyList.last().from.time
            val endTime = weeklyList.first().from.time
            val weekStartMap = mutableMapOf<Long, Chart<MusicEntry>>()
            weeklyList.forEach{
                weekStartMap[it.from.time] = it
            }

            val dpd = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.charts_choose_week)
                .setCalendarConstraints(
                    CalendarConstraints.Builder()
                        .setStart(startTime)
                        .setEnd(endTime)
                        .setOpenAt(time)
                        .setValidator(object: CalendarConstraints.DateValidator{
                            override fun describeContents(): Int {
                                return 0
                            }

                            override fun writeToParcel(p0: Parcel?, p1: Int) {
                            }

                            override fun isValid(date: Long): Boolean {
                                val utcDate = date + 12 * 3600 * 1000
                                return date in startTime..endTime &&
                                        (weekStartMap.contains(date) || weekStartMap.contains(utcDate))
                            }
                        })
                        .build()
                )
                .setSelection(time)
                .build()

            dpd.addOnPositiveButtonClickListener {
                val item = weekStartMap[it + 12 * 3600 * 1000]
                viewModel.weeklyChart = item
                viewModel.periodIdx = 0
                viewModel.weeklyChartIdx = weeklyList.indexOf(item)
                loadWeeklyCharts()
                setWeeklyChipName(pref, true)
            }
            dpd.addOnNegativeButtonClickListener {
                periodChipsBinding.chartsPeriod.check(periodChipIds[viewModel.periodIdx])
            }
            dpd.addOnCancelListener {
                periodChipsBinding.chartsPeriod.check(periodChipIds[viewModel.periodIdx])
            }

            dpd.show(parentFragmentManager, null)

            periodChipsBinding.chartsPeriod.alpha = 1f
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

    private fun setWeeklyChipName(pref: SharedPreferences, set: Boolean) {
        if (set) {
            periodChipsBinding.chartsChooseWeek.text = getString(
                    R.string.a_to_b,
                    DateFormat.getMediumDateFormat(context).format(viewModel.weeklyChart!!.from.time),
                    DateFormat.getMediumDateFormat(context).format(viewModel.weeklyChart!!.to.time)
            )
            showArrows(true)
            pref.edit()
                    ?.putLong(Stuff.PREF_ACTIVITY_LAST_CHARTS_WEEK_FROM, viewModel.weeklyChart!!.from.time)
                    ?.putLong(Stuff.PREF_ACTIVITY_LAST_CHARTS_WEEK_TO, viewModel.weeklyChart!!.to.time)
                    ?.putInt(Stuff.PREF_ACTIVITY_LAST_CHARTS_PERIOD, 0)
                    ?.apply()
        } else {
            periodChipsBinding.chartsChooseWeek.text = getString(R.string.charts_choose_week)
            showArrows(false)
        }
        periodChipsBinding.chartsChooseWeek.invalidate()
    }

    private fun showArrows(show: Boolean) {
        periodChipsBinding.chartsWeekNext.visibility = if (show && viewModel.weeklyChartIdx > 0)
            View.VISIBLE
        else
            View.GONE
        periodChipsBinding.chartsWeekPrev.visibility = if (show && viewModel.weeklyChartIdx < (viewModel.weeklyListReceiver.value?.size ?: 0) - 1)
            View.VISIBLE
        else
            View.GONE
    }
}