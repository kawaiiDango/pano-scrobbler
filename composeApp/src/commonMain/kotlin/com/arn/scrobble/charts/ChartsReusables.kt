package com.arn.scrobble.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerDefaults
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.LastfmPeriod
import com.arn.scrobble.charts.TimePeriodsGenerator.Companion.toDuration
import com.arn.scrobble.charts.TimePeriodsGenerator.Companion.toTimePeriod
import com.arn.scrobble.icons.ArrowDropDown
import com.arn.scrobble.icons.ArrowDropDownCircle
import com.arn.scrobble.icons.CalendarToday
import com.arn.scrobble.icons.CalendarViewMonth
import com.arn.scrobble.icons.CalendarViewWeek
import com.arn.scrobble.icons.Circle
import com.arn.scrobble.icons.DateRange
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.Refresh
import com.arn.scrobble.icons.automirrored.ArrowLeft
import com.arn.scrobble.icons.automirrored.ArrowRight
import com.arn.scrobble.navigation.jsonSerializableSaver
import com.arn.scrobble.ui.PanoLazyColumn
import com.arn.scrobble.ui.combineImageVectors
import com.arn.scrobble.ui.rememberLocaleWithCustomWeekday
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import com.arn.scrobble.utils.Stuff.format
import com.arn.scrobble.utils.Stuff.setMidnight
import com.arn.scrobble.utils.Stuff.timeToLocal
import com.arn.scrobble.utils.Stuff.timeToUTC
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.cancel
import pano_scrobbler.composeapp.generated.resources.charts_continuous
import pano_scrobbler.composeapp.generated.resources.charts_custom
import pano_scrobbler.composeapp.generated.resources.item_options
import pano_scrobbler.composeapp.generated.resources.listenbrainz
import pano_scrobbler.composeapp.generated.resources.months
import pano_scrobbler.composeapp.generated.resources.num_months
import pano_scrobbler.composeapp.generated.resources.num_weeks
import pano_scrobbler.composeapp.generated.resources.num_years
import pano_scrobbler.composeapp.generated.resources.ok
import pano_scrobbler.composeapp.generated.resources.reload
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
        TimePeriodType.WEEK to Icons.CalendarViewWeek,
        TimePeriodType.MONTH to Icons.CalendarViewMonth,
        TimePeriodType.YEAR to Icons.CalendarToday,
        TimePeriodType.CONTINUOUS to Icons.Circle,
        TimePeriodType.CUSTOM to Icons.DateRange,
    )
}

