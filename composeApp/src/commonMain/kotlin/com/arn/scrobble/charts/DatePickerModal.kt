package com.arn.scrobble.charts

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.arn.scrobble.ui.rememberLocaleWithCustomWeekday
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.cancel
import pano_scrobbler.composeapp.generated.resources.ok
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
    val locale = rememberLocaleWithCustomWeekday()
    val initialDisplayedMonthMillis = remember { selectedDate ?: System.currentTimeMillis() }
    val yearRange = remember { millisRangeToYears(allowedRange) }

    val datePickerState = remember {
        DatePickerState(
            locale = locale,
            initialSelectedDateMillis = selectedDate,
            initialDisplayedMonthMillis = initialDisplayedMonthMillis,
            yearRange = yearRange,
            selectableDates = selectableDates
        )
    }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDateSelected(datePickerState.selectedDateMillis)
                onDismiss()
            }) {
                Text(stringResource(Res.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}