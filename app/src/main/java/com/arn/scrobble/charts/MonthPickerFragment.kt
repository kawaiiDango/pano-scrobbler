package com.arn.scrobble.charts

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.arn.scrobble.R
import com.arn.scrobble.databinding.DialogMonthPickerBinding
import com.arn.scrobble.main.MainNotifierViewModel
import com.arn.scrobble.ui.NoOpFilter
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.firstOrNull
import com.arn.scrobble.utils.Stuff.lastOrNull
import com.arn.scrobble.utils.Stuff.setMidnight
import com.arn.scrobble.utils.Stuff.toBimap
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class MonthPickerFragment : DialogFragment(), DialogInterface.OnShowListener {
    private val activityViewModel by activityViewModels<MainNotifierViewModel>()
    private var _binding: DialogMonthPickerBinding? = null
    private val binding
        get() = _binding!!
    private val cal by lazy { Calendar.getInstance() }

    private val monthAdapter by lazy {
        object : ArrayAdapter<MonthPickerItem>(
            requireContext(),
            R.layout.list_item_month,
            R.id.text_item,
            months
        ) {
            val noOpFilter = NoOpFilter()
            override fun getFilter() = noOpFilter
        }
    }

    private val yearAdapter by lazy {
        object : ArrayAdapter<MonthPickerItem>(
            requireContext(),
            R.layout.list_item_month,
            R.id.text_item,
            years
        ) {
            val noOpFilter = NoOpFilter()
            override fun getFilter() = noOpFilter
        }
    }

    private val timePeriods by lazy {
        TimePeriodsGenerator(
            activityViewModel.currentUser.registeredTime,
            System.currentTimeMillis(),
            context
        ).months.toBimap()
    }

    private val yearFormatter by lazy { SimpleDateFormat("yyyy", Locale.getDefault()) }
    private val monthFormatter by lazy { SimpleDateFormat("MMM", Locale.getDefault()) }

    private var selectedYear
        get() = requireArguments().getInt(Stuff.ARG_SELECTED_YEAR)
        set(value) {
            requireArguments().putInt(Stuff.ARG_SELECTED_YEAR, value)
        }

    private var selectedMonth
        get() = requireArguments().getInt(Stuff.ARG_SELECTED_MONTH)
        set(value) {
            requireArguments().putInt(Stuff.ARG_SELECTED_MONTH, value)
        }

    private lateinit var monthsEditText: MaterialAutoCompleteTextView
    private lateinit var yearsEditText: MaterialAutoCompleteTextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onShow(p0: DialogInterface?) {
        yearsEditText.setText(formattedYear(selectedYear), false)
        monthsEditText.setText(formattedMonth(selectedMonth), false)

        val okButton = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
        okButton.setOnClickListener {
            // get the original one with name
            cal.setMidnight()
            cal[Calendar.DAY_OF_MONTH] = 1
            cal[Calendar.YEAR] = selectedYear
            cal[Calendar.MONTH] = selectedMonth
            val startTime = cal.timeInMillis
            cal.add(Calendar.MONTH, 1)
            val endTime = cal.timeInMillis
            val timePeriod = TimePeriod(
                startTime,
                endTime,
                name = DateUtils.formatDateTime(
                    requireContext(),
                    startTime,
                    DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_NO_MONTH_DAY
                )
            )

            val idx = timePeriods.inverse[timePeriod]
            requireActivity().supportFragmentManager.setFragmentResult(
                Stuff.ARG_MONTH_PICKER_PERIOD,
                bundleOf(Stuff.ARG_MONTH_PICKER_PERIOD to timePeriods[idx]!!)
            )
            dismiss()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogMonthPickerBinding.inflate(layoutInflater)

        val format = (DateFormat.getDateInstance(
            DateFormat.MEDIUM,
            Locale.getDefault()
        ) as SimpleDateFormat).toPattern()
        val monthIdx = format.indexOf('m', ignoreCase = true)
        val yearIdx = format.indexOf('y', ignoreCase = true)

        if (monthIdx > yearIdx) {
            yearsEditText = binding.pickerFirst
            monthsEditText = binding.pickerSecond
        } else {
            yearsEditText = binding.pickerSecond
            monthsEditText = binding.pickerFirst
        }

        monthsEditText.setAdapter(monthAdapter)
        monthsEditText.setOnItemClickListener { adapterView, view, pos, l ->
            selectedMonth = monthAdapter.getItem(pos)!!.id
        }

        yearsEditText.setAdapter(yearAdapter)
        yearsEditText.setOnItemClickListener { adapterView, view, pos, l ->
            selectedYear = yearAdapter.getItem(pos)!!.id

            months.let {
                val firstMonth = it.first().id
                val lastMonth = it.last().id
                selectedMonth = selectedMonth.coerceIn(firstMonth, lastMonth)

                monthsEditText.setText(formattedMonth(selectedMonth), false)

                monthAdapter.clear()
                monthAdapter.addAll(it)
            }

            monthAdapter.notifyDataSetChanged()
        }

        scrollToSelected(monthsEditText, monthAdapter, selectedMonth)
        scrollToSelected(yearsEditText, yearAdapter, selectedYear)

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .apply {
                setOnShowListener(this@MonthPickerFragment)
            }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    // hack to scroll to selected month
    // doesn't work in all circumstances
    private fun scrollToSelected(
        editText: AutoCompleteTextView,
        adapter: ArrayAdapter<MonthPickerItem>,
        selectedId: Int
    ) {
        var selectedPos = -1
        for (i in 0 until adapter.count) {
            if (adapter.getItem(i)!!.id == selectedId) {
                selectedPos = i
                break
            }
        }

        if (selectedPos != -1) {
            editText.setOnClickListener {
                editText.postDelayed({ editText.listSelection = selectedPos }, 100)
            }
        }
    }

    private fun formattedMonth(month: Int): String {
        cal.setMidnight()
        cal[Calendar.DAY_OF_MONTH] = 1
        cal[Calendar.MONTH] = month
        return monthFormatter.format(cal.timeInMillis)
    }

    private fun formattedYear(year: Int): String {
        cal.setMidnight()
        cal[Calendar.DAY_OF_MONTH] = 1
        cal[Calendar.MONTH] = 0
        cal[Calendar.YEAR] = year
        return yearFormatter.format(cal.timeInMillis)
    }

    private val years
        get(): List<MonthPickerItem> {
            val years = mutableListOf<MonthPickerItem>()
            cal.timeInMillis = timePeriods.firstOrNull()!!.end
            val startYear = cal[Calendar.YEAR]
            cal.timeInMillis = timePeriods.lastOrNull()!!.start
            val endYear = cal[Calendar.YEAR]

            for (year in startYear downTo endYear) {
                years += MonthPickerItem(formattedYear(year), year)
            }
            return years
        }

    private val months
        get(): List<MonthPickerItem> {
            val months = mutableListOf<MonthPickerItem>()
            cal[Calendar.YEAR] = selectedYear
            cal[Calendar.MONTH] = cal.getActualMinimum(Calendar.MONTH)
            val startMonthTime = max(cal.timeInMillis, timePeriods.lastOrNull()!!.start)
            cal[Calendar.MONTH] = cal.getActualMaximum(Calendar.MONTH)
            val endMonthTime = min(cal.timeInMillis, timePeriods.firstOrNull()!!.end - 1)
            cal.timeInMillis = startMonthTime
            val startMonth = cal[Calendar.MONTH]
            cal.timeInMillis = endMonthTime
            val endMonth = cal[Calendar.MONTH]

            for (month in startMonth..endMonth) {
                months += MonthPickerItem(formattedMonth(month), month)
            }
            return months
        }
}

data class MonthPickerItem(val text: String, val id: Int) {
    override fun toString() = text
}