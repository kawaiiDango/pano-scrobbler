package com.arn.scrobble.charts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arn.scrobble.icons.Check
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.navigation.DatePickerResult
import com.arn.scrobble.navigation.DateRangePickerResult
import com.arn.scrobble.navigation.TimePickerResult
import com.arn.scrobble.ui.rememberLocaleWithCustomWeekday
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.done
import java.util.Calendar
import java.util.TimeZone

private fun millisRangeToYears(range: Pair<Long, Long>): IntRange {
    val cal = Calendar.getInstance()
    cal.timeInMillis = range.first
    val startYear = cal.get(Calendar.YEAR)
    cal.timeInMillis = range.second
    val endYear = cal.get(Calendar.YEAR)
    return startYear..endYear
}

@Composable
fun DateDialog(
    selectedDate: Long?,
    allowedRange: Pair<Long, Long>,
    weeksOnly: Boolean,
    onDateSelected: (DatePickerResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = rememberLocaleWithCustomWeekday()
    val initialDisplayedMonthMillis = remember { selectedDate ?: System.currentTimeMillis() }
    val yearRange = remember { millisRangeToYears(allowedRange) }

    val selectableDates = remember {
        object : SelectableDates {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"), locale)
            
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                if (utcTimeMillis < allowedRange.first || utcTimeMillis > allowedRange.second)
                    return false

                if (weeksOnly) {
                    cal.timeInMillis = utcTimeMillis
                    return cal.get(Calendar.DAY_OF_WEEK) == cal.firstDayOfWeek
                }

                return true
            }
        }
    }

    val datePickerState = remember {
        DatePickerState(
            locale = locale,
            initialSelectedDateMillis = selectedDate,
            initialDisplayedMonthMillis = initialDisplayedMonthMillis,
            yearRange = yearRange,
            selectableDates = selectableDates
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        DatePicker(
            state = datePickerState,
            modifier = Modifier.weight(1f, false)
        )
        ConfirmButton(
            {
                datePickerState.selectedDateMillis?.let {
                    onDateSelected(DatePickerResult(it))
                }
            }
        )
    }
}

@Composable
fun DateRangeDialog(
    selectedDateRange: Pair<Long, Long>?,
    allowedRange: Pair<Long, Long>,
    onDateRangeSelected: (DateRangePickerResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = rememberLocaleWithCustomWeekday()
    val allowedRangeYears = remember { millisRangeToYears(allowedRange) }
//    val dateFormatter = remember { DatePickerDefaults.dateFormatter() }
    val initialDisplayedMonthMillis =
        remember { selectedDateRange?.first ?: System.currentTimeMillis() }

    val dateRangePickerState = remember {
        DateRangePickerState(
            locale = locale,
            initialSelectedStartDateMillis = selectedDateRange?.first,
            initialSelectedEndDateMillis = selectedDateRange?.second,
            initialDisplayedMonthMillis = initialDisplayedMonthMillis,
            yearRange = allowedRangeYears,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    utcTimeMillis in allowedRange.first..allowedRange.second
            }
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        DateRangePicker(
            state = dateRangePickerState,
//            headline = {
//                // workaround for a text overflow bug in compose in pt locale
//                DateRangePickerDefaults.DateRangePickerHeadline(
//                    selectedStartDateMillis = dateRangePickerState.selectedStartDateMillis,
//                    selectedEndDateMillis = dateRangePickerState.selectedEndDateMillis,
//                    displayMode = dateRangePickerState.displayMode,
//                    dateFormatter = dateFormatter,
//                    modifier = Modifier.padding(bottom = 12.dp),
//                )
//            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, false)
        )

        ConfirmButton(
            {
                val start = dateRangePickerState.selectedStartDateMillis
                val end = dateRangePickerState.selectedEndDateMillis

                if (start != null && end != null) {
                    onDateRangeSelected(
                        DateRangePickerResult(
                            start,
                            end.plus(24 * 60 * 60 * 1000 - 1)
                        )
                    )
                }
            }
        )
    }
}

@Composable
fun TimeDialog(
    h: Int,
    m: Int,
    onTimeSelected: (TimePickerResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val state = rememberTimePickerState(
        initialHour = h,
        initialMinute = m,
//        is24Hour = true,
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        TimePicker(
            state,
            modifier = Modifier.weight(1f, false)
        )

        ConfirmButton(
            {
                onTimeSelected(TimePickerResult(state.hour, state.minute))
            }
        )
    }
}

@Composable
private fun ColumnScope.ConfirmButton(
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onConfirm,
        modifier = modifier
            .align(Alignment.End)
            .padding(bottom = 16.dp, end = 16.dp)
    ) {
        Icon(
            imageVector = Icons.Check,
            contentDescription = stringResource(Res.string.done)
        )
    }
}