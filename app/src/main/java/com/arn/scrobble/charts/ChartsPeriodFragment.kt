package com.arn.scrobble.charts

import android.os.Parcel
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.forEachIndexed
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.arn.scrobble.MainNotifierViewModel
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.firstOrNull
import com.arn.scrobble.Stuff.lastOrNull
import com.arn.scrobble.Stuff.toBimap
import com.arn.scrobble.Stuff.toBundle
import com.arn.scrobble.databinding.ChipsChartsPeriodBinding
import com.arn.scrobble.info.InfoFragment
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.ui.MusicEntryItemClickListener
import com.arn.scrobble.ui.UiUtils.mySmoothScrollToPosition
import com.arn.scrobble.ui.UiUtils.showWithIcons
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import de.umass.lastfm.MusicEntry
import de.umass.lastfm.Period
import kotlin.math.max


abstract class ChartsPeriodFragment : Fragment(), MusicEntryItemClickListener {
    protected val viewModel by viewModels<ChartsVM>()
    protected open val chartsType = 0
    protected abstract val periodChipsBinding: ChipsChartsPeriodBinding
    private lateinit var periodChipsAdapter: PeriodChipsAdapter
    private lateinit var prevSelectedPeriod: TimePeriod
    protected val activityViewModel by viewModels<MainNotifierViewModel>({ activity!! })


    protected val prefs by lazy { MainPrefs(context!!) }

    abstract fun loadFirstPage(networkOnly: Boolean = false)

    protected open fun postInit() {
        context ?: return

        periodChipsAdapter = PeriodChipsAdapter(viewModel) { pos ->
            periodChipsBinding.chartsPeriodsList.mySmoothScrollToPosition(pos)
            if (viewModel.periodType.value == TimePeriodType.CUSTOM)
                showDateRangePicker()
        }
        viewModel.timePeriods.observe(viewLifecycleOwner) {
            it ?: return@observe
            periodChipsAdapter.resetSelection()
            periodChipsAdapter.notifyDataSetChanged()
            periodChipsBinding.chartsPeriodsList.scheduleLayoutAnimation()
        }

        viewModel.periodType.observe(viewLifecycleOwner) { periodType ->
            periodType ?: return@observe

            periodChipsBinding.chartsPeriodType.text = periodType.localizedName

            val timePeriodsGenerator =
                TimePeriodsGenerator(activityViewModel.peekUser().registeredTime, System.currentTimeMillis(), context)

            viewModel.timePeriods.value = when (periodType) {
                TimePeriodType.CONTINUOUS -> TimePeriodsGenerator.getContinuousPeriods(context!!)
                    .toBimap()
                TimePeriodType.CUSTOM -> {
                    val selectedPeriod = viewModel.selectedPeriod.value ?: TimePeriod(
                        context!!,
                        0,
                        System.currentTimeMillis()
                    )
                    val start = max(selectedPeriod.start, activityViewModel.peekUser().registeredTime)
                    val end = selectedPeriod.end
                    listOf(
                        TimePeriod(context!!, start, end)
                    ).toBimap()
                }
                TimePeriodType.WEEK -> timePeriodsGenerator.weeks.toBimap()
                TimePeriodType.MONTH -> timePeriodsGenerator.months.toBimap()
                TimePeriodType.YEAR -> timePeriodsGenerator.years.toBimap()
                else -> throw IllegalArgumentException("Unknown period type: $periodType")
            }

            periodChipsBinding.chartsCalendar.visibility =
                if (periodType in arrayOf(TimePeriodType.WEEK, TimePeriodType.MONTH))
                    View.VISIBLE
                else
                    View.GONE

        }

        var firstLoad = viewModel.selectedPeriod.value == null
        prevSelectedPeriod = prefs.lastChartsPeriodSelectedJson

        viewModel.selectedPeriod.observe(viewLifecycleOwner) { timePeriod ->
            timePeriod ?: return@observe

            if (viewModel.periodType.value == TimePeriodType.CUSTOM) {
                val oldTimePeriod = viewModel.timePeriods.value?.firstOrNull()
                val len = viewModel.timePeriods.value?.size ?: 0
                if (len == 1 && oldTimePeriod != timePeriod) {
                    viewModel.timePeriods.value = listOf(timePeriod).toBimap()
                }
            }

            if (firstLoad || viewModel.totalCount == 0 || timePeriod != prevSelectedPeriod) {
                loadFirstPage()
                firstLoad = false
            }

            prefs.lastChartsPeriodSelectedJson = timePeriod
            prefs.lastChartsPeriodType = viewModel.periodType.value?.name ?: ""
            prevSelectedPeriod = timePeriod

        }

        periodChipsBinding.chartsPeriodsList.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        periodChipsBinding.chartsPeriodsList.adapter = periodChipsAdapter

        viewModel.username = activityViewModel.peekUser().name
        viewModel.chartsType = chartsType

        viewModel.periodType.value = try {
            TimePeriodType.valueOf(prefs.lastChartsPeriodType)
        } catch (e: IllegalArgumentException) {
            TimePeriodType.CONTINUOUS
        }

        if (viewModel.periodType.value == TimePeriodType.CONTINUOUS && prevSelectedPeriod.period == null) {
            viewModel.selectedPeriod.value = TimePeriod(context!!, Period.ONE_MONTH)
        } else if (viewModel.timePeriods.value != null && prevSelectedPeriod !in viewModel.timePeriods.value!!.inverse) {
            val firstPeriod = viewModel.timePeriods.value!!.firstOrNull()!!
            val lastPeriod = viewModel.timePeriods.value!!.lastOrNull()!!

            // clamp or select first
            viewModel.selectedPeriod.value = when {
                prevSelectedPeriod.start > firstPeriod.start -> firstPeriod
                prevSelectedPeriod.start < lastPeriod.start -> lastPeriod
                else -> firstPeriod
            }
        } else
            viewModel.selectedPeriod.value = prevSelectedPeriod

        findSelectedAndScroll(false)

        periodChipsBinding.chartsPeriodType.setOnClickListener {
            showPeriodTypeSelector()
        }

        periodChipsBinding.chartsCalendar.setOnClickListener {
            if (viewModel.periodType.value == TimePeriodType.MONTH)
                showMonthPicker()
            else if (viewModel.periodType.value == TimePeriodType.WEEK)
                showWeekPicker()
        }

    }

