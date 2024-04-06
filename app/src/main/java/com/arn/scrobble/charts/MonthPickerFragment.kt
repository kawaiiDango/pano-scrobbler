package com.arn.scrobble.charts

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.arn.scrobble.databinding.DialogMonthPickerBinding
import com.arn.scrobble.main.MainNotifierViewModel
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.setMidnight
import com.arn.scrobble.utils.Stuff.toInverseMap
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

    private val timePeriods by lazy {
        TimePeriodsGenerator(
            activityViewModel.currentUser.registeredTime,
            System.currentTimeMillis(),
            context
        ).months.toInverseMap()
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

    private lateinit var monthsButton: MaterialButton
    private lateinit var yearsButton: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onShow(p0: DialogInterface?) {
        yearsButton.text = formattedYear(selectedYear)
        monthsButton.text = formattedMonth(selectedMonth)

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

//            val timePeriod = timePeriods[timePeriod]
            requireActivity().supportFragmentManager.setFragmentResult(
                Stuff.ARG_MONTH_PICKER_PERIOD,
                bundleOf(Stuff.ARG_MONTH_PICKER_PERIOD to timePeriod)
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
            yearsButton = binding.pickerFirst
            monthsButton = binding.pickerSecond
        } else {
            yearsButton = binding.pickerSecond
            monthsButton = binding.pickerFirst
        }

        monthsButton.setOnClickListener { v ->
            PopupMenu(requireContext(), v).apply {
                months.forEach {
                    val mi = menu.add(0, it.id, 0, it.text)
                    mi.isEnabled = it.id != selectedMonth
                }

                setOnMenuItemClickListener { item ->
                    selectedMonth = item.itemId
                    monthsButton.text = item.title
                    true
                }
                show()
            }
        }

        yearsButton.setOnClickListener { v ->
            PopupMenu(requireContext(), v).apply {
                years.forEach {
                    val mi = menu.add(0, it.id, 0, it.text)
                    mi.isEnabled = it.id != selectedYear
                }

                setOnMenuItemClickListener {
                    selectedYear = it.itemId
                    yearsButton.text = it.title

                    months.let {
                        val firstMonth = it.first().id
                        val lastMonth = it.last().id
                        selectedMonth = selectedMonth.coerceIn(firstMonth, lastMonth)

                        monthsButton.text = formattedMonth(selectedMonth)
                    }
                    true

                }
                show()
            }
        }

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
            cal.timeInMillis = timePeriods.keys.first().end
            val startYear = cal[Calendar.YEAR]
            cal.timeInMillis = timePeriods.keys.last().start
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
            val startMonthTime = max(cal.timeInMillis, timePeriods.keys.last().start)
            cal[Calendar.MONTH] = cal.getActualMaximum(Calendar.MONTH)
            val endMonthTime = min(cal.timeInMillis, timePeriods.keys.first().end - 1)
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