package com.arn.scrobble.charts

import android.os.Bundle
import android.os.Parcel
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.core.view.forEach
import androidx.core.view.forEachIndexed
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.arn.scrobble.R
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.LastFm
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.listenbrainz.ListenBrainz
import com.arn.scrobble.databinding.ChipsChartsPeriodBinding
import com.arn.scrobble.main.MainNotifierViewModel
import com.arn.scrobble.ui.MusicEntryItemClickListener
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.putData
import com.arn.scrobble.utils.UiUtils.collectLatestLifecycleFlow
import com.arn.scrobble.utils.UiUtils.mySmoothScrollToPosition
import com.arn.scrobble.utils.UiUtils.showWithIcons
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar


abstract class ChartsPeriodFragment : Fragment(), MusicEntryItemClickListener {
    protected open val viewModel by viewModels<ChartsPeriodVM>()
    protected open val chartsType = 0
    protected abstract val periodChipsBinding: ChipsChartsPeriodBinding
    private lateinit var periodChipsAdapter: PeriodChipsAdapter
    private lateinit var prevSelectedPeriod: TimePeriod
    protected val activityViewModel by activityViewModels<MainNotifierViewModel>()


//    protected open var lastPeriodSelectedJson
//        get() = App.prefs.lastChartsPeriodSelectedJson
//        set(value) {
//            App.prefs.lastChartsPeriodSelectedJson = value
//        }
//
//    protected open var lastPeriodType
//        get() = App.prefs.lastChartsPeriodType
//        set(value) {
//            App.prefs.lastChartsPeriodType = value
//        }

    abstract fun loadFirstPage(networkOnly: Boolean = false)

