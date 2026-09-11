package com.arn.scrobble.charts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.arn.scrobble.icons.Check
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.navigation.DatePickerResult
import com.arn.scrobble.navigation.DateRangePickerResult
import com.arn.scrobble.navigation.LocalNavigationType
import com.arn.scrobble.navigation.PanoNavigationType
import com.arn.scrobble.navigation.TimePickerResult
import com.arn.scrobble.ui.myGroupStandardContainerColor
import com.arn.scrobble.ui.rememberLocaleWithCustomWeekday
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff.setMidnight
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.done
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration.Companion.days

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
    var firstChange by remember { mutableStateOf(true) }

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

    fun onConfirm() {
        datePickerState.selectedDateMillis?.let {
            onDateSelected(DatePickerResult(it))
        }
    }

    LaunchedEffect(datePickerState.selectedDateMillis) {
        if (firstChange)
            firstChange = false
        else
            onConfirm()
    }

    PickerWrapper(
        onConfirm = null,
        modifier = modifier
    ) {
        DatePicker(
            state = datePickerState,
            showModeToggle = !PlatformStuff.isTv,
            title = null,
            modifier = it
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

    PickerWrapper(
        onConfirm = {
            val start = dateRangePickerState.selectedStartDateMillis
            val end = dateRangePickerState.selectedEndDateMillis

            if (start != null && end != null) {
                onDateRangeSelected(
                    DateRangePickerResult(
                        start,
                        end + (1.days.inWholeMilliseconds - 1)
                    )
                )
            }
        },
        modifier = modifier
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            title = null,
            showModeToggle = !PlatformStuff.isTv,
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
            modifier = it
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
    PickerWrapper(
        onConfirm = {
            onTimeSelected(TimePickerResult(state.hour, state.minute))
        },
        modifier = modifier
    ) {
        TimePicker(state, modifier = it)
    }
}

private fun monthPickerYears(
    start: Long,
    end: Long,
    formatter: DateFormat
): List<Pair<Int, String>> {
    val cal by lazy { Calendar.getInstance() }

    val years = mutableListOf<Pair<Int, String>>()
    cal.timeInMillis = end
    val startYear = cal[Calendar.YEAR]
    cal.timeInMillis = start
    val endYear = cal[Calendar.YEAR]

    for (year in startYear downTo endYear) {
        val millis = cal.apply { set(Calendar.YEAR, year) }.timeInMillis
        years += year to formatter.format(millis)
    }
    return years
}

private fun monthPickerMonths(
    selectedYear: Int,
    start: Long,
    end: Long,
    formatter: DateFormat,
): List<Pair<Int, String>> {
    val cal by lazy { Calendar.getInstance() }
    val months = mutableListOf<Pair<Int, String>>()
    cal[Calendar.YEAR] = selectedYear
    cal[Calendar.MONTH] = cal.getActualMinimum(Calendar.MONTH)
    val startMonthTime = max(cal.timeInMillis, start)
    cal[Calendar.MONTH] = cal.getActualMaximum(Calendar.MONTH)
    val endMonthTime = min(cal.timeInMillis, end - 1)
    cal.timeInMillis = startMonthTime
    val startMonth = cal[Calendar.MONTH]
    cal.timeInMillis = endMonthTime
    val endMonth = cal[Calendar.MONTH]

    for (month in startMonth..endMonth) {
        val millis = cal.apply { set(Calendar.MONTH, month) }.timeInMillis
        months += month to formatter.format(millis)
    }

    return months
}

@Composable
fun MonthPickerPopup(
    offset: DpOffset,
    selectedMillis: Long,
    allowedRange: Pair<Long, Long>,
    onDismissRequest: () -> Unit,
    onMonthMillisSelected: (Long) -> Unit,
) {
    val cal = remember {
        Calendar.getInstance().apply {
            timeInMillis = selectedMillis
            setMidnight()
        }
    }

    val yearFormatter = remember { SimpleDateFormat("yyyy", Locale.getDefault()) }
    val monthFormatter = remember { SimpleDateFormat("MMM", Locale.getDefault()) }

    var selectedYear by remember { mutableIntStateOf(cal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(cal.get(Calendar.MONTH)) }

    val yearsList = remember {
        monthPickerYears(allowedRange.first, allowedRange.second, yearFormatter)
    }
    val monthsList = remember(selectedYear) {
        monthPickerMonths(
            selectedYear,
            allowedRange.first,
            allowedRange.second,
            monthFormatter
        ).also {
            if (!it.any { (month, _) -> month == selectedMonth })
                selectedMonth = Calendar.JANUARY
        }
    }

    DropdownMenuPopup(
        popupPositionProvider = MenuDefaults.rememberDropdownMenuPopupPositionProvider(
            MenuAnchorPosition.Below,
            offset
        ),
        expanded = true,
        onDismissRequest = onDismissRequest
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MenuDefaults.GroupSpacing),
        ) {
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShapes(),
                containerColor = MenuDefaults.myGroupStandardContainerColor,
                modifier = Modifier
                    .width(IntrinsicSize.Min)
                    .verticalScroll(rememberScrollState())
            ) {
                yearsList.forEachIndexed { index, (year, text) ->
                    SelectableDropdownMenuItem(
                        selected = year == selectedYear,
                        shapes = MenuDefaults.itemShape(index, yearsList.size),
                        onClick = {
                            selectedYear = year

                            cal.apply {
                                set(Calendar.YEAR, selectedYear)
                                set(Calendar.MONTH, selectedMonth)
                                set(Calendar.DAY_OF_MONTH, 1)
                            }

                            onMonthMillisSelected(cal.timeInMillis)
                        },
                        text = {
                            Text(text)
                        },
                    )
                }
            }

            DropdownMenuGroup(
                shapes = MenuDefaults.groupShapes(),
                containerColor = MenuDefaults.myGroupStandardContainerColor,
                modifier = Modifier
                    .width(IntrinsicSize.Min)
                    .verticalScroll(rememberScrollState())
            ) {
                monthsList.forEachIndexed { index, (month, text) ->

                    SelectableDropdownMenuItem(
                        selected = month == selectedMonth,
                        shapes = MenuDefaults.itemShape(index, monthsList.size),
                        onClick = {
                            selectedMonth = month

                            cal.apply {
                                set(Calendar.YEAR, selectedYear)
                                set(Calendar.MONTH, selectedMonth)
                                set(Calendar.DAY_OF_MONTH, 1)
                            }

                            onMonthMillisSelected(cal.timeInMillis)
                            onDismissRequest()
                        },
                        text = {
                            Text(text)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PickerWrapper(
    onConfirm: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit
) {
    if (LocalNavigationType.current == PanoNavigationType.BOTTOM_NAVIGATION) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
        ) {
            content(
                Modifier
                    .weight(1f, false)
                    .clip(MaterialTheme.shapes.large)
            )

            if (onConfirm != null)
                FloatingActionButton(
                    onClick = onConfirm,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(bottom = 16.dp, end = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Check,
                        contentDescription = stringResource(Res.string.done)
                    )
                }
        }
    } else {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            modifier = modifier
        ) {
            content(
                Modifier
                    .weight(1f, false)
                    .clip(MaterialTheme.shapes.large)
            )

            if (onConfirm != null)
                FloatingActionButton(
                    onClick = onConfirm,
                    modifier = Modifier
                        .align(Alignment.Bottom)
                        .padding(bottom = 16.dp, end = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Check,
                        contentDescription = stringResource(Res.string.done)
                    )
                }
        }
    }
}