    override fun onItemClick(view: View, entry: MusicEntry) {
        val info = InfoFragment()
        info.arguments = entry.toBundle()
        info.show(activity!!.supportFragmentManager, null)
    }

    private fun findSelectedAndScroll(animate: Boolean) {
        val selectedIdx =
            viewModel.timePeriods.value?.inverse?.get(viewModel.selectedPeriod.value) ?: -1
        if (selectedIdx != -1) {
            periodChipsBinding.chartsPeriodsList.mySmoothScrollToPosition(
                selectedIdx,
                animate = animate
            )
            periodChipsAdapter.refreshSelection(selectedIdx)
        }
    }

    private fun showWeekPicker() {
        val startTime = Stuff.timeToUTC(viewModel.timePeriods.value?.lastOrNull()?.start ?: return)
        val endTime = Stuff.timeToUTC(viewModel.timePeriods.value?.firstOrNull()?.end ?: return)
        var openAtTime = Stuff.timeToUTC(
            viewModel.selectedPeriod.value?.start
                ?: viewModel.timePeriods.value!!.firstOrNull()!!.start
        )
        if (openAtTime !in startTime..endTime)
            openAtTime = System.currentTimeMillis()

        val validTimesUTC = viewModel.timePeriods.value
            ?.map { (i, it) -> Stuff.timeToUTC(it.start) }
            ?.toSet()
            ?: return

        val dpd = MaterialDatePicker.Builder.datePicker()
            .setTitleText(periodChipsBinding.chartsPeriodType.text)
//            .setTheme(context!!.attrToThemeId(R.attr.materialCalendarFullscreenTheme))
            .setCalendarConstraints(
                CalendarConstraints.Builder()
                    .setStart(startTime)
                    .setEnd(endTime)
                    .setOpenAt(openAtTime)
                    .setValidator(object : CalendarConstraints.DateValidator {
                        override fun describeContents() = 0

                        override fun writeToParcel(p0: Parcel, p1: Int) {}

                        override fun isValid(date: Long): Boolean {
                            return date in validTimesUTC
                        }
                    })
                    .build()
            )
            .setSelection(openAtTime)
            .build()

        dpd.addOnPositiveButtonClickListener {
            val idx = validTimesUTC.indexOf(it)
            viewModel.selectedPeriod.value =
                viewModel.timePeriods.value?.get(idx) ?: return@addOnPositiveButtonClickListener
            findSelectedAndScroll(true)
        }

        dpd.show(parentFragmentManager, null)
    }

