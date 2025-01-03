package com.arn.scrobble.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CalendarViewMonth
import androidx.compose.material.icons.outlined.CalendarViewWeek
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SplitButton
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.LastfmPeriod
import com.arn.scrobble.charts.TimePeriodsGenerator.Companion.toDuration
import com.arn.scrobble.charts.TimePeriodsGenerator.Companion.toTimePeriod
import com.arn.scrobble.ui.ButtonWithSpinner
import com.arn.scrobble.ui.PanoLazyRow
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import com.arn.scrobble.utils.Stuff.format
import com.arn.scrobble.utils.Stuff.setMidnight
import com.arn.scrobble.utils.Stuff.setUserFirstDayOfWeek
import com.arn.scrobble.utils.Stuff.timeToLocal
import com.arn.scrobble.utils.Stuff.timeToUTC
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.appwidget_period
import pano_scrobbler.composeapp.generated.resources.cancel
import pano_scrobbler.composeapp.generated.resources.charts_continuous
import pano_scrobbler.composeapp.generated.resources.charts_custom
import pano_scrobbler.composeapp.generated.resources.date_range
import pano_scrobbler.composeapp.generated.resources.item_options
import pano_scrobbler.composeapp.generated.resources.listenbrainz
import pano_scrobbler.composeapp.generated.resources.months
import pano_scrobbler.composeapp.generated.resources.num_months
import pano_scrobbler.composeapp.generated.resources.num_weeks
import pano_scrobbler.composeapp.generated.resources.num_years
import pano_scrobbler.composeapp.generated.resources.ok
import pano_scrobbler.composeapp.generated.resources.weeks
import pano_scrobbler.composeapp.generated.resources.years
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

private val TimePeriodType.stringRes
    get() = when (this) {
        TimePeriodType.CONTINUOUS -> Res.string.charts_continuous
        TimePeriodType.CUSTOM -> Res.string.charts_custom
        TimePeriodType.WEEK -> Res.string.weeks
        TimePeriodType.MONTH -> Res.string.months
        TimePeriodType.YEAR -> Res.string.years
        TimePeriodType.LISTENBRAINZ -> Res.string.listenbrainz
        else -> throw IllegalArgumentException("Unknown period type: $this")
    }

private val periodTypeMenuItems by lazy {
    mapOf(
        TimePeriodType.WEEK to Icons.Outlined.CalendarViewWeek,
        TimePeriodType.MONTH to Icons.Outlined.CalendarViewMonth,
        TimePeriodType.YEAR to Icons.Outlined.CalendarToday,
        TimePeriodType.CONTINUOUS to Icons.Outlined.Circle,
        TimePeriodType.CUSTOM to Icons.Outlined.DateRange,
    )
}

private enum class SelectorType {
    PERIOD_TYPE, DATE_RANGE, MONTH, WEEK
}

fun getPeriodTypeIcon(periodType: TimePeriodType): ImageVector {
    return periodTypeMenuItems[periodType]!!
}

fun getPeriodTypePluralRes(periodType: TimePeriodType): PluralStringResource {
    return when (periodType) {
        TimePeriodType.WEEK -> Res.plurals.num_weeks
        TimePeriodType.MONTH -> Res.plurals.num_months
        TimePeriodType.YEAR -> Res.plurals.num_years
        else -> throw IllegalArgumentException("Unknown period type: $periodType")
    }
}

