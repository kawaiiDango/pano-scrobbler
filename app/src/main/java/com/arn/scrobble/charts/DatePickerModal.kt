package com.arn.scrobble.charts

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.arn.scrobble.R
import java.util.Calendar

fun millisRangeToYears(range: Pair<Long, Long>): IntRange {
    val cal = Calendar.getInstance()
    cal.timeInMillis = range.first
    val startYear = cal.get(Calendar.YEAR)
    cal.timeInMillis = range.second
    val endYear = cal.get(Calendar.YEAR)
    return startYear..endYear
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
    selectedDate: Long?,
    allowedRange: Pair<Long, Long>,
    selectableDates: SelectableDates = DatePickerDefaults.AllDates,
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate,
        initialDisplayedMonthMillis = selectedDate ?: System.currentTimeMillis(),
        yearRange = remember { millisRangeToYears(allowedRange) },
        selectableDates = selectableDates
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDateSelected(datePickerState.selectedDateMillis)
                onDismiss()
            }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}