    private fun showDateRangePicker() {
        val time = System.currentTimeMillis()
        var openAtTime = Stuff.timeToUTC(viewModel.selectedPeriod.value?.start ?: 0)
        if (openAtTime !in activityViewModel.peekUser().registeredTime..time)
            openAtTime = System.currentTimeMillis()

        val dpd = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(periodChipsBinding.chartsPeriodType.text)
//            .setTheme(context!!.attrToThemeId(R.attr.materialCalendarFullscreenTheme))
            .setCalendarConstraints(
                CalendarConstraints.Builder()
                    .setStart(activityViewModel.peekUser().registeredTime)
                    .setEnd(time)
                    .setOpenAt(openAtTime)
                    .setValidator(object : CalendarConstraints.DateValidator {
                        override fun describeContents() = 0

                        override fun writeToParcel(p0: Parcel, p1: Int) {}

                        override fun isValid(date: Long) = date in activityViewModel.peekUser().registeredTime..time
                    })
                    .build()
            )
            .build()

        dpd.addOnPositiveButtonClickListener {
            viewModel.selectedPeriod.value =
                TimePeriod(
                    context!!,
                    Stuff.timeToLocal(it.first),
                    Stuff.timeToLocal(it.second + 24 * 60 * 60 * 1000)
                )
            Stuff.log("selectedPeriod: ${viewModel.selectedPeriod.value}")
        }

        dpd.show(parentFragmentManager, null)
    }

    private fun showMonthPicker() {
        MonthPickerFragment.launch(
            this,
            viewModel.timePeriods.value!!,
            viewModel.selectedPeriod.value
        ) {
            viewModel.selectedPeriod.value = it
            findSelectedAndScroll(true)
        }
    }

    private fun showPeriodTypeSelector() {
        val popup = PopupMenu(context!!, periodChipsBinding.chartsPeriodType)
        popup.menuInflater.inflate(R.menu.period_type_menu, popup.menu)

        val menuIds = mutableListOf<Int>()
        popup.menu.forEachIndexed { index, menuItem ->
            menuIds += menuItem.itemId
            menuItem.title =
                TimePeriodType.values().filter { it != TimePeriodType.DAY }[index].localizedName
        }

        popup.setOnMenuItemClickListener { menuItem ->
            val periodType =
                TimePeriodType.values()
                    .filter { it != TimePeriodType.DAY }[menuIds.indexOf(menuItem.itemId)]
            if (periodType != viewModel.periodType.value) {
                viewModel.periodType.value = periodType
                viewModel.selectedPeriod.value = null // reset selected period
                periodChipsBinding.chartsPeriodsList.mySmoothScrollToPosition(0)
            }
            if (periodType == TimePeriodType.CUSTOM) {
                showDateRangePicker()
            }
            true
        }

        popup.showWithIcons()
    }

    private val TimePeriodType.localizedName
        get() = when (this) {
            TimePeriodType.CONTINUOUS -> getString(R.string.charts_continuous)
            TimePeriodType.CUSTOM -> getString(R.string.charts_custom)
            TimePeriodType.WEEK -> getString(R.string.weeks)
            TimePeriodType.MONTH -> getString(R.string.months)
            TimePeriodType.YEAR -> getString(R.string.years)
            else -> throw IllegalArgumentException("Unknown period type: $this")
        }

}