private fun monthPickerYears(start: Long, end: Long, formatter: DateFormat): Map<Int, String> {
    val cal by lazy { Calendar.getInstance() }

    val years = mutableMapOf<Int, String>()
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
): Map<Int, String> {
    val cal by lazy { Calendar.getInstance() }
    val months = mutableMapOf<Int, String>()
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TimePeriodSelector(
    user: UserCached,
    viewModel: ChartsPeriodVM,
    onSelected: (timePeriod: TimePeriod, prevTimePeriod: TimePeriod?) -> Unit,
    modifier: Modifier = Modifier,
) {

    val timePeriods by viewModel.timePeriods.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val periodType by viewModel.periodType.collectAsStateWithLifecycle(null)
    var selectorTypeShown by remember { mutableStateOf<SelectorType?>(null) }
    val isListenBrainz by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.currentAccountType == AccountType.LISTENBRAINZ }
    val listState = rememberLazyListState()

    LaunchedEffect(user) {
        viewModel.setUser(user)
    }

    LaunchedEffect(selectedPeriod) {
        viewModel.timePeriods.value[selectedPeriod]?.let { idx ->
            listState.animateScrollToItem(idx, -100.dp.value.toInt())
        }

        selectedPeriod?.let { selectedPeriod ->
            var prevPeriod: TimePeriod? = null
            if (periodType != TimePeriodType.CONTINUOUS) {
                viewModel.timePeriods.value[selectedPeriod]?.let { idx ->
                    prevPeriod = viewModel.timePeriods.value.keys.elementAtOrNull(idx + 1)
                }
            } else {
                if (selectedPeriod.lastfmPeriod != null && selectedPeriod.lastfmPeriod != LastfmPeriod.OVERALL) {
                    val cal = Calendar.getInstance()
                    cal.setUserFirstDayOfWeek()
                    cal.setMidnight()
                    val duration =
                        selectedPeriod.lastfmPeriod.toDuration(endTime = cal.timeInMillis)
                    prevPeriod =
                        selectedPeriod.lastfmPeriod.toTimePeriod(endTime = cal.timeInMillis - duration)
                }
            }

            onSelected(selectedPeriod, prevPeriod)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {

        if (!isListenBrainz) {
            Box {
                // todo: enable when new api
//                SplitButtonLayout(
//                    leadingButton = {
//                        OutlinedLeadingButton(
//                            onClick = {
//                                selectorTypeShown = SelectorType.PERIOD_TYPE
//                            }
//                        ) {
//                            periodType?.let {
//                                Icon(
//                                    imageVector = getPeriodTypeIcon(it),
//                                    contentDescription = stringResource(Res.string.item_options)
//                                )
//                            }
//                        }
//                    },
//
//                    trailingButton = {
//                        if (periodType == TimePeriodType.WEEK || periodType == TimePeriodType.MONTH) {
//                            OutlinedTrailingButton(
//                                checked = false,
//                                onCheckedChange = {
//                                    selectorTypeShown =
//                                        if (periodType == TimePeriodType.WEEK) SelectorType.WEEK else SelectorType.MONTH
//                                }
//                            ) {
//                                Icon(
//                                    imageVector = Icons.Outlined.Colorize,
//                                    contentDescription = stringResource(Res.string.appwidget_period)
//                                )
//                            }
//                        }
//                    },
//                )

                SplitButton(
                    leadingButton = {
                        SplitButtonDefaults.LeadingButton(
                            onClick = {
                                selectorTypeShown = SelectorType.PERIOD_TYPE
                            }
                        ) {
                            periodType?.let {
                                Icon(
                                    imageVector = getPeriodTypeIcon(it),
                                    contentDescription = stringResource(Res.string.item_options)
                                )
                            }
                        }
                    },

                    trailingButton = {
                        if (periodType == TimePeriodType.WEEK || periodType == TimePeriodType.MONTH) {
                            SplitButtonDefaults.TrailingButton(
                                checked = false,
                                onClick = {
                                    selectorTypeShown =
                                        if (periodType == TimePeriodType.WEEK) SelectorType.WEEK else SelectorType.MONTH
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Colorize,
                                    contentDescription = stringResource(Res.string.appwidget_period)
                                )
                            }
                        }
                    },
                )

                PeriodTypeSelector(
                    expanded = selectorTypeShown == SelectorType.PERIOD_TYPE,
                    onDismissRequest = { selectorTypeShown = null },
                    selectedPeriodType = periodType,
                    onMenuItemClick = { viewModel.setPeriodType(it) }
                )
            }
        }

        PanoLazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            contentPadding = PaddingValues(end = 24.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(timePeriods.keys.toList()) { timePeriod ->
                FilterChip(
                    onClick = {
                        if (periodType == TimePeriodType.CUSTOM)
                            selectorTypeShown = SelectorType.DATE_RANGE
                        else
                            viewModel.setSelectedPeriod(timePeriod)
                    },
                    selected = timePeriod == selectedPeriod,
                    label = { Text(text = timePeriod.name) },
                )
            }
        }
    }

    when (selectorTypeShown) {
        SelectorType.DATE_RANGE -> {
            DateRangePickerModal(
                selectedDateRange = selectedPeriod?.start?.timeToUTC() to selectedPeriod?.end?.timeToUTC(),
                allowedRange = user.registeredTime to System.currentTimeMillis(),
                onDateRangeSelected = { (startUtc, endUtc) ->
                    if (startUtc != null && endUtc != null) {
                        val timePeriod = TimePeriod(startUtc.timeToLocal(), endUtc.timeToLocal())

                        viewModel.setCustomPeriodInput(timePeriod)
                    }
                },
                onDismiss = { selectorTypeShown = null }
            )
        }

        SelectorType.WEEK -> {
            val validTimes = remember(timePeriods) {
                timePeriods.keys.associateBy { it.start.timeToUTC() }
            }

            DatePickerModal(
                selectedDate = selectedPeriod?.start,
                allowedRange = user.registeredTime to System.currentTimeMillis(),
                selectableDates = object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) =
                        utcTimeMillis in user.registeredTime..System.currentTimeMillis() && utcTimeMillis in validTimes
                },
                onDateSelected = {
                    if (it != null && validTimes[it] != null)
                        viewModel.setSelectedPeriod(validTimes[it]!!)
                },
                onDismiss = { selectorTypeShown = null }
            )
        }

        SelectorType.MONTH -> {
            val validTimes = remember(timePeriods) { timePeriods.keys.associateBy { it.start } }

            MonthPickerDialog(
                selectedMillis = selectedPeriod?.start ?: System.currentTimeMillis(),
                onDismissRequest = { selectorTypeShown = null },
                allowedRange = user.registeredTime to System.currentTimeMillis(),
                onMonthMillisSelected = {
                    viewModel.setSelectedPeriod(validTimes[it]!!)
                }
            )
        }

        else -> {}
    }
}