fun getPeriodTypeIcon(periodType: TimePeriodType): ImageVector {
    return periodTypeMenuItems[periodType] ?: Icons.Circle
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TimePeriodSelector(
    user: UserCached,
    viewModel: ChartsPeriodVM,
    onSelected: (timePeriod: TimePeriod, prevTimePeriod: TimePeriod?, Int) -> Unit,
    showRefreshButton: Boolean,
    modifier: Modifier = Modifier,
    digestTimePeriod: LastfmPeriod? = null,
) {
    val timePeriods by viewModel.timePeriods.collectAsStateWithLifecycle()
    val timePeriodsList by remember(timePeriods) { mutableStateOf(timePeriods.keys.toList()) }
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val refreshCount by viewModel.refreshCount.collectAsStateWithLifecycle()
    val periodType by viewModel.periodType.collectAsStateWithLifecycle()
    var dropdownTypeShown by rememberSaveable(saver = jsonSerializableSaver<TimePeriodType?>()) {
        mutableStateOf(null)
    }
    var typeSelectorShown by remember { mutableStateOf(false) }
    val accountType by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.currentAccountType }
    val listState = rememberLazyListState()
    var selectedPeriodOffsetX by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    LaunchedEffect(accountType, digestTimePeriod) {
        when (accountType) {
            AccountType.LISTENBRAINZ -> {
                viewModel.setPeriodType(TimePeriodType.LISTENBRAINZ)
                if (digestTimePeriod == null) {
                    val selected = PlatformStuff.mainPrefs.data.mapLatest {
                        it.lastChartsListenBrainzPeriod
                    }.first()
                    viewModel.setSelectedPeriod(selected)
                }
            }

            AccountType.LASTFM if digestTimePeriod == null -> {
                val (type, selected, custom) = PlatformStuff.mainPrefs.data.mapLatest {
                    Triple(
                        it.lastChartsPeriodType,
                        it.lastChartsLastfmPeriod,
                        it.lastChartsCustomPeriod
                    )
                }.first()
                viewModel.setPeriodType(type)
                viewModel.setSelectedPeriod(selected)
                viewModel.setCustomPeriodInput(custom)
            }

            else -> {
                viewModel.setPeriodType(TimePeriodType.CONTINUOUS)
            }
        }

        if (digestTimePeriod != null) {
            viewModel.setDigestPeriod(digestTimePeriod)
        }
    }

    LaunchedEffect(selectedPeriod, refreshCount) {
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
                    cal.setMidnight()
                    val duration =
                        selectedPeriod.lastfmPeriod.toDuration(endTime = cal.timeInMillis)
                    prevPeriod =
                        selectedPeriod.lastfmPeriod.toTimePeriod(endTime = cal.timeInMillis - duration)
                }
            }

            onSelected(selectedPeriod, prevPeriod, refreshCount)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.background(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.large
        ).clip(
            shape = MaterialTheme.shapes.large
        )
    ) {
        if (accountType != AccountType.LISTENBRAINZ) {
            Box {
                OutlinedToggleButton(
                    checked = typeSelectorShown,
                    onCheckedChange = {
                        typeSelectorShown = it
                    },
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                ) {
                    periodType?.let {
                        Icon(
                            combineImageVectors(
                                getPeriodTypeIcon(it),
                                Icons.ArrowDropDown
                            ),
                            contentDescription = stringResource(Res.string.item_options)
                        )
                    }
                }

                PeriodTypeSelector(
                    expanded = typeSelectorShown,
                    onDismissRequest = { typeSelectorShown = false },
                    selectedPeriodType = periodType,
                    onMenuItemClick = { viewModel.setPeriodType(it) },
                    onRefresh = if (showRefreshButton) {
                        {
                            viewModel.refresh()
                        }
                    } else null
                )
            }
        }

        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp),
            modifier = Modifier.weight(1f),
        ) {
            itemsIndexed(
                timePeriodsList
            ) { idx, timePeriod ->
                FilterChip(
                    onClick = {
                        if (timePeriod == selectedPeriod) {
                            dropdownTypeShown = periodType
                        } else
                            viewModel.setSelectedPeriod(timePeriod)
                    },
                    selected = timePeriod == selectedPeriod,
                    trailingIcon = {
                        if (timePeriod == selectedPeriod) {
                            Icon(
                                imageVector = Icons.ArrowDropDownCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        } else if (timePeriodsList.getOrNull(idx + 1) == selectedPeriod) {
                            Icon(
                                imageVector = Icons.AutoMirrored.ArrowLeft,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    },
                    leadingIcon = {
                        if (timePeriodsList.getOrNull(idx - 1) == selectedPeriod) {
                            Icon(
                                imageVector = Icons.AutoMirrored.ArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    },
                    label = { Text(text = timePeriod.name) },
                    modifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            if (timePeriod == selectedPeriod) {
                                selectedPeriodOffsetX = coordinates.positionInParent().round().x +
                                        coordinates.size.width / 2
                            }
                        }
                )
            }
        }
    }

    when (dropdownTypeShown) {
        TimePeriodType.CUSTOM -> {
            DateRangePickerModal(
                selectedDateRange = selectedPeriod?.start?.timeToUTC() to selectedPeriod?.end?.timeToUTC(),
                allowedRange = user.registeredTime to System.currentTimeMillis(),
                onDateRangeSelected = { (startUtc, endUtc) ->
                    if (startUtc != null && endUtc != null) {
                        val timePeriod =
                            TimePeriod(startUtc.timeToLocal(), endUtc.timeToLocal())

                        viewModel.setCustomPeriodInput(timePeriod)
                    }
                },
                onDismiss = { dropdownTypeShown = null }
            )
        }

        TimePeriodType.WEEK -> {
            val validTimes = remember(timePeriods) {
                timePeriodsList.associateBy { it.start.timeToUTC() }
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
                onDismiss = { dropdownTypeShown = null }
            )
        }

        TimePeriodType.MONTH -> {
            val validTimes = remember(timePeriods) { timePeriodsList.associateBy { it.start } }

            MonthPickerPopup(
                offset = IntOffset(selectedPeriodOffsetX, 0),
                selectedMillis = selectedPeriod?.start ?: System.currentTimeMillis(),
                onDismissRequest = { dropdownTypeShown = null },
                allowedRange = user.registeredTime to System.currentTimeMillis(),
                onMonthMillisSelected = {
                    validTimes[it]?.let {
                        viewModel.setSelectedPeriod(it)
                    }
                }
            )
        }

        TimePeriodType.CONTINUOUS, TimePeriodType.YEAR, TimePeriodType.LISTENBRAINZ -> {
            val selectedPeriodOffsetXDp = with(density) {
                selectedPeriodOffsetX.toDp()
            }

            Box {
                DropdownMenu(
                    offset = DpOffset(selectedPeriodOffsetXDp, 0.dp),
                    expanded = dropdownTypeShown != null,
                    onDismissRequest = { dropdownTypeShown = null }
                ) {
                    timePeriodsList.forEach { timePeriod ->
                        DropdownMenuItem(
                            onClick = {
                                viewModel.setSelectedPeriod(timePeriod)
                                dropdownTypeShown = null
                            },
                            enabled = timePeriod != selectedPeriod,
                            text = {
                                Text(
                                    text = timePeriod.name,
                                )
                            },
                        )
                    }
                }
            }
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
    onRefresh: (() -> Unit)?
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

        if (onRefresh != null) {
            DropdownMenuItem(
                onClick = {
                    onRefresh()
                    onDismissRequest()
                },
                text = { Text(text = stringResource(Res.string.reload)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Refresh, contentDescription = null)
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
    val locale = rememberLocaleWithCustomWeekday()
    val allowedRangeYears = remember { millisRangeToYears(allowedRange) }
    val dateFormatter = remember { DatePickerDefaults.dateFormatter() }
    val initialDisplayedMonthMillis =
        remember { selectedDateRange.first ?: System.currentTimeMillis() }

    val dateRangePickerState = remember {
        DateRangePickerState(
            locale = locale,
            initialSelectedStartDateMillis = selectedDateRange.first,
            initialSelectedEndDateMillis = selectedDateRange.second,
            initialDisplayedMonthMillis = initialDisplayedMonthMillis,
            yearRange = allowedRangeYears,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    utcTimeMillis in allowedRange.first..allowedRange.second
            }
        )
    }

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
            headline = {
                // workaround for a text overflow bug in compose in pt locale
                DateRangePickerDefaults.DateRangePickerHeadline(
                    selectedStartDateMillis = dateRangePickerState.selectedStartDateMillis,
                    selectedEndDateMillis = dateRangePickerState.selectedEndDateMillis,
                    displayMode = dateRangePickerState.displayMode,
                    dateFormatter = dateFormatter,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
                .padding(16.dp)
        )
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MonthPickerPopup(
    offset: IntOffset,
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
    val monthsMap = remember(selectedYear) {
        monthPickerMonths(
            selectedYear,
            allowedRange.first,
            allowedRange.second,
            monthFormatter
        ).also {
            if (selectedMonth !in it.keys)
                selectedMonth = Calendar.JANUARY
        }
    }

    Popup(
        offset = offset,
        properties = PopupProperties(
            focusable = true,
            usePlatformDefaultWidth = true
        ),
        onDismissRequest = onDismissRequest
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            shadowElevation = 4.dp,
            tonalElevation = 4.dp,
            modifier = Modifier
                .fillMaxHeight(0.6f)
                .wrapContentWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                PanoLazyColumn {
                    items(yearsMap.toList()) { (year, text) ->
                        OutlinedToggleButton(
                            checked = year == selectedYear,
                            onCheckedChange = {
                                if (it) {
                                    selectedYear = year

                                    cal.apply {
                                        set(Calendar.YEAR, selectedYear)
                                        set(Calendar.MONTH, selectedMonth)
                                        set(Calendar.DAY_OF_MONTH, 1)
                                    }

                                    onMonthMillisSelected(cal.timeInMillis)
                                }
                            },
//                        modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text)
                        }
                    }
                }
                PanoLazyColumn {
                    items(monthsMap.toList()) { (month, text) ->
                        OutlinedToggleButton(
                            checked = month == selectedMonth,
                            onCheckedChange = {
                                if (it) {
                                    selectedMonth = month

                                    cal.apply {
                                        set(Calendar.YEAR, selectedYear)
                                        set(Calendar.MONTH, selectedMonth)
                                        set(Calendar.DAY_OF_MONTH, 1)
                                    }

                                    onMonthMillisSelected(cal.timeInMillis)
                                    onDismissRequest()
                                }
                            },
//                        modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text)
                        }
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