    protected open fun postInit() {
        requireActivity().supportFragmentManager.setFragmentResultListener(
            Stuff.ARG_MONTH_PICKER_PERIOD + "|" + chartsType,
            viewLifecycleOwner
        ) { key, bundle ->
            viewModel.setSelectedPeriod(bundle.getParcelable(Stuff.ARG_MONTH_PICKER_PERIOD)!!)
            findSelectedAndScroll(true)
        }

        periodChipsBinding.root.visibility = View.VISIBLE

        periodChipsAdapter = PeriodChipsAdapter(viewModel) { pos, timePeriod ->
            periodChipsBinding.chartsPeriodsList.mySmoothScrollToPosition(pos)
            viewModel.setSelectedPeriod(timePeriod)
            if (viewModel.periodType.value == TimePeriodType.CUSTOM)
                showDateRangePicker()
        }

        collectLatestLifecycleFlow(viewModel.timePeriods) {
            periodChipsAdapter.resetSelection()
            periodChipsAdapter.submitList(it.keys.toList())
            periodChipsBinding.chartsPeriodsList.scheduleLayoutAnimation()
            findSelectedAndScroll(false)
        }

        collectLatestLifecycleFlow(viewModel.periodType) { periodType ->
            periodChipsBinding.chartsPeriodType.text = periodType.localizedName

            // date picker has invisible cursor on TV
            periodChipsBinding.chartsCalendar.isVisible = !Stuff.isTv &&
                    periodType in arrayOf(TimePeriodType.WEEK, TimePeriodType.MONTH)
        }

        periodChipsBinding.chartsPeriodsList.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        periodChipsBinding.chartsPeriodsList.adapter = periodChipsAdapter

        periodChipsBinding.chartsPeriodType.setOnClickListener {
            showPeriodTypeSelector()
        }

        periodChipsBinding.chartsCalendar.setOnClickListener {
            if (viewModel.periodType.value == TimePeriodType.MONTH)
                showMonthPicker()
            else if (viewModel.periodType.value == TimePeriodType.WEEK)
                showWeekPicker()
        }

        periodChipsBinding.chartsPeriodType.isVisible = Scrobblables.current is LastFm

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                if (viewModel.input.value == null)
                    loadFirstPage()
            }
        }
    }

    override fun onItemClick(view: View, entry: MusicEntry) {
        val args = Bundle().putData(entry)
        findNavController().navigate(R.id.infoFragment, args)
    }

    private fun findSelectedAndScroll(animate: Boolean) {
        val selectedIdx =
            viewModel.timePeriods.value[viewModel.selectedPeriod.value] ?: -1
        if (selectedIdx != -1) {
            periodChipsBinding.chartsPeriodsList.mySmoothScrollToPosition(
                selectedIdx,
                animate = animate
            )
            periodChipsAdapter.refreshSelection(selectedIdx)
        }
    }

    private fun showWeekPicker() {
        if (viewModel.timePeriods.value.isEmpty())
            return

        val timePeriods = viewModel.timePeriods.value.keys

        val startTime = Stuff.timeToUTC(timePeriods.last().start)
        val endTime = Stuff.timeToUTC(timePeriods.first().end)
        var openAtTime = Stuff.timeToUTC(
            viewModel.selectedPeriod.value.start
        )
        if (openAtTime !in startTime..endTime)
            openAtTime = System.currentTimeMillis()

        val validTimesUTC = viewModel.timePeriods.value.keys
            .associateBy { Stuff.timeToUTC(it.start) }

        val dpd = MaterialDatePicker.Builder.datePicker()
            .setTitleText(periodChipsBinding.chartsPeriodType.text)
//            .setTheme(requireContext().attrToThemeId(R.attr.materialCalendarFullscreenTheme))
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
            viewModel.setSelectedPeriod(validTimesUTC[it]!!)
            findSelectedAndScroll(true)
        }

        dpd.show(parentFragmentManager, null)
    }

    private fun showDateRangePicker() {
        val time = System.currentTimeMillis()
        var openAtTime = Stuff.timeToUTC(viewModel.selectedPeriod.value.start)
        if (openAtTime !in activityViewModel.currentUser.registeredTime..time)
            openAtTime = System.currentTimeMillis()

        val dpd = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(periodChipsBinding.chartsPeriodType.text)
            .setCalendarConstraints(
                CalendarConstraints.Builder()
                    .setStart(activityViewModel.currentUser.registeredTime)
                    .setEnd(time)
                    .setOpenAt(openAtTime)
                    .setValidator(object : CalendarConstraints.DateValidator {
                        override fun describeContents() = 0

                        override fun writeToParcel(p0: Parcel, p1: Int) {}

                        override fun isValid(date: Long) =
                            date in activityViewModel.currentUser.registeredTime..time
                    })
                    .build()
            )
            .build()

        dpd.addOnPositiveButtonClickListener {
            TimePeriod(
                Stuff.timeToLocal(it.first),
                Stuff.timeToLocal(it.second + 24 * 60 * 60 * 1000),
            ).let {
                viewModel.setSelectedPeriod(it)
                viewModel.setPeriodType(TimePeriodType.CUSTOM) // re emit to fire the flow
            }
            Timber.i("selectedPeriod: ${viewModel.selectedPeriod.value}")
        }

        dpd.show(parentFragmentManager, null)
    }

    private fun showMonthPicker() {
        val selectedPeriod = viewModel.selectedPeriod.value
        val cal = Calendar.getInstance()
        cal.timeInMillis = selectedPeriod.start

        val args = bundleOf(
            Stuff.ARG_SELECTED_YEAR to cal[Calendar.YEAR],
            Stuff.ARG_SELECTED_MONTH to cal[Calendar.MONTH],
            Stuff.ARG_CUSTOM_REQUEST_KEY to Stuff.ARG_MONTH_PICKER_PERIOD + "|" + chartsType
        )
        findNavController().navigate(R.id.monthPickerFragment, args)
    }

    private fun showPeriodTypeSelector() {
        val popup = PopupMenu(requireContext(), periodChipsBinding.chartsPeriodType)
        popup.menuInflater.inflate(R.menu.period_type_menu, popup.menu)

        val idToKeep = when (Scrobblables.current) {
            is ListenBrainz -> R.id.menu_listenbrainz
            !is LastFm -> R.id.menu_continuous
            else -> 0
        }

        if (Scrobblables.current !is LastFm) {
//            val idsToRemove = mutableListOf<Int>()
            popup.menu.forEach {
                if (idToKeep != 0 && it.itemId != idToKeep) {
                    it.isVisible = false
                    it.isEnabled = false
                }
//                    idsToRemove += it.itemId
            }
//            idsToRemove.forEach { popup.menu.removeItem(it) }
        }

        if (Stuff.isTv)
            popup.menu.findItem(R.id.menu_custom)?.isVisible = false

        val menuIds = mutableListOf<Int>()
        popup.menu.forEachIndexed { index, menuItem ->
            menuIds += menuItem.itemId
            menuItem.title =
                TimePeriodType.entries.filter { it != TimePeriodType.DAY }[index].localizedName
        }

        popup.setOnMenuItemClickListener { menuItem ->
            val periodType =
                TimePeriodType.entries
                    .filter { it != TimePeriodType.DAY }[menuIds.indexOf(menuItem.itemId)]
            viewModel.setPeriodType(periodType)

            // reset selected period
//                viewModel.setSelectedPeriod(null)
            periodChipsBinding.chartsPeriodsList.mySmoothScrollToPosition(0)
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
            TimePeriodType.LISTENBRAINZ -> getString(R.string.listenbrainz)
            else -> throw IllegalArgumentException("Unknown period type: $this")
        }

}