@Composable
private fun PeriodTypeSelector(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    selectedPeriodType: TimePeriodType?,
    onMenuItemClick: (TimePeriodType) -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {
        periodTypeMenuItems.forEach { (periodType, icon) ->
            DropdownMenuItem(
                onClick = {
                    onMenuItemClick(periodType)
                    onDismissRequest()
                },
                enabled = periodType != selectedPeriodType,
                text = { Text(text = stringResource(periodType.stringRes)) },
                leadingIcon = {
                    Icon(imageVector = icon, contentDescription = null)
                }
            )
        }
    }

}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerModal(
    selectedDateRange: Pair<Long?, Long?>,
    allowedRange: Pair<Long, Long>,
    onDateRangeSelected: (Pair<Long?, Long?>) -> Unit,
    onDismiss: () -> Unit,
) {
    val allowedRangeYears = remember { millisRangeToYears(allowedRange) }

    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = selectedDateRange.first,
        initialSelectedEndDateMillis = selectedDateRange.second,
        initialDisplayedMonthMillis = selectedDateRange.first ?: System.currentTimeMillis(),
        yearRange = allowedRangeYears,
        selectableDates = remember {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    utcTimeMillis in allowedRange.first..allowedRange.second
            }
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onDateRangeSelected(
                        Pair(
                            dateRangePickerState.selectedStartDateMillis,
                            dateRangePickerState.selectedEndDateMillis?.plus(24 * 60 * 60 * 1000 - 1)
                        )
                    )
                    onDismiss()
                }
            ) {
                Text(stringResource(Res.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            title = {
                Text(
                    text = stringResource(Res.string.date_range),
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
                .padding(16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthPickerDialog(
    selectedMillis: Long,
    allowedRange: Pair<Long, Long>,
    onDismissRequest: () -> Unit,
    onMonthMillisSelected: (Long) -> Unit,
) {
    val cal =
        remember {
            Calendar.getInstance().apply {
                timeInMillis = selectedMillis
                setMidnight()
            }
        }

    val yearFormatter = remember { SimpleDateFormat("yyyy", Locale.getDefault()) }
    val monthFormatter = remember { SimpleDateFormat("MMM", Locale.getDefault()) }

    var selectedYear by remember { mutableIntStateOf(cal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(cal.get(Calendar.MONTH)) }

    val yearsMap =
        remember {
            monthPickerYears(allowedRange.first, allowedRange.second, yearFormatter)
        }
    val monthsMap = remember {
        monthPickerMonths(selectedYear, allowedRange.first, allowedRange.second, monthFormatter)
    }

    BasicAlertDialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(24.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ButtonWithSpinner(
                        prefixText = null,
                        itemToTexts = yearsMap,
                        selected = selectedYear,
                        onItemSelected = { selectedYear = it },
                    )
                    ButtonWithSpinner(
                        prefixText = null,
                        itemToTexts = monthsMap,
                        selected = selectedMonth,
                        onItemSelected = { selectedMonth = it },
                    )
                }


                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismissRequest) {
                        Text(text = stringResource(Res.string.cancel))
                    }
                    TextButton(onClick = {
                        cal.apply {
                            set(Calendar.YEAR, selectedYear)
                            set(Calendar.MONTH, selectedMonth)
                            set(Calendar.DAY_OF_MONTH, 1)
                        }

                        onMonthMillisSelected(cal.timeInMillis)
                        onDismissRequest()
                    }) {
                        Text(text = stringResource(Res.string.ok))
                    }
                }
            }
        }
    }
}

@Composable
fun getMusicEntryQString(
    zeroStrRes: StringResource,
    pluralRes: PluralStringResource,
    count: Int,
    isTimePeriodContinuous: Boolean,
): String {
    val plus = if (count == 1000 && !isTimePeriodContinuous) "+" else ""

    return if (count <= 0)
        stringResource(zeroStrRes)
    else
        pluralStringResource(
            pluralRes,
            count,
            count.format() + plus
        )
}

